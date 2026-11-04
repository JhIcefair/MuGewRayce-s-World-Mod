package com.mugewrayce.sizeadjuster.network;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * W04 测试状态包（服务端 → 客户端，接口约定 §3.1）。
 *
 * <p><b>临时测试包</b>：服务端对 {@link W04TestIntentPayload} 的重算结果；
 * {@code requestId} 原样回显，方便连续请求 / 多人环境排查。最迟 W23 清理。</p>
 *
 * <p>字段：{@code requestId} 请求号、{@code accepted} 是否接受、{@code value} 重算后的值
 * （拒绝时为 0）、{@code reason} 原因码（{@link W04TestProtocol} 的 REASON_*）。</p>
 */
public record W04TestStatePayload(int requestId, boolean accepted, int value, int reason)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<W04TestStatePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "w04_test_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, W04TestStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, W04TestStatePayload::requestId,
                    ByteBufCodecs.BOOL, W04TestStatePayload::accepted,
                    ByteBufCodecs.INT, W04TestStatePayload::value,
                    ByteBufCodecs.INT, W04TestStatePayload::reason,
                    W04TestStatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
