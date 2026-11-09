package com.mugewrayce.sizeadjuster.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 【草案外临时要求】音效复用入口（W16 §0）。
 *
 * <p>只播放 {@code SoundEvents} 里现成的原版音效；不注册 SoundEvent、不加 sounds.json / ogg。
 * 统一在服务端调用，客户端由原版声音包自动同步。</p>
 */
public final class SoundFx {

    private SoundFx() {
    }

    public static void at(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        if (level != null && !level.isClientSide() && sound != null) {
            level.playSound(null, pos, sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    public static void at(Level level, double x, double y, double z,
                          SoundEvent sound, float volume, float pitch) {
        if (level != null && !level.isClientSide() && sound != null) {
            level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    public static void at(Entity entity, SoundEvent sound, float volume, float pitch) {
        if (entity != null) {
            at(entity.level(), entity.getX(), entity.getY(), entity.getZ(), sound, volume, pitch);
        }
    }
}
