package com.mugewrayce.sizeadjuster.payload;

import com.mugewrayce.sizeadjuster.item.StaffCastModes;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 净化法杖施法模式选择（客户端 → 服务端）：切换 GUI 底部功能行点击后发送，
 * 服务端按玩家记录模式（StaffCastModes），法杖据此决定释放方式。
 */
public record StaffModePayload(int mode) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StaffModePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("mugewrayce-mod", "staff_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StaffModePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, StaffModePayload::mode,
                    StaffModePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, StaffModePayload::handle);
    }

    private static void handle(StaffModePayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() ->
                StaffCastModes.set(context.player(), payload.mode()));
    }
}
