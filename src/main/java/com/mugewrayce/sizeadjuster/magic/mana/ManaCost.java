package com.mugewrayce.sizeadjuster.magic.mana;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * W10 方块功能魔力扣费入口（接口约定 §3.3）。
 *
 * <p><b>W10 只负责调用，真实实现留 W13</b>：W13 才做区块魔力存储、8 区块双线性分布、
 * 九宫格位置判定、恢复与释放。当前四个方法全部 {@code return false}（fail closed），
 * 保证 W13 之前不会白送水晶 / 法杖。</p>
 *
 * <p>W10 只验证调用点与参数：聚合台 30000、附加台合成法杖 30、附加书分支 10；
 * 分支 A（法杖升级）不调用。不要在 W10 里读玩家背包或判断永恒水晶 / 供能水晶。</p>
 */
public final class ManaCost {

    /** 聚合台总消耗：中心 5000 + 周围 8 个区块各 3125 = 30000。 */
    public static final int AGGREGATION_TOTAL = 30000;
    /** 聚合台所在区块。 */
    public static final int AGGREGATION_CENTER = 5000;
    /** 聚合台周围 3×3 除中心外每个区块。 */
    public static final int AGGREGATION_NEIGHBOR = 3125;

    private ManaCost() {
    }

    /** 聚合台预检：30000 是否可扣（W13 实现）。 */
    public static boolean canPayAggregation(ServerLevel level, BlockPos pos) {
        // TODO W13: 读取中心区块 5000 + 周围 8 区块各 3125，原子预检。
        return false;
    }

    /** 聚合台完成扣费：30000，原子（W13 实现）。 */
    public static boolean payAggregation(ServerLevel level, BlockPos pos) {
        // TODO W13: 真实扣除；W10 只保证调用点在完成时。
        return false;
    }

    /** 只检查方块所在区块是否够 amount（W13 实现）。 */
    public static boolean canPayBlockChunk(ServerLevel level, BlockPos pos, int amount) {
        // TODO W13: 只读方块所在区块的魔力值。
        return false;
    }

    /** 只扣方块所在区块 amount（W13 实现）。 */
    public static boolean payBlockChunk(ServerLevel level, BlockPos pos, int amount) {
        // TODO W13: 只扣方块所在区块，不做 3×3 分摊。
        return false;
    }
}
