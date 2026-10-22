package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 纯"魔法体"玩家：不可被投射物命中（远程投射物直接穿过）。
 *
 * 26.1 投射物的实体命中判定统一走
 * ProjectileUtil → target.canBeHitByProjectile()（反编译确认：
 * Projectile.canHitEntity 首先检查目标 canBeHitByProjectile），
 * 因此让纯魔法体玩家的该判定返回 false，即可使弓箭/烈焰弹/雪球等一切
 * 远程投射物从玩家身上"直接穿过去"（服务端判定，客户端随之同步穿行）。
 *
 * 注意：仅对"纯魔法体"（角色编号 1）生效；特殊角色（如 icefair）不在此列——
 * 它们需要被这些投射物命中，以触发自动防御魔法阵（见 icefair 自动防御逻辑）。
 * 本模组自身的魔法球/归零属于"魔法伤害"投射物，仍能命中魔法体玩家
 * （见 AbstractStraightProjectile.canHitEntity 的放开逻辑）。
 */
@Mixin(Entity.class)
public abstract class MagicBodyPlayerProjectileMixin {

    @Inject(method = "canBeHitByProjectile", at = @At("HEAD"), cancellable = true)
    private void mugewrayce$letProjectilesPassThroughMagicBody(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player
                && !self.level().isClientSide()
                && PlayerRoles.getRole(player) == PlayerRoles.ROLE_MAGIC_BODY) {
            cir.setReturnValue(false);
        }
    }
}
