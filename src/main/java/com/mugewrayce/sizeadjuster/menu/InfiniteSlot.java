package com.mugewrayce.sizeadjuster.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * 无限取物格（W11 讲台 ③④）。
 *
 * <p>格子里<b>永远</b>有一个常驻物品（魔法附加台 / 魔法聚合台方块物品），
 * 玩家点一次只拿 1 个、拿走后格子里仍然有；不能放入任何东西。</p>
 *
 * <p>关键点（交接单 §1.12）：<b>不能用默认 Slot</b>——默认 Slot 会把常驻物品经
 * {@code container.setItem/removeItem} 真正拿走或写进 BlockEntity。这里把
 * 取/放/同步全部改为不碰容器；真正的给物品逻辑在
 * {@code MagicStudyLecternMenu#clicked} 与 {@code quickMoveStack} 里。</p>
 */
public class InfiniteSlot extends Slot {

    private final Supplier<ItemStack> resident;

    public InfiniteSlot(Container container, int slot, int x, int y, Supplier<ItemStack> resident) {
        super(container, slot, x, y);
        this.resident = resident;
    }

    /** 常驻物品（每次给新副本，避免被调用方改到共享实例）。 */
    public ItemStack residentStack() {
        return this.resident.get();
    }

    @Override
    public ItemStack getItem() {
        return this.residentStack();
    }

    @Override
    public boolean hasItem() {
        return true;
    }

    /** 每格最多 1 个（§1.3）。 */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    /** 永远不能放入。 */
    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    /** 取物不走原版路径（由菜单 clicked / quickMoveStack 处理）。 */
    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        // 按交接单建议：返回 1 个副本且不改容器（正常路径不会走到这里）。
        return amount <= 0 ? ItemStack.EMPTY : this.residentStack();
    }

    @Override
    public void set(ItemStack stack) {
        // 不改变常驻状态。
    }

    @Override
    public void setByPlayer(ItemStack stack) {
        // 不改变常驻状态。
    }

    @Override
    public void setByPlayer(ItemStack stack, ItemStack previous) {
        // 不改变常驻状态。
    }

    @Override
    public void setChanged() {
        // 不写 BlockEntity。
    }
}
