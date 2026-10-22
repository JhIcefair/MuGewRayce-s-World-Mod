package com.mugewrayce.sizeadjuster.payload;

import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 特殊角色操作请求（客户端 → 服务端），由柃之册/生命转换令牌的角色切换 GUI 发出。
 * 动作：选择角色 / 查询当前角色。
 * 服务端处理后总是回发 RoleStatusPayload（当前角色编号），供 GUI 显示当前角色。
 */
public record RoleActionPayload(int action, int role) implements CustomPacketPayload {

    /** 选择并应用角色。 */
    public static final int ACTION_SELECT = 0;

    /** 查询当前角色（仅查询，不改变）。 */
    public static final int ACTION_QUERY = 1;

    public static final CustomPacketPayload.Type<RoleActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("mugewrayce-mod", "role_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoleActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, RoleActionPayload::action,
                    ByteBufCodecs.INT, RoleActionPayload::role,
                    RoleActionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, RoleActionPayload::handle);
    }

    private static void handle(RoleActionPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (payload.action() == ACTION_SELECT) {
                PlayerRoles.applyRole(player, payload.role());
            }
            // 回发当前角色编号（选择后为最新值）
            ServerPlayNetworking.send(player, new RoleStatusPayload(PlayerRoles.getRole(player)));
        });
    }
}
