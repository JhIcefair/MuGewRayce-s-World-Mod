package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 魔法试炼晋级认证数据（接口约定 §2，组件 ID {@code promotion_data}）。
 *
 * <p>与"魔法类型 + 模式 + 具体项 + 目标等级"绑定，**不含编号**、不绑定具体学习证；
 * `targetLevel > 当前等级` 才可应用（草案 §3.24）。</p>
 */
public record PromotionData(String type, String mode, String sub, int targetLevel) {

    public static final PromotionData DEFAULT = new PromotionData("", "", "", 0);

    public static final Codec<PromotionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type", "").forGetter(PromotionData::type),
            Codec.STRING.optionalFieldOf("mode", "").forGetter(PromotionData::mode),
            Codec.STRING.optionalFieldOf("sub", "").forGetter(PromotionData::sub),
            Codec.intRange(0, 5).optionalFieldOf("targetLevel", 0).forGetter(PromotionData::targetLevel)
    ).apply(instance, PromotionData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PromotionData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, PromotionData::type,
                    ByteBufCodecs.STRING_UTF8, PromotionData::mode,
                    ByteBufCodecs.STRING_UTF8, PromotionData::sub,
                    ByteBufCodecs.INT, PromotionData::targetLevel,
                    PromotionData::new);
}
