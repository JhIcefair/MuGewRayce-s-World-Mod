package com.mugewrayce.sizeadjuster.payload;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.component.CrystalData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.util.ComponentSync;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 普通魔法水晶供能开关（W12，C2S）。
 *
 * <p>服务端校验主手仍是水晶、且不是 {@code auto && !supply} 无效态；
 * 保留原 {@code mana}，只写两个开关。W13 才处理容量/消耗/消失。</p>
 */
public record CrystalSettingsPayload(boolean supplyEnabled, boolean autoSupplyEnabled)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CrystalSettingsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    SizeAdjusterMod.MOD_ID, "crystal_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrystalSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, CrystalSettingsPayload::supplyEnabled,
                    ByteBufCodecs.BOOL, CrystalSettingsPayload::autoSupplyEnabled,
                    CrystalSettingsPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, CrystalSettingsPayload::handle);
    }

    private static void handle(CrystalSettingsPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() != MagicItems.magicCrystal) {
                return;
            }
            if (payload.autoSupplyEnabled() && !payload.supplyEnabled()) {
                return;
            }
            CrystalData old = stack.getOrDefault(ModComponents.CRYSTAL_DATA, CrystalData.DEFAULT);
            stack.set(ModComponents.CRYSTAL_DATA,
                    new CrystalData(old.mana(), payload.supplyEnabled(), payload.autoSupplyEnabled()));
            ComponentSync.markChanged(player);
        });
    }
}
