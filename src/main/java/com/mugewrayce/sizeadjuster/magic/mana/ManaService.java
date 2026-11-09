package com.mugewrayce.sizeadjuster.magic.mana;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 区块魔力对外唯一入口（W13，接口约定 §3.6；W14/W15 的施法消耗都走这里）。
 *
 * <p>{@link #tryConsume} 的判定顺序：</p>
 * <ol>
 *   <li>背包里有<b>永恒水晶</b> → {@code EXEMPT_ETERNAL}（连区块都不读；方块功能不走这里）；</li>
 *   <li><b>锁定</b>中（某次区块不足后，直到该区块 ≥1000 或供能关闭）→ 只扣水晶，
 *       返回 {@code CRYSTAL_LOCKED}；水晶也不足 → {@code INSUFFICIENT}；</li>
 *   <li>正常：九宫格分摊（一次性）或轮流单区块（持续）；</li>
 *   <li>区块不足 → 用水晶兜底：未开自动供能的持续消耗<b>中途不接续</b>（本次失败并置锁，
 *       玩家重新开始一次施法后由水晶支付）；开了自动供能则直接扣水晶；</li>
 *   <li>都没有 → {@code INSUFFICIENT}，<b>失败不产生任何副作用</b>。</li>
 * </ol>
 *
 * <p>只在服务端、只在主线程调用。</p>
 */
public final class ManaService {

    /** 一次消耗的结果。 */
    public enum Result {
        /** 区块魔力已支付（或 amount &lt;= 0）。 */
        OK,
        /** 区块与水晶都付不起。 */
        INSUFFICIENT,
        /** 携带永恒水晶，施法类直接免除。 */
        EXEMPT_ETERNAL,
        /** 由普通魔法水晶支付。 */
        CRYSTAL_PAID,
        /** 锁定期内只扣水晶。 */
        CRYSTAL_LOCKED
    }

    /** 探测器 5×5。 */
    public static final int PROBE_SIZE = 5;

    /** 持续消耗的轮流步数（每个玩家一份）。 */
    private static final Map<UUID, Integer> ROTATION_STEPS = new HashMap<>();

    private ManaService() {
    }

    /**
     * 施法类消耗。
     *
     * @param everySecond true = 每秒一次且整笔扣同一个区块（按“本→邻→本…”轮流）
     */
    public static Result tryConsume(ServerPlayer player, int amount, boolean everySecond) {
        if (amount <= 0) {
            return Result.OK;
        }
        ServerLevel level = player.level();
        if (CrystalSupply.hasEternal(player)) {
            return Result.EXEMPT_ETERNAL;
        }
        ChunkPos owner = player.chunkPosition();
        ResourceKey<Level> dimension = level.dimension();
        int localX = ManaPosition.localX(player.getX());
        int localZ = ManaPosition.localZ(player.getZ());
        CrystalSupply.Supply crystal = CrystalSupply.find(player);

        if (ManaLock.isLocked(player, dimension, owner)) {
            boolean recovered = ManaStorage.get(level, owner) >= ManaLock.RELEASE_THRESHOLD;
            if (recovered || crystal == null) {
                ManaLock.clear(player, dimension, owner);
            } else {
                return CrystalSupply.pay(player, crystal, amount, owner, localX, localZ)
                        ? Result.CRYSTAL_LOCKED
                        : Result.INSUFFICIENT;
            }
        }

        ManaPosition.ManaSplit split = ManaPosition.split(owner, localX, localZ, amount);
        boolean chunkPaid = everySecond
                ? consumeRotating(level, player, split, amount)
                : consumeSplit(level, split);
        if (chunkPaid) {
            return Result.OK;
        }

        if (crystal == null) {
            return Result.INSUFFICIENT;
        }
        if (everySecond && !crystal.data().autoSupplyEnabled()) {
            // 未开自动供能：持续消耗中途不足不接续；置锁，重新开始一次施法后由水晶支付。
            ROTATION_STEPS.remove(player.getUUID());
            ManaLock.lock(player, dimension, owner);
            return Result.INSUFFICIENT;
        }
        ManaLock.lock(player, dimension, owner);
        return CrystalSupply.pay(player, crystal, amount, owner, localX, localZ)
                ? Result.CRYSTAL_PAID
                : Result.INSUFFICIENT;
    }

    /** 持续消耗：按轮流顺序只扣一个区块（整笔）。 */
    private static boolean consumeRotating(ServerLevel level, ServerPlayer player,
                                           ManaPosition.ManaSplit split, int amount) {
        int step = ROTATION_STEPS.merge(player.getUUID(), 1, Integer::sum) - 1;
        int target = ManaPosition.rotationTarget(split, step, player.getRandom());
        ChunkPos targetChunk = split.chunks().get(target);
        if (!ManaStorage.consume(level, targetChunk, amount)) {
            return false;
        }
        ManaTicker.recordConsumption(level, split.chunks(), List.of(targetChunk), new int[]{amount});
        return true;
    }

    /** 一次性消耗：九宫格分摊，全部够才原子扣（余数在所在区块）。 */
    private static boolean consumeSplit(ServerLevel level, ManaPosition.ManaSplit split) {
        List<ChunkPos> chunks = split.chunks();
        int[] amounts = split.amounts();
        for (int index = 0; index < chunks.size(); index++) {
            if (ManaStorage.get(level, chunks.get(index)) < amounts[index]) {
                return false;
            }
        }
        for (int index = 0; index < chunks.size(); index++) {
            ManaStorage.consume(level, chunks.get(index), amounts[index]);
        }
        ManaTicker.recordConsumption(level, chunks, chunks, amounts);
        return true;
    }

    public static int get(ServerLevel level, int chunkX, int chunkZ) {
        return ManaStorage.get(level, chunkX, chunkZ);
    }

    public static int getMax(ServerLevel level, int chunkX, int chunkZ) {
        return ManaStorage.getMax(level, chunkX, chunkZ);
    }

    /** 释放/迁移入口：加到该方块所在区块（允许超过最大值）。 */
    public static void add(ServerLevel level, BlockPos pos, int amount) {
        ManaStorage.add(level, pos, amount);
    }

    /** 释放入口：加到指定区块（九宫格分摊时逐个用）。 */
    public static void addChunk(ServerLevel level, ChunkPos pos, int amount) {
        ManaStorage.addChunk(level, pos, amount);
    }

    public static boolean consumeChunk(ServerLevel level, ChunkPos pos, int amount) {
        return ManaStorage.consume(level, pos, amount);
    }

    /** 探测器 5×5：行优先 NW→SE（上北下南、左西右东），取调用瞬间的值。 */
    public static int[] probe25(ServerLevel level, ChunkPos center) {
        int[] values = new int[PROBE_SIZE * PROBE_SIZE];
        int originX = center.x() - PROBE_SIZE / 2;
        int originZ = center.z() - PROBE_SIZE / 2;
        for (int row = 0; row < PROBE_SIZE; row++) {
            for (int column = 0; column < PROBE_SIZE; column++) {
                values[row * PROBE_SIZE + column] =
                        get(level, originX + column, originZ + row);
            }
        }
        return values;
    }

    /** 玩家断开：清掉轮流步数与锁定。 */
    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            ROTATION_STEPS.remove(playerId);
            ManaLock.clearPlayer(playerId);
        }
    }

    /** 服务器启动：账本回到最大值；停止：清空轮转表。 */
    public static void resetAll() {
        ManaStorage.resetAll();
        ROTATION_STEPS.clear();
        ManaLock.clearAll();
    }
}
