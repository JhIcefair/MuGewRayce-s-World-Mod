package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * 魔法体基类（W08 只做内容层接缝）。
 *
 * <p>共同规则：</p>
 * <ul>
 *   <li>只受魔法伤害；W08 用 {@link MagicBodyCombat#isMagicDamage(DamageSource)} 临时判定，W14 换成
 *       {@code mugewrayce-mod:magic_damage}；</li>
 *   <li>非魔法远程投射物直接穿过：{@link #canBeHitByProjectile()} 返回 {@code false}，
 *       但本模组 {@link EquipoiseOrbEntity} 在 {@code canHitEntity} 里对魔法体单独放行；</li>
 *   <li>强制处决（{@code /kill}、虚空）仍然生效；物理近战与普通远程全部免疫。</li>
 * </ul>
 */
public abstract class AbstractMagicBodyEntity extends Monster {

    protected AbstractMagicBodyEntity(EntityType<? extends AbstractMagicBodyEntity> type, Level level) {
        super(type, level);
    }

    /** 非魔法投射物穿透；魔法球在 {@link EquipoiseOrbEntity} 里单独放行。 */
    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (PlayerRoles.isForcedKill(source)) {
            return false;
        }
        if (!MagicBodyCombat.isMagicDamage(source)) {
            return true;
        }
        return super.isInvulnerableTo(level, source);
    }
}
