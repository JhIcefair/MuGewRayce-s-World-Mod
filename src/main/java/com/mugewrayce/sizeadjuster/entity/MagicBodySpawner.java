package com.mugewrayce.sizeadjuster.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * 没有生命的魔法体的自然生成调度骨架（W08a）。
 *
 * <p>每 3 分钟（3600 tick）尝试一次；只做和平难度与 mob 生成规则过滤。
 * 区块魔力值概率（0.1% / 5%）、3000/5000 消耗、50/80 生命分档全部留 W13，
 * 本任务<b>不伪造魔力值、不实际生成</b>，避免做出与草案不符的临时数值。</p>
 */
public final class MagicBodySpawner {

    /** 每 3 分钟尝试一次（草案 §5.1，2026-10-09 由 20 秒改为 3 分钟）。 */
    public static final int ATTEMPT_INTERVAL_TICKS = 20 * 180;

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

        if (server.getWorldData().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.getGameRules().get(GameRules.SPAWN_MOBS)
                    || !level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
                continue;
            }
            // W13：按所在区块魔力值决定概率/消耗/生命分档，并在合法位置生成。
            // W08 只保留调度与过滤骨架，不在这里写入临时魔力数值。
        }
    }
}
