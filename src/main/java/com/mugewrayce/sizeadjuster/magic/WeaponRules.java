package com.mugewrayce.sizeadjuster.magic;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.item.MagicStaffItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * 附加台可接受物品的判定（W10）。
 *
 * <p>26.1 用 {@link DataComponents#WEAPON} 判断“是武器”，不要用旧的 {@code instanceof SwordItem}；
 * 同时排除本模组的魔法类武器（法杖 / 净化系 / 归零），否则会把魔法杖当成普通武器附加。</p>
 */
public final class WeaponRules {

    private WeaponRules() {
    }

    public static boolean isStaff(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MagicStaffItem;
    }

    /** 是否是可被魔法附加书附加的普通武器（非本模组魔法武器）。 */
    public static boolean isAttachableWeapon(ItemStack stack) {
        if (stack.isEmpty() || isStaff(stack)) {
            return false;
        }
        if (stack.get(DataComponents.WEAPON) == null) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null && SizeAdjusterMod.MOD_ID.equals(id.getNamespace())) {
            String path = id.getPath();
            return !path.startsWith("equipoise_")
                    && !path.equals("zero_reset")
                    && !path.equals("magic_staff");
        }
        return true;
    }
}
