package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.minecraft.world.damagesource.DamageSource;

/**
 * 魔法体战斗接缝（W08 只提供伤害分类）。
 *
 * <p>W08 不注册 {@code mugewrayce-mod:magic_damage}：先复用
 * {@link PlayerRoles#isMagicDamage(DamageSource)}（本模组魔法球 / 魔法阵 + 原版 magic / indirect_magic）。
 * W14 注册正式伤害类型后，把这里换成对 {@code magic_damage} 的判定。</p>
 */
public final class MagicBodyCombat {

    private MagicBodyCombat() {
    }

    /** 是否属于魔法体可承受的“魔法伤害”（W14 前临时口径）。 */
    public static boolean isMagicDamage(DamageSource source) {
        return PlayerRoles.isMagicDamage(source);
    }
}
