package com.mugewrayce.sizeadjuster.magic.mana;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

/**
 * W10 冻结的方块功能魔力扣费入口（接口约定 §3.3；W13 换真实实现）。
 *
 * <p>四条规则（草案 §7.2 / §4.2 / §4.3）：</p>
 * <ul>
 *   <li>聚合台合成水晶：**原子**扣 30000 = 中心区块 5000 + 周围 3×3 除中心 8 个区块各 3125；
 *       <b>不遵循</b>常规九宫格/迁移机制；</li>
 *   <li>附加台「合成魔法杖」30、「附加书给普通武器」10：<b>只扣方块所在区块</b>；
 *       分支 A（法杖升级/附加）不调用；</li>
 *   <li><b>方块功能不免除</b>：永恒水晶 / 开着供能的普通水晶都照扣，所以本类
 *       <b>绝不读玩家背包</b>、也不看锁定状态；</li>
 *   <li>预检与实扣分离：菜单开始前 {@code canPay*}、完成时 {@code pay*}（W10 已定）。</li>
 * </ul>
 */
public final class ManaCost {

    /** 聚合台总消耗：中心 5000 + 周围 8 个区块各 3125 = 30000。 */
    public static final int AGGREGATION_TOTAL = 30000;
    /** 聚合台所在区块。 */
    public static final int AGGREGATION_CENTER = 5000;
    /** 聚合台周围 3×3 除中心外每个区块。 */
    public static final int AGGREGATION_NEIGHBOR = 3125;

    /** 附加台「合成魔法杖」消耗（只扣所在区块）。 */
    public static final int COST_CRAFT_STAFF = 30;
    /** 附加台「附加书给普通武器」消耗（只扣所在区块）。 */
    public static final int COST_ATTACH_BOOK = 10;

    private ManaCost() {
    }

    /** 聚合台预检：中心 5000 + 周围 8 个区块各 3125 是否都够（差 1 点也拒）。 */
    public static boolean canPayAggregation(ServerLevel level, BlockPos pos) {
        ChunkPos center = ChunkPos.containing(pos);
        List<ChunkPos> neighbors = neighborChunks(center);
        int[] values = new int[neighbors.size()];
        for (int i = 0; i < neighbors.size(); i++) {
            values[i] = ManaStorage.get(level, neighbors.get(i));
        }
        return canPayAggregationValues(ManaStorage.get(level, center), values);
    }

    /** 聚合台完成扣费：30000，原子（要么全扣，要么不扣）。 */
    public static boolean payAggregation(ServerLevel level, BlockPos pos) {
        if (!canPayAggregation(level, pos)) {
            return false;
        }
        ChunkPos center = ChunkPos.containing(pos);
        ManaStorage.consume(level, center, AGGREGATION_CENTER);
        for (ChunkPos neighbor : neighborChunks(center)) {
            ManaStorage.consume(level, neighbor, AGGREGATION_NEIGHBOR);
        }
        return true;
    }

    /** 只检查方块所在区块是否够 amount。 */
    public static boolean canPayBlockChunk(ServerLevel level, BlockPos pos, int amount) {
        if (amount <= 0) {
            return true;
        }
        return ManaStorage.get(level, ChunkPos.containing(pos)) >= amount;
    }

    /** 只扣方块所在区块 amount（附加台 30 / 10）；不分摊、不迁移。 */
    public static boolean payBlockChunk(ServerLevel level, BlockPos pos, int amount) {
        return ManaStorage.consume(level, ChunkPos.containing(pos), amount);
    }

    /** 3×3 除中心外的 8 个区块（西→东、北→南固定顺序）。 */
    static List<ChunkPos> neighborChunks(ChunkPos center) {
        List<ChunkPos> neighbors = new ArrayList<>(8);
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                neighbors.add(new ChunkPos(center.x() + dx, center.z() + dz));
            }
        }
        return neighbors;
    }

    /** 纯函数版本（自检用）：给定中心值与 8 个邻居值判断能否扣 30000。 */
    static boolean canPayAggregationValues(int centerValue, int[] neighborValues) {
        if (centerValue < AGGREGATION_CENTER) {
            return false;
        }
        for (int value : neighborValues) {
            if (value < AGGREGATION_NEIGHBOR) {
                return false;
            }
        }
        return true;
    }
}
