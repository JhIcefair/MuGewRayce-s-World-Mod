package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.magic.MagicDamage;
import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 魔法体战斗接缝（W08 建，W14 改用正式伤害类型）。
 *
 * <p>只受 {@code mugewrayce-mod:magic_damage}；原版 {@code magic} / {@code indirect_magic}
 * 不再算本模组魔法伤害（草案 §8.1 独立体系）。</p>
 */
public final class MagicBodyCombat {

    private MagicBodyCombat() {
    }

    /** 是否属于魔法体可承受的“魔法伤害”（W14 起严格按 {@code magic_damage} 类型）。 */
    public static boolean isMagicDamage(DamageSource source) {
        return MagicDamage.is(source);
    }

    /**
     * 目标是否“魔法体”：没有生命的魔法体实体、有生命的魔法体角色、icefair 等特殊角色。
     *
     * <p>附加书只对魔法体追加魔法伤害；普通生物 / 玩家完全不触发。</p>
     */
    public static boolean isMagicBody(Entity target) {
        if (target instanceof AbstractMagicBodyEntity) {
            return true;
        }
        return target instanceof Player player
                && PlayerRoles.isMagicBody(PlayerRoles.getRole(player));
    }
}
