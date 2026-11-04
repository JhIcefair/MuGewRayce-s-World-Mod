package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 被魔法附加书附加过的武器数据（接口约定 §2，组件 ID {@code attachment_data}）。
 *
 * <p>{@code level} = 附加等级 1~3（I/II/III）；{@code 0} 表示无附加/无效。
 * 覆盖制：新等级必须高于当前等级才允许（草案 §3.25 批注 A-11）。</p>
 */
public record AttachmentData(int level) {

    public static final AttachmentData DEFAULT = new AttachmentData(0);

    public static final Codec<AttachmentData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 3).optionalFieldOf("level", 0).forGetter(AttachmentData::level)
    ).apply(instance, AttachmentData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttachmentData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, AttachmentData::level,
                    AttachmentData::new);
}
