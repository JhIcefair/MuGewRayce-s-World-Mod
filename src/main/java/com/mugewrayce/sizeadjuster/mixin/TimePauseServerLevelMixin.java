package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.magic.cast.TimePauseZone;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时间暂停：区域内区块跳过随机刻（W14b）。
 *
 * <p>只跳过被冻结区块的 {@code ServerLevel#tickChunk}，不全局停 tick；
 * 植物生长 / 结冰 / 闪电等随机刻一并暂停。</p>
 */
@Mixin(ServerLevel.class)
public abstract class TimePauseServerLevelMixin {

    @Inject(method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
            at = @At("HEAD"), cancellable = true)
    private void mugewrayce$skipFrozenChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (TimePauseZone.isFrozen(self, chunk.getPos())) {
            ci.cancel();
        }
    }
}
