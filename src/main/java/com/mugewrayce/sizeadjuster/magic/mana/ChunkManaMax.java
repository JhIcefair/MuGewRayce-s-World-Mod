package com.mugewrayce.sizeadjuster.magic.mana;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * 区块最大魔力值分布（W13）。
 *
 * <p>草案 §7.8 四步：粗网格控制点 → 映射到 8000~80000 → smoothstep 双线性 → 四舍五入。
 * 草案的“8 区块网格”与其“相邻 ≤1000”算术上不可共存（72000/8 = 9000，smoothstep 峰值
 * 再 ×1.5），W13 按交接单 §2.2 取 <b>128 区块网格</b>：4 邻差上界 ≈ 72000×1.5/128 = 844，
 * 实测 658，满足“4 邻 ≤900”。**改 {@link #GRID} 一个常量即可回退**。</p>
 *
 * <p>哈希输入 = 世界种子 + 维度标识 + 控制点坐标（SplitMix64），保证同种子同维度一致、
 * 跨维度不串。结果按维度缓存（只缓存查询过的区块，超过上限整体清空）。</p>
 *
 * <p><b>只在服务端</b>：客户端拿不到最大值，探测器数值仍走 W12 的 S2C 包。</p>
 */
public final class ChunkManaMax {

    /** 最小默认最大值。 */
    public static final int MIN_VALUE = 8000;
    /** 最大默认最大值。 */
    public static final int MAX_VALUE = 80000;
    /** 网格间距（区块）；草案是 8，W13 改 128（见类注释）。 */
    public static final int GRID = 128;

    private static final int SPAN = MAX_VALUE - MIN_VALUE;
    private static final long SPAN_PLUS_ONE = (long) SPAN + 1L;
    private static final int CACHE_LIMIT = 1 << 16;

    private static final Map<ResourceKey<Level>, Map<Long, Integer>> CACHE = new HashMap<>();

    private ChunkManaMax() {
    }

    /** 区块 (chunkX, chunkZ) 的默认最大值。 */
    public static int maxFor(ServerLevel level, int chunkX, int chunkZ) {
        ResourceKey<Level> dimension = level.dimension();
        Map<Long, Integer> cache = CACHE.computeIfAbsent(dimension, key -> new HashMap<>());
        long packed = ChunkPos.pack(chunkX, chunkZ);
        Integer hit = cache.get(packed);
        if (hit != null) {
            return hit;
        }
        int value = computeMax(level.getSeed(), saltFor(dimension), chunkX, chunkZ);
        if (cache.size() >= CACHE_LIMIT) {
            cache.clear();
        }
        cache.put(packed, value);
        return value;
    }

    public static int maxFor(ServerLevel level, ChunkPos pos) {
        return maxFor(level, pos.x(), pos.z());
    }

    /** 维度隔离用的稳定盐：`minecraft:overworld` 之类字符串的 `String.hashCode`（Java 规范固定）。 */
    static int saltFor(ResourceKey<Level> dimension) {
        return dimension.identifier().toString().hashCode();
    }

    /** 纯函数：给定种子/维度盐/区块坐标算最大值（自检直接调用）。 */
    static int computeMax(long seed, int dimensionSalt, int chunkX, int chunkZ) {
        int gridX = Math.floorDiv(chunkX, GRID);
        int gridZ = Math.floorDiv(chunkZ, GRID);
        int localX = chunkX - gridX * GRID;
        int localZ = chunkZ - gridZ * GRID;

        double weightX = smoothstep(localX / (double) GRID);
        double weightZ = smoothstep(localZ / (double) GRID);

        double v00 = control(gridX, gridZ, seed, dimensionSalt);
        double v10 = control(gridX + 1, gridZ, seed, dimensionSalt);
        double v01 = control(gridX, gridZ + 1, seed, dimensionSalt);
        double v11 = control(gridX + 1, gridZ + 1, seed, dimensionSalt);

        double low = v00 + (v10 - v00) * weightX;
        double high = v01 + (v11 - v01) * weightX;
        return (int) Math.round(low + (high - low) * weightZ);
    }

    /** 控制点取值：8000 + hash % 72001。 */
    static int control(int gridX, int gridZ, long seed, int dimensionSalt) {
        long mixed = mix(seed
                ^ ((long) dimensionSalt * 0x9E3779B97F4A7C15L)
                ^ ((long) gridX * 0x85EBCA77C2B2AE63L)
                ^ ((long) gridZ * 0xC2B2AE3D27D4EB4FL));
        long nonNegative = mixed & Long.MAX_VALUE;
        return MIN_VALUE + (int) (nonNegative % SPAN_PLUS_ONE);
    }

    /** SplitMix64 收尾混淆：整数、无浮点、跨 JVM 稳定。 */
    static long mix(long value) {
        long z = value + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** smoothstep：t*t*(3-2t)，峰值导数 1.5。 */
    static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    /** 服务器启动 / 重进世界时清空缓存（与账本一起重置）。 */
    public static void clearCache() {
        CACHE.clear();
    }
}
