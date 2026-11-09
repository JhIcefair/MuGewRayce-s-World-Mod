package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.magic.cast.TimePauseZone;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时间暂停：区域内非玩家生物跳过 tick/AI（W14b）。
 *
 * <p>玩家默认不冻结（服务端强拉会橡皮筋）；需要时由
 * {@link TimePauseZone#TIME_PAUSE_FREEZE_PLAYERS} 打开位置回拉。</p>
 */
@Mixin(LivingEntity.class)
public abstract class TimePauseLivingEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void mugewrayce$freezeLiving(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player) {
            return;
        }
        if (TimePauseZone.isFrozen(self.level(), self)) {
            ci.cancel();
        }
    }
}
