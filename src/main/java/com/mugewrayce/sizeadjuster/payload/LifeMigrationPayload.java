package com.mugewrayce.sizeadjuster.payload;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.magic.SoundFx;
import com.mugewrayce.sizeadjuster.util.ComponentSync;
import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * 生命迁移石确认（C2S，W12）。
 *
 * <p>服务端校验主手仍是迁移石、目标只能是默认(0) / 有生命的魔法体(1)；
 * 只有真正发生角色变化时才应用并消耗 1 个。切到当前已有角色视为无变化，不消耗。
 * 失败/非法 → 不消耗、不切换。</p>
 */
public record LifeMigrationPayload(int role) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LifeMigrationPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    SizeAdjusterMod.MOD_ID, "life_migration"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LifeMigrationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, LifeMigrationPayload::role,
                    LifeMigrationPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, LifeMigrationPayload::handle);
    }

    private static void handle(LifeMigrationPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() != MagicItems.lifeMigrationStone) {
                return;
            }
            int role = payload.role();
            if (role != PlayerRoles.ROLE_DEFAULT && role != PlayerRoles.ROLE_MAGIC_BODY) {
                return;
            }
            int current = PlayerRoles.getRole(player);
            if (current == role) {
                // 无变化：不消耗、不切换；仍回发当前角色供界面刷新。
                ServerPlayNetworking.send(player, new RoleStatusPayload(current));
                return;
            }
            PlayerRoles.applyRole(player, role);
            stack.shrink(1);
            ComponentSync.markChanged(player);
            SoundFx.at(player, SoundEvents.SOUL_SAND_PLACE, 0.9F, 1.0F);
            ServerPlayNetworking.send(player, new RoleStatusPayload(PlayerRoles.getRole(player)));
        });
    }
}
