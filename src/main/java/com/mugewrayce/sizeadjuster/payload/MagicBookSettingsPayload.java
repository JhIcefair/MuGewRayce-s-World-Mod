package com.mugewrayce.sizeadjuster.payload;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.component.MagicBookData;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 魔法书设置（W12，C2S）。
 *
 * <p>服务端校验主手是魔法书、技能/范围/坐标/排除列表合法后整体写入
 * {@link MagicBookData}。W12 只存数据；传送/时间暂停效果、耗魔、范围结算留 W14。</p>
 */
public record MagicBookSettingsPayload(String skill, int range, int targetX, int targetY, int targetZ,
                                       List<String> excluded) implements CustomPacketPayload {

    /** 排除列表条目上限与单条长度上限。 */
    public static final int MAX_EXCLUDED = 64;
    public static final int MAX_EXCLUDED_LENGTH = 128;
    /** 坐标绝对值上限（避免异常值）。 */
    public static final int MAX_COORDINATE = 30_000_000;

    public static final CustomPacketPayload.Type<MagicBookSettingsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    SizeAdjusterMod.MOD_ID, "magic_book_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagicBookSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, MagicBookSettingsPayload::skill,
                    ByteBufCodecs.INT, MagicBookSettingsPayload::range,
                    ByteBufCodecs.INT, MagicBookSettingsPayload::targetX,
                    ByteBufCodecs.INT, MagicBookSettingsPayload::targetY,
                    ByteBufCodecs.INT, MagicBookSettingsPayload::targetZ,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    MagicBookSettingsPayload::excluded,
                    MagicBookSettingsPayload::new);

    /** 防御性复制；null 列表落空列表。 */
    public MagicBookSettingsPayload {
        skill = skill == null ? "" : skill;
        excluded = excluded == null ? List.of() : List.copyOf(excluded);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, MagicBookSettingsPayload::handle);
    }

    private static void handle(MagicBookSettingsPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() != MagicItems.magicBook) {
                return;
            }
            if (!MagicBookData.isValidSkill(payload.skill())
                    || !MagicBookData.isValidRange(payload.skill(), payload.range())) {
                return;
            }
            if (!isValidCoordinate(payload.targetX())
                    || !isValidCoordinate(payload.targetY())
                    || !isValidCoordinate(payload.targetZ())) {
                return;
            }
            List<String> excluded = sanitizeExcluded(payload.excluded());
            if (excluded == null) {
                return;
            }
            stack.set(ModComponents.MAGIC_BOOK_DATA,
                    new MagicBookData(payload.skill(), payload.range(),
                            payload.targetX(), payload.targetY(), payload.targetZ(), excluded));
            ComponentSync.markChanged(player);
        });
    }

    private static boolean isValidCoordinate(int value) {
        return Math.abs((long) value) <= MAX_COORDINATE;
    }

    /**
     * 校验并复制排除列表：只接受 {@code entity:<uuid>} / {@code type:<id>}，
     * 拒绝 null、空串、无前缀、前后缀为空、超长或条数超限。
     */
    private static List<String> sanitizeExcluded(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw.size() > MAX_EXCLUDED) {
            return null;
        }
        List<String> cleaned = new ArrayList<>(raw.size());
        for (String value : raw) {
            if (value == null || value.length() > MAX_EXCLUDED_LENGTH) {
                return null;
            }
            String trimmed = value.trim();
            if (trimmed.length() < 4) {
                return null;
            }
            if (trimmed.startsWith("entity:") && trimmed.length() > 7) {
                cleaned.add(trimmed);
            } else if (trimmed.startsWith("type:") && trimmed.length() > 5) {
                cleaned.add(trimmed);
            } else {
                return null;
            }
        }
        return List.copyOf(cleaned);
    }
}
