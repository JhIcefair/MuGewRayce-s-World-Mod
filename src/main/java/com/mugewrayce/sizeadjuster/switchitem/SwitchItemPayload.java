package com.mugewrayce.sizeadjuster.switchitem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * 切换物品的网络包（客户端 → 服务端）。
 * 携带目标物品的注册 ID，服务端将玩家主手物品替换为目标物品（方式 A：直接变形，无需背包拥有）。
 */
public record SwitchItemPayload(Identifier targetItem) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SwitchItemPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("mugewrayce-mod", "switch_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchItemPayload> STREAM_CODEC =
            StreamCodec.composite(Identifier.STREAM_CODEC, SwitchItemPayload::targetItem, SwitchItemPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, SwitchItemPayload::handle);
    }

    private static void handle(SwitchItemPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            ItemStack held = player.getMainHandItem();
            if (!SwitchItems.isSwitchable(held)) {
                return;
            }
            Item target = BuiltInRegistries.ITEM.getValue(payload.targetItem());
            if (target == null || !SwitchItems.ORDER.contains(target) || target == held.getItem()) {
                return;
            }
            player.getInventory().setItem(player.getInventory().getSelectedSlot(),
                    new ItemStack(target, held.getCount()));
        });
    }
}
