package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.entity.AbstractMagicBodyEntity;
import com.mugewrayce.sizeadjuster.magic.MagicArrows;
import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * W18：魔法箭可以命中“没有生命的魔法体”（以及魔法体玩家）。
 *
 * <p>魔法体的 {@code canBeHitByProjectile()} 为 false（让普通物理投射物穿过去），
 * 这里在 {@code Projectile#canHitEntity} HEAD 对本模组魔法箭放行；主人自己仍排除。</p>
 */
@Mixin(Projectile.class)
public abstract class ProjectileMagicHitMixin {

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void mugewrayce$magicArrowHitsMagicBody(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Projectile self = (Projectile) (Object) this;
        if (!MagicArrows.isMagic(self) || target == self.getOwner()) {
            return;
        }
        if (target instanceof AbstractMagicBodyEntity body) {
            if (body.isAlive() && !body.isSpectator()) {
                cir.setReturnValue(true);
            }
            return;
        }
        if (target instanceof Player player
                && !player.level().isClientSide()
                && player.isAlive()
                && !player.isSpectator()
                && PlayerRoles.isMagicBody(PlayerRoles.getRole(player))) {
            cir.setReturnValue(true);
        }
    }
}
