package com.mugewrayce.sizeadjuster.magic.mana;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 消耗的三种位置判定（九宫格，W13，接口约定 §3.6 / 草案 §7.5 + 批注 A-12）。
 *
 * <p>区块内局部坐标 {@code lx = floorMod(x, 16)}、{@code lz = floorMod(z, 16)}，按 2 格宽的
 * 边界带切成 3×3：</p>
 *
 * <pre>
 *   中间   lx 2..13 × lz 2..13        → 只扣本区块
 *   边缘   一侧 lx 或 lz ∈ 0..1 / 14..15，另一侧 2..13 → 本区块 + 该方向邻块（2 个）
 *   边角   四个 2×2 角                → 本区块 + 两邻 + 对角（4 个）
 * </pre>
 *
 * <p>一次性分摊：{@code base = amount / n}，{@code rem = amount % n}，**余数全加在所在区块**
 * （边缘时等价于“较大的一份扣在所在区块”）。持续消耗按“本 → 邻 → 本 …”轮流，边角的
 * “邻”从剩余三个里随机取；轮到的区块一次承担整笔 amount。</p>
 *
 * <p>全部用整数坐标判定，<b>不要用几何距离</b>（草案 A-12 原话）。纯函数，便于自检。</p>
 */
public final class ManaPosition {

    /** 区块边长。 */
    public static final int CHUNK_SIZE = 16;

    /** 九宫格区域。 */
    public enum Region {
        MIDDLE,
        EDGE_WEST,
        EDGE_EAST,
        EDGE_NORTH,
        EDGE_SOUTH,
        CORNER_NW,
        CORNER_NE,
        CORNER_SW,
        CORNER_SE;

        public boolean isMiddle() {
            return this == MIDDLE;
        }

        public boolean isCorner() {
            return this.ordinal() >= CORNER_NW.ordinal();
        }

        /** 一次性消耗时参与的区块数：中间 1 / 边缘 2 / 边角 4。 */
        public int participants() {
            return isMiddle() ? 1 : (isCorner() ? 4 : 2);
        }
    }

    /**
     * 一次性分摊结果。
     *
     * @param chunks     参与区块，{@code chunks.get(ownerIndex)} 是使用者所在区块
     * @param ownerIndex 所在区块下标
     * @param amounts    与 {@code chunks} 同长的扣减量，合计等于原始 amount
     */
    public record ManaSplit(List<ChunkPos> chunks, int ownerIndex, int[] amounts) {

        public ManaSplit {
            chunks = List.copyOf(chunks);
            amounts = amounts.clone();
        }

        @Override
        public int[] amounts() {
            return this.amounts.clone();
        }

        public int size() {
            return this.chunks.size();
        }

        public ChunkPos ownerChunk() {
            return this.chunks.get(this.ownerIndex);
        }
    }

    private ManaPosition() {
    }

    /** 世界坐标 → 区块内局部 x（0..15，含负坐标）。 */
    public static int localX(double worldX) {
        return Math.floorMod(Mth.floor(worldX), CHUNK_SIZE);
    }

    /** 世界坐标 → 区块内局部 z（0..15，含负坐标）。 */
    public static int localZ(double worldZ) {
        return Math.floorMod(Mth.floor(worldZ), CHUNK_SIZE);
    }

    /** 九宫格判定：整数局部坐标，2 格带宽。 */
    public static Region regionOf(int localX, int localZ) {
        boolean west = localX <= 1;
        boolean east = localX >= CHUNK_SIZE - 2;
        boolean north = localZ <= 1;
        boolean south = localZ >= CHUNK_SIZE - 2;
        if (!west && !east && !north && !south) {
            return Region.MIDDLE;
        }
        if ((west || east) && (north || south)) {
            if (west) {
                return north ? Region.CORNER_NW : Region.CORNER_SW;
            }
            return north ? Region.CORNER_NE : Region.CORNER_SE;
        }
        if (west) {
            return Region.EDGE_WEST;
        }
        if (east) {
            return Region.EDGE_EAST;
        }
        return north ? Region.EDGE_NORTH : Region.EDGE_SOUTH;
    }

    /** 参与区块列表：所在区块永远在下标 0，其余按“西/东/北/南/对角”固定顺序。 */
    public static ManaSplit participants(ChunkPos owner, Region region) {
        List<ChunkPos> chunks = new ArrayList<>(region.participants());
        chunks.add(owner);
        int ownerX = owner.x();
        int ownerZ = owner.z();
        switch (region) {
            case MIDDLE -> {
                // 只有所在区块
            }
            case EDGE_WEST -> chunks.add(new ChunkPos(ownerX - 1, ownerZ));
            case EDGE_EAST -> chunks.add(new ChunkPos(ownerX + 1, ownerZ));
            case EDGE_NORTH -> chunks.add(new ChunkPos(ownerX, ownerZ - 1));
            case EDGE_SOUTH -> chunks.add(new ChunkPos(ownerX, ownerZ + 1));
            case CORNER_NW -> {
                chunks.add(new ChunkPos(ownerX - 1, ownerZ));
                chunks.add(new ChunkPos(ownerX, ownerZ - 1));
                chunks.add(new ChunkPos(ownerX - 1, ownerZ - 1));
            }
            case CORNER_NE -> {
                chunks.add(new ChunkPos(ownerX + 1, ownerZ));
                chunks.add(new ChunkPos(ownerX, ownerZ - 1));
                chunks.add(new ChunkPos(ownerX + 1, ownerZ - 1));
            }
            case CORNER_SW -> {
                chunks.add(new ChunkPos(ownerX - 1, ownerZ));
                chunks.add(new ChunkPos(ownerX, ownerZ + 1));
                chunks.add(new ChunkPos(ownerX - 1, ownerZ + 1));
            }
            case CORNER_SE -> {
                chunks.add(new ChunkPos(ownerX + 1, ownerZ));
                chunks.add(new ChunkPos(ownerX, ownerZ + 1));
                chunks.add(new ChunkPos(ownerX + 1, ownerZ + 1));
            }
        }
        return new ManaSplit(chunks, 0, new int[chunks.size()]);
    }

    /** 一次性分摊：余数全加在所在区块。 */
    public static ManaSplit split(ChunkPos owner, int localX, int localZ, int amount) {
        ManaSplit base = participants(owner, regionOf(localX, localZ));
        int count = base.size();
        int per = amount / count;
        int remainder = amount % count;
        int[] amounts = new int[count];
        Arrays.fill(amounts, per);
        amounts[base.ownerIndex()] += remainder;
        return new ManaSplit(base.chunks(), base.ownerIndex(), amounts);
    }

    /**
     * 持续消耗的轮流目标下标：偶数步 = 所在区块，奇数步 = 邻块（边角时从剩余三个里随机）。
     *
     * @param step 该次施法内的第几次持续扣减（0 起）
     */
    public static int rotationTarget(ManaSplit split, int step, RandomSource random) {
        int others = split.size() - 1;
        int pick = others <= 0 ? 0 : random.nextInt(others);
        return rotationTarget(split, step, pick);
    }

    /** {@link #rotationTarget(ManaSplit, int, RandomSource)} 的纯函数版本（自检直接给随机下标）。 */
    static int rotationTarget(ManaSplit split, int step, int randomPick) {
        int count = split.size();
        if (count <= 1 || Math.floorMod(step, 2) == 0) {
            return split.ownerIndex();
        }
        int others = count - 1;
        int wanted = Math.floorMod(randomPick, Math.max(1, others));
        int seen = 0;
        for (int index = 0; index < count; index++) {
            if (index == split.ownerIndex()) {
                continue;
            }
            if (seen == wanted) {
                return index;
            }
            seen++;
        }
        return split.ownerIndex();
    }
}
