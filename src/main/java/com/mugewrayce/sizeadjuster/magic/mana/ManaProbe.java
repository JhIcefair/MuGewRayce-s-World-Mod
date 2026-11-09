package com.mugewrayce.sizeadjuster.magic.mana;

import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;

/**
 * 区块魔力取样接口（W12 stub）。
 *
 * <p>探测器只需要 25 个 int：以请求中心区块为中心，行优先 NW→SE。
 * W12 没有真实区块魔力，统一返回 -1（界面显示 {@code --}）；W13 实现
 * 8 区块粗网格双线性分布后替换本方法的取样逻辑，调用方不变。</p>
 */
public final class ManaProbe {

    public static final int SAMPLE_SIZE = 25;
    public static final int PLACEHOLDER = -1;

    private ManaProbe() {
    }

    /** 请求瞬间取样 25 个值；W12 全部占位。 */
    public static int[] sample(ServerLevel level, int centerChunkX, int centerChunkZ) {
        int[] values = new int[SAMPLE_SIZE];
        Arrays.fill(values, PLACEHOLDER);
        return values;
    }
}
