package com.mugewrayce.sizeadjuster.client.particle;

import com.mugewrayce.sizeadjuster.magic.MagicParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

/**
 * W14c 灰度粒子（magic_point / magic_line）：颜色与缩放来自
 * {@link MagicParticleOptions}，贴图只提供形状。
 */
public class MagicParticleProvider implements ParticleProvider<MagicParticleOptions> {

    private final SpriteSet sprites;

    public MagicParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Override
    public Particle createParticle(MagicParticleOptions options, ClientLevel level,
                                   double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed,
                                   RandomSource random) {
        return new MagicParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options,
                this.sprites.first(), random);
    }

    /** 实际粒子：半透明、全亮、短寿命、无重力。 */
    private static final class MagicParticle extends SingleQuadParticle {

        private MagicParticle(ClientLevel level, double x, double y, double z,
                              double xSpeed, double ySpeed, double zSpeed,
                              MagicParticleOptions options, TextureAtlasSprite sprite,
                              RandomSource random) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite);
            this.lifetime = 8 + random.nextInt(6);
            this.gravity = 0.0F;
            this.friction = 0.94F;
            this.quadSize = 0.10F * options.scale();
            this.setParticleSpeed(xSpeed * 0.15D, ySpeed * 0.15D, zSpeed * 0.15D);
            int color = options.color();
            this.setColor(((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F);
            this.setAlpha(1.0F);
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }

        /** 自发光：全亮，不受世界光照影响。 */
        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }
    }
}
