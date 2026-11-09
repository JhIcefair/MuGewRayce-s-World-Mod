package com.mugewrayce.sizeadjuster.magic.cast;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 闪光技能的 30 秒光源方块回收（W14b）。
 *
 * <p>放置 {@code Blocks.LIGHT}（亮度 15）后记录到期 tick；服务器每 tick 检查，
 * 到期且该位置仍是光源方块才恢复为空气，避免误删玩家后来放的方块。</p>
 */
public final class FlashLightTracker {

    /** 维度 → (位置 → 到期 gameTime)。 */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> LIGHTS = new HashMap<>();

    private FlashLightTracker() {
    }

    public static void place(ServerLevel level, BlockPos pos, int ticks) {
        LIGHTS.computeIfAbsent(level.dimension(), key -> new HashMap<>())
                .put(pos.immutable(), level.getGameTime() + ticks);
    }

    public static void tick(MinecraftServer server) {
        if (LIGHTS.isEmpty()) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Map<BlockPos, Long> lights = LIGHTS.get(level.dimension());
            if (lights == null || lights.isEmpty()) {
                continue;
            }
            long now = level.getGameTime();
            Iterator<Map.Entry<BlockPos, Long>> iterator = lights.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Long> entry = iterator.next();
                if (now < entry.getValue()) {
                    continue;
                }
                if (level.getBlockState(entry.getKey()).is(Blocks.LIGHT)) {
                    level.setBlockAndUpdate(entry.getKey(), Blocks.AIR.defaultBlockState());
                }
                iterator.remove();
            }
            if (lights.isEmpty()) {
                LIGHTS.remove(level.dimension());
            }
        }
    }

    public static void clearAll() {
        LIGHTS.clear();
    }

    /** 服务器停止前移除仍亮着的光源方块，避免 30 秒光源被存档永久保留。 */
    public static void clearAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Map<BlockPos, Long> lights = LIGHTS.remove(level.dimension());
            if (lights == null) {
                continue;
            }
            for (BlockPos pos : lights.keySet()) {
                if (level.getBlockState(pos).is(Blocks.LIGHT)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
        }
        LIGHTS.clear();
    }
}
