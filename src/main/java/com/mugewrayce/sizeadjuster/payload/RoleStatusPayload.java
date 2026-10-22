package com.mugewrayce.sizeadjuster.payload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 玩家当前特殊角色（服务端 → 客户端）：服务端对 RoleActionPayload 的处理结果，
 * 携带当前角色编号（0 默认 / 1 魔法体 / 2 起为特殊角色），
 * 供角色切换 GUI 显示当前角色。
 */
public record RoleStatusPayload(int role) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RoleStatusPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("mugewrayce-mod", "role_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoleStatusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, RoleStatusPayload::role,
                    RoleStatusPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
    }
}
