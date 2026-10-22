package com.mugewrayce.sizeadjuster.switchitem;

import com.mugewrayce.sizeadjuster.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 净化物品切换联动。
 * 手持列表中任意物品时按切换键（默认 I）可切换为其他净化物品。
 * 顺序：净化水晶 → 净化法杖 → 净化圣剑 → 净化长枪 → 净化圣弓 → 归零（循环）。
 */
public final class SwitchItems {

    public static final List<Item> ORDER = List.of(
            ModItems.purificationCrystal,
            ModItems.purificationStaff,
            ModItems.purificationHolySword,
            ModItems.purificationLongSpear,
            ModItems.purificationHolyBow,
            ModItems.zeroReset);

    private SwitchItems() {
    }

    public static boolean isSwitchable(ItemStack stack) {
        return !stack.isEmpty() && ORDER.contains(stack.getItem());
    }

    public static Item getItem(int index) {
        return ORDER.get(Math.floorMod(index, ORDER.size()));
    }

    public static int indexOf(Item item) {
        return ORDER.indexOf(item);
    }
}
