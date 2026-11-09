package com.mugewrayce.sizeadjuster.payload;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.component.MagicBookData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.magic.cast.BookCaster;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 魔法书释放（W14b，C2S，无字段；接口约定 §3.7）。
 *
 * <p>服务端重新读 {@code magic_book_data} 并校验主手/技能/范围/耗魔；
 * 客户端只负责在二级界面「确认」时先发 settings、再发本包。</p>
 */
public record MagicBookCastPayload() implements CustomPacketPayload {

    public static final MagicBookCastPayload INSTANCE = new MagicBookCastPayload();

    public static final CustomPacketPayload.Type<MagicBookCastPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    SizeAdjusterMod.MOD_ID, "magic_book_cast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagicBookCastPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, MagicBookCastPayload::handle);
    }

    private static void handle(MagicBookCastPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() != MagicItems.magicBook) {
                return;
            }
            MagicBookData data = stack.getOrDefault(ModComponents.MAGIC_BOOK_DATA, MagicBookData.DEFAULT);
            BookCaster.cast(player, data);
        });
    }
}
