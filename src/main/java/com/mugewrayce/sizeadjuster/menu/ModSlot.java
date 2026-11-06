package com.mugewrayce.sizeadjuster.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * 通用槽位（W09）。
 *
 * <p>相比原版 {@link Slot}，这里把“每格最多放几个”和“能放什么”做成可配置项：
 * 26.1 的“只放 1 个”必须由 {@link #getMaxStackSize()} 返回 1 实现，
 * 不要在上层用“判断放入数量”的方式替代（见草案 §14.5）。</p>
 */
public class ModSlot extends Slot {

    private final int maxStackSize;
    private final Predicate<ItemStack> mayPlace;

    public ModSlot(Container container, int slot, int x, int y) {
        this(container, slot, x, y, 64, stack -> true);
    }

    public ModSlot(Container container, int slot, int x, int y, int maxStackSize) {
        this(container, slot, x, y, maxStackSize, stack -> true);
    }

    public ModSlot(Container container, int slot, int x, int y,
                   int maxStackSize, Predicate<ItemStack> mayPlace) {
        super(container, slot, x, y);
        this.maxStackSize = maxStackSize;
        this.mayPlace = mayPlace;
    }

    /** “只放 1 个”的槽位。 */
    public static ModSlot oneItem(Container container, int slot, int x, int y) {
        return new ModSlot(container, slot, x, y, 1);
    }

    /** 只出不进的产物槽（玩家不能放入，只能取走）。 */
    public static ModSlot outputOnly(Container container, int slot, int x, int y) {
        return new ModSlot(container, slot, x, y, 64, stack -> false);
    }

    @Override
    public int getMaxStackSize() {
        return this.maxStackSize;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(this.maxStackSize, super.getMaxStackSize(stack));
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.mayPlace.test(stack) && super.mayPlace(stack);
    }
}
