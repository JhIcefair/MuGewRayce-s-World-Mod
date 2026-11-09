package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.magic.mana.ManaService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * 没有生命的魔法体的自然生成（W08 骨架 → W13 分档）。
 *
 * <p>每 <b>3 分钟</b>尝试一次：对每个维度、<b>每名玩家</b>在其附近随机 1 个位置判 1 次，
 * 不一次刷一片。位置要求：可站立固体方块上方、非液体、与玩家水平距离 24~64 格、
 * 区块已加载/可 tick。</p>
 *
 * <p>分档（草案 §5.1）：区块魔力 ≤10000 → 概率 <b>0.1%</b>、消耗 <b>3000</b>、生命 <b>50</b>；
 * &gt;10000 → 概率 <b>5%</b>、消耗 <b>5000</b>、生命 <b>80</b>。顺序：读魔力 → 定档 → 掷概率 →
 * <b>先扣再 spawn</b>（魔力不足不生成也不扣）；spawn 失败把扣掉的还回去。</p>
 *
 * <p>按草案「不受和平模式影响生成」，<b>不再</b>有 {@code PEACEFUL} 早退（W08 骨架里有，
 * W13 删除，报告记偏差）；但保留 {@code SPAWN_MOBS} / {@code SPAWN_MONSTERS} 游戏规则过滤
 * （Q7：gamerule 属于“游戏默认不能自然刷新”）。</p>
 */
public final class MagicBodySpawner {

    /** 每 3 分钟尝试一次（草案 §5.1，2026-10-09 由 20 秒改为 3 分钟）。 */
    public static final int ATTEMPT_INTERVAL_TICKS = 20 * 180;

    /** 分档阈值：≤10000 低档，>10000 高档。 */
    public static final int TIER_THRESHOLD = 10000;
    /** 低档概率 0.1%。 */
    public static final float LOW_CHANCE = 0.001F;
    /** 高档概率 5%。 */
    public static final float HIGH_CHANCE = 0.05F;
    /** 低档消耗 3000。 */
    public static final int LOW_COST = 3000;
    /** 高档消耗 5000。 */
    public static final int HIGH_COST = 5000;
    /** 低档生命 50。 */
    public static final int LOW_HEALTH = 50;
    /** 高档生命 80。 */
    public static final int HIGH_HEALTH = 80;
    /** 与玩家水平距离下限。 */
    public static final int MIN_DISTANCE = 24;
    /** 与玩家水平距离上限。 */
    public static final int MAX_DISTANCE = 64;

    private static final int POSITION_ATTEMPTS = 4;

    private static int tickCounter;

    private MagicBodySpawner() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MagicBodySpawner::tick);
    }

    private static void tick(MinecraftServer server) {
        if (++tickCounter < ATTEMPT_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.getGameRules().get(GameRules.SPAWN_MOBS)
                    || !level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
                continue;
            }
            List<ServerPlayer> players = level.players();
            for (ServerPlayer player : players) {
                attemptFor(level, player);
            }
        }
    }

    /** 单个玩家的一次尝试：找位置 → 定档 → 掷概率 → 扣魔力 → 生成。 */
    private static void attemptFor(ServerLevel level, ServerPlayer player) {
        BlockPos pos = findSpawnPos(level, player);
        if (pos == null) {
            return;
        }
        ChunkPos chunk = ChunkPos.containing(pos);
        int mana = ManaService.get(level, chunk.x(), chunk.z());
        boolean high = mana > TIER_THRESHOLD;
        float chance = high ? HIGH_CHANCE : LOW_CHANCE;
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }
        int cost = high ? HIGH_COST : LOW_COST;
        if (!ManaService.consumeChunk(level, chunk, cost)) {
            return; // 魔力不足 3000 / 5000：不生成、不扣
        }
        LifelessMagicBodyEntity body = ModEntities.LIFELESS_MAGIC_BODY
                .spawn(level, pos, EntitySpawnReason.NATURAL);
        if (body == null) {
            ManaService.addChunk(level, chunk, cost); // 生成失败：退回扣掉的魔力
            return;
        }
        body.applyHealthTier(high ? HIGH_HEALTH : LOW_HEALTH);
    }

    /** 在玩家附近 24~64 格找可站立位置；找不到返回 {@code null}。 */
    private static BlockPos findSpawnPos(ServerLevel level, ServerPlayer player) {
        RandomSource random = player.getRandom();
        BlockPos origin = player.blockPosition();
        int distanceRange = MAX_DISTANCE - MIN_DISTANCE + 1;
        for (int attempt = 0; attempt < POSITION_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = MIN_DISTANCE + random.nextInt(distanceRange);
            int x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
            ChunkPos chunk = new ChunkPos(x >> 4, z >> 4);
            if (!level.getChunkSource().hasChunk(chunk.x(), chunk.z())) {
                continue;
            }
            int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, height - 1, z);
            BlockPos pos = new BlockPos(x, height, z);
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState groundState = level.getBlockState(ground);
            BlockState feet = level.getBlockState(pos);
            BlockState head = level.getBlockState(pos.above());
            if (!groundState.isFaceSturdy(level, ground, Direction.UP)) {
                continue;
            }
            if (groundState.liquid() || feet.liquid() || head.liquid()) {
                continue;
            }
            if (!feet.isAir() || !head.isAir()) {
                continue;
            }
            return pos;
        }
        return null;
    }
}
