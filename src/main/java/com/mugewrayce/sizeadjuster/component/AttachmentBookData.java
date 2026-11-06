package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 魔法附加书自身等级（W10 新增，接口约定 §2）。
 *
 * <p>{@code level} = 1~3，表示这本书是 I / II / III 哪一档；
 * 旧 W08 造出的附加书没有该组件，读取时按默认 1 处理。
 * 武器上的倍率仍写 {@link AttachmentData}，两者语义不同。</p>
 */
public record AttachmentBookData(int level) {

    public static final AttachmentBookData DEFAULT = new AttachmentBookData(1);

    public static final Codec<AttachmentBookData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 3).optionalFieldOf("level", 1).forGetter(AttachmentBookData::level)
    ).apply(instance, AttachmentBookData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttachmentBookData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, AttachmentBookData::level,
                    AttachmentBookData::new);
}
