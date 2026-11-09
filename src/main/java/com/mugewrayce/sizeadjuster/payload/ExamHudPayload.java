package com.mugewrayce.sizeadjuster.payload;

import com.mojang.datafixers.util.Function8;
import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 考试 HUD（S2C，W15，接口约定 §3.9）。
 *
 * <p>{@code state}：0 = 无（清除 HUD）/ 1 = 进行 / 2 = 通过 / 3 = 失败。
 * <b>只发给发起者</b>，不广播（§6 坑 7）。</p>
 *
 * <p>字段名带 {@code Id} 后缀是为了避开 {@link CustomPacketPayload#type()} 的同名冲突；
 * 线上顺序仍是 state / remainingSeconds / type / mode / sub / targetLevel / chest / dimension。</p>
 */
public record ExamHudPayload(
        int state,
        int remainingSeconds,
        String typeId,
        String modeId,
        String subId,
        int targetLevel,
        BlockPos chest,
        Identifier dimension) implements CustomPacketPayload {

    public static final int STATE_NONE = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_PASSED = 2;
    public static final int STATE_FAILED = 3;

    public static final CustomPacketPayload.Type<ExamHudPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    SizeAdjusterMod.MOD_ID, "exam_hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExamHudPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, ExamHudPayload::state,
                    ByteBufCodecs.INT, ExamHudPayload::remainingSeconds,
                    ByteBufCodecs.STRING_UTF8, ExamHudPayload::typeId,
                    ByteBufCodecs.STRING_UTF8, ExamHudPayload::modeId,
                    ByteBufCodecs.STRING_UTF8, ExamHudPayload::subId,
                    ByteBufCodecs.INT, ExamHudPayload::targetLevel,
                    BlockPos.STREAM_CODEC, ExamHudPayload::chest,
                    Identifier.STREAM_CODEC, ExamHudPayload::dimension,
                    (Function8<Integer, Integer, String, String, String, Integer, BlockPos, Identifier,
                            ExamHudPayload>) ExamHudPayload::new);

    public ExamHudPayload {
        typeId = typeId == null ? "" : typeId;
        modeId = modeId == null ? "" : modeId;
        subId = subId == null ? "" : subId;
        dimension = dimension == null ? Identifier.fromNamespaceAndPath("minecraft", "overworld") : dimension;
        chest = chest == null ? BlockPos.ZERO : chest;
    }

    /** 清除 HUD 的空包。 */
    public static ExamHudPayload none(Identifier dimension) {
        return new ExamHudPayload(STATE_NONE, 0, "", "", "", 0, BlockPos.ZERO, dimension);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** S2C 注册（服务端与客户端共同初始化）。 */
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
    }
}
