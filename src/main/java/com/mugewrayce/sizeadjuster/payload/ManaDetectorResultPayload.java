package com.mugewrayce.sizeadjuster.payload;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.List;

/**
 * 魔力值探测器结果（S2C，W12）。
 *
 * <p>25 个 int，行优先 NW→SE；W12 占位值为 -1（界面显示 {@code --}），
 * W13 接入真实区块魔力后填 0..30000。{@code complete} 为 true 表示这次探测完成。</p>
 */
public record ManaDetectorResultPayload(int[] values, boolean complete)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ManaDetectorResultPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    SizeAdjusterMod.MOD_ID, "mana_detector_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManaDetectorResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT.apply(ByteBufCodecs.list()),
                    p -> Arrays.stream(p.values()).boxed().toList(),
                    ByteBufCodecs.BOOL, ManaDetectorResultPayload::complete,
                    (list, complete) -> new ManaDetectorResultPayload(
                            list.stream().mapToInt(Integer::intValue).toArray(), complete));

    public ManaDetectorResultPayload {
        values = values == null ? new int[0] : values.clone();
    }

    /** 防御性复制：调用方拿到的是副本。 */
    @Override
    public int[] values() {
        return this.values.clone();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
    }
}
