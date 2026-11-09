package com.mugewrayce.sizeadjuster.payload;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.component.CertEntry;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.component.StaffData;
import com.mugewrayce.sizeadjuster.component.StaffSelectionData;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.magic.MagicDataRules;
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
 * 普通魔法杖当前选择（W12，C2S）。
 *
 * <p>客户端只发送选中的三元组；服务端重新校验主手是普通魔法杖、且
 * {@code (type, mode, sub)} 确实存在于该法杖的 {@link StaffData#entries()} 中，
 * 才写入 {@link StaffSelectionData}。</p>
 */
public record MagicStaffSelectionPayload(String typeId, String mode, String sub)
        implements CustomPacketPayload {

    /** 单个 ID 字段的长度上限（防止异常大包）。 */
    public static final int MAX_ID_LENGTH = 64;

    public static final CustomPacketPayload.Type<MagicStaffSelectionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    SizeAdjusterMod.MOD_ID, "magic_staff_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagicStaffSelectionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, MagicStaffSelectionPayload::typeId,
                    ByteBufCodecs.STRING_UTF8, MagicStaffSelectionPayload::mode,
                    ByteBufCodecs.STRING_UTF8, MagicStaffSelectionPayload::sub,
                    MagicStaffSelectionPayload::new);

    public MagicStaffSelectionPayload {
        typeId = typeId == null ? "" : typeId;
        mode = mode == null ? "" : mode;
        sub = sub == null ? "" : sub;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, MagicStaffSelectionPayload::handle);
    }

    private static void handle(MagicStaffSelectionPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() != MagicItems.magicStaff) {
                return;
            }
            if (!isValidId(payload.typeId()) || !isValidId(payload.mode()) || !isValidId(payload.sub())) {
                return;
            }
            if (payload.typeId().isEmpty() || payload.mode().isEmpty()) {
                return;
            }
            StaffData staff = stack.getOrDefault(ModComponents.STAFF_DATA, StaffData.DEFAULT);
            boolean found = false;
            for (CertEntry entry : staff.entries()) {
                if (entry.type().equals(payload.typeId())
                        && entry.mode().equals(payload.mode())
                        && entry.sub().equals(payload.sub())
                        && MagicDataRules.isValidEntry(entry)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return;
            }
            stack.set(ModComponents.STAFF_SELECTION_DATA,
                    new StaffSelectionData(payload.typeId(), payload.mode(), payload.sub()));
            ComponentSync.markChanged(player);
        });
    }

    private static boolean isValidId(String value) {
        return value != null && value.length() <= MAX_ID_LENGTH;
    }
}
