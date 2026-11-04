package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 学习证 / 魔法杖里的一条学习记录（接口约定 §2 / §2.1）。
 *
 * <p>字段：{@code type} / {@code mode} / {@code sub} 都是稳定 ID 字符串
 * （{@code MagicType.id()} / {@code CastMode.id()} / {@code CastSubMode.id()} / {@code MagicSkill.id()}），
 * <b>不用 enum name() / ordinal</b>；{@code level} 0=初学者、1~5=I~V。</p>
 *
 * <p>技能固定表示：{@code mode="skill"}、{@code sub=MagicSkill.id()}、{@code level=0}。
 * 字段缺失一律取默认值（空串 / 0），合法性由 {@code MagicDataRules} 判定。</p>
 */
public record CertEntry(String type, String mode, String sub, int level) {

    /** 空记录（全字段默认值）。 */
    public static final CertEntry EMPTY = new CertEntry("", "", "", 0);

    public static final Codec<CertEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type", "").forGetter(CertEntry::type),
            Codec.STRING.optionalFieldOf("mode", "").forGetter(CertEntry::mode),
            Codec.STRING.optionalFieldOf("sub", "").forGetter(CertEntry::sub),
            Codec.intRange(0, 5).optionalFieldOf("level", 0).forGetter(CertEntry::level)
    ).apply(instance, CertEntry::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CertEntry> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CertEntry::type,
                    ByteBufCodecs.STRING_UTF8, CertEntry::mode,
                    ByteBufCodecs.STRING_UTF8, CertEntry::sub,
                    ByteBufCodecs.INT, CertEntry::level,
                    CertEntry::new);
}
