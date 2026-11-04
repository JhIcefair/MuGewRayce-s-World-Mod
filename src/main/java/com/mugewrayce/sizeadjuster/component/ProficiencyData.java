package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 魔法熟练证明数据（接口约定 §2，组件 ID {@code proficiency_data}）。
 *
 * <p>与"魔法类型 + 模式 + 具体项"绑定，**不含编号**、不绑定具体学习证；
 * 用于把学习证上对应模式升到 I 级（草案 §3.22）。字段为稳定 ID 字符串。</p>
 */
public record ProficiencyData(String type, String mode, String sub) {

    public static final ProficiencyData DEFAULT = new ProficiencyData("", "", "");

    public static final Codec<ProficiencyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type", "").forGetter(ProficiencyData::type),
            Codec.STRING.optionalFieldOf("mode", "").forGetter(ProficiencyData::mode),
            Codec.STRING.optionalFieldOf("sub", "").forGetter(ProficiencyData::sub)
    ).apply(instance, ProficiencyData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProficiencyData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ProficiencyData::type,
                    ByteBufCodecs.STRING_UTF8, ProficiencyData::mode,
                    ByteBufCodecs.STRING_UTF8, ProficiencyData::sub,
                    ProficiencyData::new);
}
