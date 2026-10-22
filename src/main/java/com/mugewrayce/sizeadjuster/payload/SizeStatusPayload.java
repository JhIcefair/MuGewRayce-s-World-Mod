package com.mugewrayce.sizeadjuster.payload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 玩家尺寸当前状态（服务端 → 客户端）：服务端对 SizeActionPayload 的处理结果，
 * 携带 SizeAttributes.getStatusLines 的多行文本组件，供柃之册尺寸 GUI 左侧面板显示。
 * 文本组件由客户端按自身语言渲染，因此中英文客户端都能看到本地化内容。
 */
public record SizeStatusPayload(List<Component> lines) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SizeStatusPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("mugewrayce-mod", "size_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SizeStatusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SizeStatusPayload::lines,
                    SizeStatusPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
    }
}
