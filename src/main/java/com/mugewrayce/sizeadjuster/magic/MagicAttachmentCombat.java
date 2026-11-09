package com.mugewrayce.sizeadjuster.magic;

import com.mugewrayce.sizeadjuster.component.AttachmentData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.entity.MagicBodyCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 魔法附加书的攻击结算（W14a，草案 §3.25 / §8.5）。
 *
 * <p>只对“会受到魔法伤害的目标”（魔法体）生效：在目标受到物理攻击时，
 * 按武器上 {@code attachment_data.level}（1/2/3）追加一份
 * {@code 实际物理伤害 × 倍率} 的 {@code magic_damage}；物理伤害本身不变。</p>
 *
 * <p>挂点：{@code LivingEntity#hurtServer} HEAD（不能用 {@code ALLOW_DAMAGE}，
 * 因为 26.1 的 {@code hurtServer} 先走 {@code isInvulnerableTo}，魔法体在事件前就被拦下）。
 * 追加的魔法伤害会再次进入 {@code hurtServer}，用 {@link MagicDamage#is(DamageSource)}
 * 防递归。</p>
 */
public final class MagicAttachmentCombat {

    private MagicAttachmentCombat() {
    }

    /**
     * 在目标受到一次物理伤害前调用；只对魔法体、只对主手带附加书倍率的近战攻击生效。
     *
     * @return true = 已追加一份魔法伤害
     */
    public static boolean applyOnHurt(LivingEntity target, DamageSource source, float physicalDamage) {
        if (target == null || source == null || physicalDamage <= 0.0F) {
            return false;
        }
        if (MagicDamage.is(source)) {
            return false; // 追加出来的魔法伤害不再递归追加
        }
        if (target.isDeadOrDying() || !target.isAlive()) {
            return false;
        }
        if (!(source.getDirectEntity() instanceof LivingEntity attacker) || attacker == target) {
            return false;
        }
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!MagicBodyCombat.isMagicBody(target)) {
            return false; // 普通生物 / 普通玩家：完全不触发
        }
        ItemStack weapon = attacker.getMainHandItem();
        int level = weapon.getOrDefault(ModComponents.ATTACHMENT_DATA, AttachmentData.DEFAULT).level();
        float bonus = MagicNumbers.attachmentMagicDamage(physicalDamage, level);
        if (bonus <= 0.0F) {
            return false;
        }
        target.hurt(MagicDamage.of(serverLevel, attacker, attacker), bonus);
        return true;
    }
}
