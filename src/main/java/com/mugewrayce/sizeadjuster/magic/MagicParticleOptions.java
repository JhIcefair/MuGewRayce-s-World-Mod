package com.mugewrayce.sizeadjuster.magic;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * W14c 粒子参数：灰度贴图 + 代码染色 + 缩放 + 点式/线式种类。
 *
 * <p>两个 {@code ParticleType}（{@code magic_point} / {@code magic_line}）共用本参数；
 * {@link #getType()} 按 {@code kind} 返回已注册的类型。</p>
 */
public record MagicParticleOptions(int color, float scale, int kind) implements ParticleOptions {

    public static final int KIND_POINT = 0;
    public static final int KIND_LINE = 1;

    public static final MapCodec<MagicParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.INT.optionalFieldOf("color", 0xFFFFFFFF)
                    .forGetter(MagicParticleOptions::color),
            com.mojang.serialization.Codec.FLOAT.optionalFieldOf("scale", 1.0F)
                    .forGetter(MagicParticleOptions::scale),
            com.mojang.serialization.Codec.INT.optionalFieldOf("kind", KIND_POINT)
                    .forGetter(MagicParticleOptions::kind)
    ).apply(instance, MagicParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagicParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, MagicParticleOptions::color,
                    ByteBufCodecs.FLOAT, MagicParticleOptions::scale,
                    ByteBufCodecs.INT, MagicParticleOptions::kind,
                    MagicParticleOptions::new);

    public MagicParticleOptions {
        scale = Math.max(0.05F, Math.min(4.0F, scale));
        kind = kind == KIND_LINE ? KIND_LINE : KIND_POINT;
    }

    @Override
    public ParticleType<?> getType() {
        return MagicParticles.typeFor(kind);
    }
}
