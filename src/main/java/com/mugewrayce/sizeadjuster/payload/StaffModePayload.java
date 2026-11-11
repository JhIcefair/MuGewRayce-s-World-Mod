package com.mugewrayce.sizeadjuster.payload;

import com.mugewrayce.sizeadjuster.item.ModItems;
import com.mugewrayce.sizeadjuster.item.StaffCastModes;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 净化法杖施法模式选择（客户端 → 服务端）：W12 起由净化法杖设置 GUI 的「确认」
 * 或 Enter 发送；服务端校验主手仍是净化法杖后，按玩家记录模式（StaffCastModes）。
 *
 * <p>W19：模式编号范围为 {@code 0..14}（7~14 是 8 个自然魔法技能）；
 * 校验仍在 {@link StaffCastModes#set} 内部按 {@code MODE_COUNT} 进行，本类逻辑未变。</p>
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
        context.server().execute(() -> {
            if (context.player().getMainHandItem().getItem() != ModItems.equipoiseStaff) {
                return;
            }
            StaffCastModes.set(context.player(), payload.mode());
        });
    }
}
