package com.mugewrayce.sizeadjuster.switchitem;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 净化系方向键切换（客户端 → 服务端）。
 *
 * <p>客户端只发方向意图（0=↑、1=↓、2=←、3=→），<b>不发送目标物品 ID</b>；
 * 服务端按玩家当前主手 + 方向重算目标，拒绝非法主手 / 越界方向 / 目标等于当前物品，
 * 拒绝时无任何副作用。切换只替换主手该格物品并保留数量。</p>
 *
 * <p>包 ID 与字段见《接口约定》§3.2：{@code mugewrayce-mod:equipoise_switch} / {@code int direction}。</p>
 */
public record EquipoiseSwitchPayload(int direction) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EquipoiseSwitchPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("mugewrayce-mod", "equipoise_switch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EquipoiseSwitchPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, EquipoiseSwitchPayload::direction,
                    EquipoiseSwitchPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, EquipoiseSwitchPayload::handle);
    }

    private static void handle(EquipoiseSwitchPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            ItemStack held = player.getMainHandItem();
            Item target = SwitchItems.targetFor(held, payload.direction());
            if (target == null || target == held.getItem()) {
                return;
            }
            player.getInventory().setItem(player.getInventory().getSelectedSlot(),
                    new ItemStack(target, held.getCount()));
        });
    }
}
