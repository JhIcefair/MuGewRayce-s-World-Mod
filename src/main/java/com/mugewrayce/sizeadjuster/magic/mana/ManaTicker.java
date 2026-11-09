package com.mugewrayce.sizeadjuster.magic.mana;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 区块魔力每秒结算（W13，接口约定 §3.6）。
 *
 * <p>每 20 tick（{@link ServerTickEvents#END_SERVER_TICK} 计数）做两件事，顺序固定：</p>
 * <ol>
 *   <li><b>自然恢复</b>：每个“被记过账”的区块 +1，到默认最大值终止（值已超上限的保持不动）；</li>
 *   <li><b>临近迁移</b>：只对“迁移中”集合里的区块，从其供体按差值四档拉取
 *       {@code <20→0 / 20~99→1 / 100~499→3 / ≥500→5}；距最大值 &lt;20 时移出集合。</li>
 * </ol>
 *
 * <p>迁移<b>由消耗事件触发</b>（草案 §7.6 / Q2）：{@link #recordConsumption} 在每次扣费成功时
 * 登记“迁移中”区块与供体；门槛 = 单区块扣量 &lt;20 不迁移；供体只取直接相邻 4 区块，
 * 且排除本轮参与消耗的区块（1 区块消耗 → 4 供体；2 → 3；4 → 2），迁完不再连锁。</p>
 *
 * <p>只遍历被记过的区块与迁移集合，<b>绝不遍历世界</b>；遍历顺序按 {@code (维度, cx, cz)} 排序，
 * 保证确定性。释放（聚合台 4500）不在这里，在聚合台 BE 自己的 ticker 里。</p>
 */
public final class ManaTicker {

    /** 每秒结算一次。 */
    public static final int INTERVAL_TICKS = 20;
    /** 自然恢复：每秒 +1。 */
    public static final int NATURAL_RECOVERY = 1;
    /** 迁移门槛：单区块扣量 < 20 不产生迁移（草案 §7.6）。 */
    public static final int MIGRATION_THRESHOLD = 20;
    /** 需求区块距最大值 < 20 时停止迁移（草案 §7.6）。 */
    public static final int MIGRATION_STOP_DISTANCE = 20;
    /** 回退口：迁移出问题时可置 false，只保留恢复（交接单 §6）。 */
    public static final boolean MIGRATION_ENABLED = true;

    private static final int TIER_1_DIFF = 20;
    private static final int TIER_2_DIFF = 100;
    private static final int TIER_3_DIFF = 500;
    private static final int TIER_1_STEP = 1;
    private static final int TIER_2_STEP = 3;
    private static final int TIER_3_STEP = 5;

    private static final Map<ResourceKey<Level>, Map<Long, long[]>> MIGRATIONS = new HashMap<>();

    private static int tickCounter;

    private ManaTicker() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ManaTicker::onEndTick);
    }

    /**
     * 登记一次成功的区块扣费，用于触发邻近迁移（草案 §7.6）。
     *
     * @param level        维度
     * @param participants 本次消耗参与的区块（九宫格：1 / 2 / 4 个），作为供体排除集
     * @param consumed     本次真正被扣的区块（一次性=全部参与区块；持续=轮到的那个）
     * @param amounts      与 {@code consumed} 同长的扣量
     */
    public static void recordConsumption(ServerLevel level, List<ChunkPos> participants,
                                         List<ChunkPos> consumed, int[] amounts) {
        if (!MIGRATION_ENABLED || consumed.isEmpty()) {
            return;
        }
        Set<Long> excluded = new HashSet<>();
        for (ChunkPos chunk : participants) {
            excluded.add(chunk.pack());
        }
        Map<Long, long[]> targets = MIGRATIONS.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        for (int i = 0; i < consumed.size(); i++) {
            if (amounts[i] < MIGRATION_THRESHOLD) {
                continue;
            }
            ChunkPos chunk = consumed.get(i);
            long[] donors = donorPacks(chunk, excluded);
            if (donors.length > 0) {
                targets.put(chunk.pack(), donors);
            }
        }
    }

    /** 迁移四档（纯函数，自检直接调用）。 */
    static int migrationStep(int diff) {
        if (diff < TIER_1_DIFF) {
            return 0;
        }
        if (diff < TIER_2_DIFF) {
            return TIER_1_STEP;
        }
        if (diff < TIER_3_DIFF) {
            return TIER_2_STEP;
        }
        return TIER_3_STEP;
    }

    /** 供体列表：4 个直接相邻里排除“本轮参与消耗的区块”。 */
    static long[] donorPacks(ChunkPos chunk, Set<Long> excluded) {
        return fourNeighbors(chunk).stream()
                .filter(neighbor -> !excluded.contains(neighbor.pack()))
                .mapToLong(ChunkPos::pack)
                .toArray();
    }

    private static void onEndTick(MinecraftServer server) {
        if (++tickCounter < INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        for (ServerLevel level : sortedLevels(server)) {
            recover(level);
            migrate(level);
        }
    }

    private static void recover(ServerLevel level) {
        Map<Long, Integer> snapshot = ManaStorage.snapshot(level);
        if (snapshot.isEmpty()) {
            return;
        }
        long[] keys = snapshot.keySet().stream().mapToLong(Long::longValue).sorted().toArray();
        for (long packed : keys) {
            ChunkPos pos = ChunkPos.unpack(packed);
            int current = ManaStorage.get(level, pos);
            int max = ManaStorage.getMax(level, pos);
            if (current > max) {
                continue; // 迁移/释放造成的“超上限”保持不动，也不参与自然恢复
            }
            ManaStorage.set(level, pos, current + NATURAL_RECOVERY); // 到 max 时 set 自动删条目
        }
    }

    private static void migrate(ServerLevel level) {
        Map<Long, long[]> targets = MIGRATIONS.get(level.dimension());
        if (targets == null || targets.isEmpty()) {
            return;
        }
        long[] keys = new long[targets.size()];
        int index = 0;
        for (Long key : targets.keySet()) {
            keys[index++] = key;
        }
        java.util.Arrays.sort(keys);

        Map<Long, Integer> deltas = new HashMap<>();
        for (long packed : keys) {
            long[] donors = targets.get(packed);
            if (donors == null) {
                continue;
            }
            ChunkPos recipient = ChunkPos.unpack(packed);
            int current = ManaStorage.get(level, recipient);
            int max = ManaStorage.getMax(level, recipient);
            if (max - current < MIGRATION_STOP_DISTANCE) {
                targets.remove(packed);
                continue;
            }
            for (long donorPacked : donors) {
                ChunkPos donor = ChunkPos.unpack(donorPacked);
                int donorValue = ManaStorage.get(level, donor);
                int step = migrationStep(donorValue - current);
                if (step <= 0 || donorValue <= 0) {
                    continue;
                }
                deltas.merge(packed, step, Integer::sum);
                deltas.merge(donorPacked, -step, Integer::sum);
            }
        }
        if (targets.isEmpty()) {
            MIGRATIONS.remove(level.dimension());
        }
        if (deltas.isEmpty()) {
            return;
        }
        long[] deltaKeys = deltas.keySet().stream().mapToLong(Long::longValue).sorted().toArray();
        for (long packed : deltaKeys) {
            ManaStorage.addChunk(level, ChunkPos.unpack(packed), deltas.get(packed));
        }
    }

    private static List<ServerLevel> sortedLevels(MinecraftServer server) {
        List<ServerLevel> levels = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            levels.add(level);
        }
        levels.sort(Comparator.comparing(level -> level.dimension().identifier().toString()));
        return levels;
    }

    /** 直接相邻 4 区块（供体只直接相邻，不做递归）。 */
    static List<ChunkPos> fourNeighbors(ChunkPos chunk) {
        List<ChunkPos> neighbors = new ArrayList<>(4);
        neighbors.add(new ChunkPos(chunk.x() - 1, chunk.z()));
        neighbors.add(new ChunkPos(chunk.x() + 1, chunk.z()));
        neighbors.add(new ChunkPos(chunk.x(), chunk.z() - 1));
        neighbors.add(new ChunkPos(chunk.x(), chunk.z() + 1));
        return neighbors;
    }

    /** 服务器停止时清空迁移集合与计数。 */
    public static void reset() {
        MIGRATIONS.clear();
        tickCounter = 0;
    }

    /** 当前“迁移中”区块数（诊断/自检用）。 */
    public static int migratingCount(ServerLevel level) {
        Map<Long, long[]> targets = MIGRATIONS.get(level.dimension());
        return targets == null ? 0 : targets.size();
    }

    /** 供体列表（诊断用，只读副本）。 */
    public static List<ChunkPos> donorsOf(ServerLevel level, ChunkPos chunk) {
        Map<Long, long[]> targets = MIGRATIONS.get(level.dimension());
        long[] donors = targets == null ? null : targets.get(chunk.pack());
        List<ChunkPos> result = new ArrayList<>();
        if (donors != null) {
            for (long packed : donors) {
                result.add(ChunkPos.unpack(packed));
            }
        }
        return result;
    }
}
