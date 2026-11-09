package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.magic.cast.TimePauseZone;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时间暂停：区域内非生物实体（掉落物 / TNT / 投射物等）跳过 tick（W14b）。
 *
 * <p>生物由 {@link TimePauseLivingEntityMixin} 处理；玩家不在此列。</p>
 */
@Mixin(Entity.class)
public abstract class TimePauseEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void mugewrayce$freezeEntity(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity) {
            return;
        }
        if (TimePauseZone.isFrozen(self.level(), self)) {
            ci.cancel();
        }
    }
}
