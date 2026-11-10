package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.item.ModItems;
import com.mugewrayce.sizeadjuster.magic.MagicArrows;
import com.mugewrayce.sizeadjuster.magic.MagicDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * W18：净化圣剑 / 圣弓的伤害改用 {@code magic_damage} 类型。
 *
 * <p>在 {@code LivingEntity#hurtServer} 的 HEAD 替换 {@code DamageSource} 参数：
 * 数值、击退、附魔结算全部保持原样，只换伤害类型（无视护甲、死亡消息走魔法）。
 * 早于 {@code isInvulnerableTo}，因此对“没有生命的魔法体”也能生效。</p>
 */
@Mixin(LivingEntity.class)
public abstract class MagicWeaponDamageMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, index = 2)
    private static DamageSource mugewrayce$replaceWithMagicDamage(DamageSource source, ServerLevel level,
                                                                  DamageSource original, float amount) {
        if (source == null) {
            return null;
        }
        Entity direct = source.getDirectEntity();
        Entity causing = source.getEntity();
        // 圣剑近战：直接来源与归属都是手持圣剑的玩家
        if (direct instanceof Player player && causing == player
                && player.getMainHandItem().is(ModItems.equipoiseSword)) {
            return MagicDamage.of(level, player, player);
        }
        // 圣弓：带魔法标记的箭
        if (MagicArrows.isMagic(direct)) {
            return MagicDamage.of(level, direct, causing);
        }
        return source;
    }
}
