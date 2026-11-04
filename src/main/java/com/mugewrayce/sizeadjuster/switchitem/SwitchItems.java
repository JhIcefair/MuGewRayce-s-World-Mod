package com.mugewrayce.sizeadjuster.switchitem;

import com.mugewrayce.sizeadjuster.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 净化系五件物品与方向键切换的映射。
 *
 * <p>顺序与 {@link EquipoiseSwitchMatrix} 的物品码一致：
 * 净化水晶 → 净化法杖 → 净化圣剑 → 净化圣弓 → 归零。</p>
 *
 * <p>本类只做“物品 ↔ 物品码”的映射；5×4 矩阵与拒绝规则在
 * {@link EquipoiseSwitchMatrix#target(int, int)}，服务端按当前主手重算。</p>
 */
public final class SwitchItems {

    /** 五件可切换物品，下标即 {@link EquipoiseSwitchMatrix} 的物品码。 */
    public static final List<Item> ORDER = List.of(
            ModItems.equipoiseCrystal,
            ModItems.equipoiseStaff,
            ModItems.equipoiseSword,
            ModItems.equipoiseBow,
            ModItems.zeroReset);

    private SwitchItems() {
    }

    public static boolean isSwitchable(ItemStack stack) {
        return !stack.isEmpty() && indexOf(stack.getItem()) >= 0;
    }

    public static int indexOf(Item item) {
        return ORDER.indexOf(item);
    }

    public static Item itemOf(int itemCode) {
        return EquipoiseSwitchMatrix.isValidItem(itemCode) ? ORDER.get(itemCode) : null;
    }

    /** 按当前主手与方向计算目标物品；非法主手 / 方向 / 目标等于当前物品时返回 {@code null}。 */
    public static Item targetFor(Item held, int direction) {
        int heldCode = indexOf(held);
        if (heldCode < 0) {
            return null;
        }
        return itemOf(EquipoiseSwitchMatrix.target(heldCode, direction));
    }

    public static Item targetFor(ItemStack held, int direction) {
        return isSwitchable(held) ? targetFor(held.getItem(), direction) : null;
    }
}
