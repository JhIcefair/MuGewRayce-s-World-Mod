package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.magic.MagicAttachmentCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 魔法附加书：在目标受到物理伤害前追加一份 {@code magic_damage}（W14a）。
 *
 * <p>注入 {@code LivingEntity#hurtServer} HEAD——必须早于 {@code isInvulnerableTo}，
 * 否则魔法体在事件前就被拦下（详见 {@link MagicAttachmentCombat}）。</p>
 */
@Mixin(LivingEntity.class)
public abstract class AttachmentBookDamageMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void mugewrayce$applyAttachmentBonus(ServerLevel level, DamageSource source, float amount,
                                                 CallbackInfoReturnable<Boolean> cir) {
        MagicAttachmentCombat.applyOnHurt((LivingEntity) (Object) this, source, amount);
    }
}
