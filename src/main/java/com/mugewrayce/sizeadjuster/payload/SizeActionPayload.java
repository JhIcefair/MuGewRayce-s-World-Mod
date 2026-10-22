package com.mugewrayce.sizeadjuster.payload;

import com.mugewrayce.sizeadjuster.util.SizeAttributes;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 玩家尺寸操作请求（客户端 → 服务端），由柃之册的尺寸 GUI 发出。
 * 动作：应用身高 / 应用特殊组合 / 重置 / 查询当前状态。
 * 服务端处理后总是回发 SizeStatusPayload（当前全部属性值），供 GUI 左侧面板显示。
 */
public record SizeActionPayload(int action, int height) implements CustomPacketPayload {

    public static final int ACTION_APPLY_HEIGHT = 0;
    public static final int ACTION_APPLY_SPECIAL = 1;
    public static final int ACTION_RESET = 2;
    public static final int ACTION_QUERY_STATUS = 3;

    public static final CustomPacketPayload.Type<SizeActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("mugewrayce-mod", "size_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SizeActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SizeActionPayload::action,
                    ByteBufCodecs.INT, SizeActionPayload::height,
                    SizeActionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, SizeActionPayload::handle);
    }

    private static void handle(SizeActionPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            switch (payload.action()) {
                // 服务端二次校验：GUI 已限制 10~1000，但网络包不可信，越界值直接忽略
                case ACTION_APPLY_HEIGHT -> {
                    if (SizeAttributes.isHeightAllowed(payload.height())) {
                        SizeAttributes.applyHeight(player, payload.height());
                    }
                }
                case ACTION_APPLY_SPECIAL -> SizeAttributes.applySpecial(player);
                case ACTION_RESET -> SizeAttributes.resetToDefault(player);
                default -> { /* ACTION_QUERY_STATUS：仅查询 */ }
            }
            // 回发当前全部属性值（多行文本组件，客户端按自身语言渲染）
            ServerPlayNetworking.send(player, new SizeStatusPayload(SizeAttributes.getStatusLines(player)));
        });
    }
}
