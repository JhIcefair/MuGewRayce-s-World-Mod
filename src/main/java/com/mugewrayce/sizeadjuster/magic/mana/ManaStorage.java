package com.mugewrayce.sizeadjuster.magic.mana;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 区块魔力稀疏账本（W13，接口约定 §3.6）。
 *
 * <p><b>核心语义：只记“不等于最大值”的区块；没有记过的区块 = 正处于自己的最大值。</b>
 * 这样重进世界/重启服务器天然“回到最大值”，不需要持久化，也不需要预热世界。</p>
 *
 * <p>约定：</p>
 * <ul>
 *   <li><b>按维度隔离</b>：外层 key 是 {@link ResourceKey}{@code <Level>}，主世界与下界同坐标互不影响；</li>
 *   <li><b>不持久化</b>：没有任何存档写入，服务器启动时 {@link #resetAll()}；</li>
 *   <li>允许超过最大值（迁移、水晶释放、聚合台释放都会超）；</li>
 *   <li><b>只在服务端主线程</b>访问（`ServerTickEvents` / 包处理 / 菜单按钮），不加锁。</li>
 * </ul>
 */
public final class ManaStorage {

    /** 维度 → (ChunkPos.pack → 当前值)；只存不等于最大值的区块。 */
    private static final Map<ResourceKey<Level>, Map<Long, Integer>> CURRENT = new HashMap<>();

    private ManaStorage() {
    }

    /** 当前值；没记过 → 该区块的最大值。 */
    public static int get(ServerLevel level, ChunkPos pos) {
        Map<Long, Integer> ledger = CURRENT.get(level.dimension());
        if (ledger == null) {
            return ChunkManaMax.maxFor(level, pos);
        }
        Integer stored = ledger.get(pos.pack());
        return stored != null ? stored : ChunkManaMax.maxFor(level, pos);
    }

    public static int get(ServerLevel level, int chunkX, int chunkZ) {
        return get(level, new ChunkPos(chunkX, chunkZ));
    }

    public static int getMax(ServerLevel level, ChunkPos pos) {
        return ChunkManaMax.maxFor(level, pos);
    }

    public static int getMax(ServerLevel level, int chunkX, int chunkZ) {
        return ChunkManaMax.maxFor(level, chunkX, chunkZ);
    }

    /** 该区块是否被“记过账”（低于最大值，或曾超过最大值）。 */
    public static boolean isRecorded(ServerLevel level, ChunkPos pos) {
        Map<Long, Integer> ledger = CURRENT.get(level.dimension());
        return ledger != null && ledger.containsKey(pos.pack());
    }

    /** 写入当前值；等于最大值时删条目（稀疏），负值按 0。 */
    public static void set(ServerLevel level, ChunkPos pos, int value) {
        int clamped = Math.max(0, value);
        int max = ChunkManaMax.maxFor(level, pos);
        Map<Long, Integer> ledger = CURRENT.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        if (clamped == max) {
            ledger.remove(pos.pack());
            if (ledger.isEmpty()) {
                CURRENT.remove(level.dimension());
            }
            return;
        }
        ledger.put(pos.pack(), clamped);
    }

    /**
     * 加值（释放 / 迁移入口）；允许超过最大值。
     *
     * @return 加了多少（负数 delta 会被 {@link #set} 夹到 0）
     */
    public static int addChunk(ServerLevel level, ChunkPos pos, int delta) {
        if (delta == 0) {
            return 0;
        }
        int before = get(level, pos);
        int after = Math.max(0, before + delta);
        set(level, pos, after);
        return after - before;
    }

    /** 按方块位置加值（击杀释放等）；只作用于该方块的区块。 */
    public static int add(ServerLevel level, BlockPos pos, int delta) {
        return addChunk(level, ChunkPos.containing(pos), delta);
    }

    /** 原子扣减：不够则一点都不扣。 */
    public static boolean consume(ServerLevel level, ChunkPos pos, int amount) {
        if (amount <= 0) {
            return true;
        }
        int current = get(level, pos);
        if (current < amount) {
            return false;
        }
        set(level, pos, current - amount);
        return true;
    }

    /** 该维度“被记过”的区块快照（只读）；ticker 只遍历这些区块，绝不扫世界。 */
    public static Map<Long, Integer> snapshot(ServerLevel level) {
        Map<Long, Integer> ledger = CURRENT.get(level.dimension());
        if (ledger == null || ledger.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(ledger));
    }

    /** 该维度被记过的区块数（自检/诊断用）。 */
    public static int recordedCount(ServerLevel level) {
        Map<Long, Integer> ledger = CURRENT.get(level.dimension());
        return ledger == null ? 0 : ledger.size();
    }

    /** 服务器启动 / 重进世界：全部区块回到最大值。 */
    public static void resetAll() {
        CURRENT.clear();
        ChunkManaMax.clearCache();
    }
}
