package com.mugewrayce.sizeadjuster.magic.mana;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * 区块魔力取样接口（W12 建，W13 接真实值）。
 *
 * <p>探测器只需要 25 个 int：以请求中心区块为中心，行优先 NW→SE（上北下南、左西右东）。
 * W12 统一返回 {@link #PLACEHOLDER}（界面显示 {@code --}）；W13 起改为
 * {@link ManaService#probe25} 的真实区块魔力。<b>签名不变</b>，{@code ManaDetectorRequests}
 * 与界面一行不动。</p>
 */
public final class ManaProbe {

    public static final int SAMPLE_SIZE = 25;

    /**
     * W12 占位值，仅保留给 W12 静态检查引用；W13 起 {@link #sample} 不再返回它。
     *
     * @deprecated 真实值见 {@link ManaService#probe25}
     */
    @Deprecated
    public static final int PLACEHOLDER = -1;

    private ManaProbe() {
    }

    /** 请求瞬间取样 25 个真实区块魔力值（行优先 NW→SE）。 */
    public static int[] sample(ServerLevel level, int centerChunkX, int centerChunkZ) {
        return ManaService.probe25(level, new ChunkPos(centerChunkX, centerChunkZ));
    }
}
