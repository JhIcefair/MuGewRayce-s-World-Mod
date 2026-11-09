package com.mugewrayce.sizeadjuster.magic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import com.mugewrayce.sizeadjuster.SizeAdjusterMod;

/**
 * W14c 两个灰度粒子类型（26.1 实际 API）：
 * {@code magic_point} / {@code magic_line}。
 *
 * <p>26.1 的 {@link ParticleType} 是抽象类，必须给 {@code MapCodec} 与
 * {@code StreamCodec}；客户端注册用 Fabric {@code ParticleProviderRegistry}。</p>
 */
public final class MagicParticles {

    public static final MagicParticleType MAGIC_POINT = new MagicParticleType();
    public static final MagicParticleType MAGIC_LINE = new MagicParticleType();

    private MagicParticles() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("magic_point"), MAGIC_POINT);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("magic_line"), MAGIC_LINE);
    }

    public static ParticleType<MagicParticleOptions> typeFor(int kind) {
        return kind == MagicParticleOptions.KIND_LINE ? MAGIC_LINE : MAGIC_POINT;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, path);
    }

    /** 两个粒子共用的类型实现。 */
    public static final class MagicParticleType extends ParticleType<MagicParticleOptions> {

        private final MapCodec<MagicParticleOptions> codec = MagicParticleOptions.CODEC;
        private final StreamCodec<? super RegistryFriendlyByteBuf, MagicParticleOptions> streamCodec =
                MagicParticleOptions.STREAM_CODEC;

        private MagicParticleType() {
            super(false);
        }

        @Override
        public MapCodec<MagicParticleOptions> codec() {
            return this.codec;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, MagicParticleOptions> streamCodec() {
            return this.streamCodec;
        }
    }
}
