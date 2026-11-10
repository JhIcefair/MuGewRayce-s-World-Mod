package com.mugewrayce.sizeadjuster.magic;

import net.minecraft.world.entity.Entity;

/**
 * W18：净化圣弓发出的魔法箭标记（实体 tag，不是数据组件、也不新增实体类型）。
 *
 * <p>箭的创建在 {@code EquipoiseBowItem#createProjectile} 里打标记；
 * 命中时由 {@code MagicWeaponDamageMixin} 据此换成 {@code magic_damage}，
 * 由 {@code ProjectileMagicHitMixin} 放行“没有生命的魔法体”。</p>
 */
public final class MagicArrows {

    /** 实体 tag：本模组魔法箭。 */
    public static final String TAG = "mugewrayce_magic_arrow";

    private MagicArrows() {
    }

    public static void mark(Entity entity) {
        if (entity != null) {
            entity.addTag(TAG);
        }
    }

    public static boolean isMagic(Entity entity) {
        return entity != null && entity.entityTags().contains(TAG);
    }
}
