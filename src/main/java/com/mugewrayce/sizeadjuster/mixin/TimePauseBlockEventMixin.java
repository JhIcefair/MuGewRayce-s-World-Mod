package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.magic.cast.TimePauseZone;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 时间暂停 W20：冻结区块内的方块事件不执行（活塞伸缩、音符盒、箱子开合等）。
 *
 * <p>{@code ServerLevel#runBlockEvents} 逐个调 {@code doBlockEvent}；只拦这一层，
 * 不 cancel {@code runBlockEvents()}（那会误伤整个维度）。</p>
 */
@Mixin(ServerLevel.class)
public abstract class TimePauseBlockEventMixin {

    @Inject(method = "doBlockEvent", at = @At("HEAD"), cancellable = true)
    private void mugewrayce$skipFrozenBlockEvent(BlockEventData data, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (data != null && TimePauseZone.isFrozen(self, data.pos())) {
            cir.setReturnValue(false);
        }
    }
}
