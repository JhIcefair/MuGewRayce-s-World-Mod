package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.entity.EquipoiseFormationEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * W18：原版投射物（箭等）不命中主人的防御魔法阵。
 *
 * <p>{@code Projectile#canHitEntity} 是箭只命中判定的总入口；对“主人的阵”返回 false，
 * 箭直接穿过。自模组魔法球另在 {@code AbstractStraightProjectile} 里做了同样处理。</p>
 */
@Mixin(Projectile.class)
public abstract class ProjectileHitOwnShieldMixin {

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void mugewrayce$skipOwnFormation(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Projectile self = (Projectile) (Object) this;
        if (target instanceof EquipoiseFormationEntity formation && formation.isOwner(self.getOwner())) {
            cir.setReturnValue(false);
        }
    }
}
