package com.mugewrayce.sizeadjuster.menu;

import com.mugewrayce.sizeadjuster.container.CrystalTableMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 水晶拆合台的槽位（W20）。
 *
 * <p>9 个槽按模式动态分成两侧：</p>
 * <ul>
 *   <li><b>拆分</b>：中心 = 真实输入（水晶块），环 = 输出预览（各 1 个水晶）；</li>
 *   <li><b>合成</b>：环 = 真实输入（各 ≥1 个同种水晶），中心 = 输出预览（1 个水晶块）。</li>
 * </ul>
 *
 * <p>预览格（{@link #isGhost()}）不写容器：{@code getItem()} 每次按真实输入现算，
 * {@code remove()} 才真正结算（一次取走 = 一次交易）。真实输入格照常读写 BlockEntity，
 * 因此物品不会因为切换模式被复制或藏匿。</p>
 */
public class CrystalTableSlot extends Slot {

    /** 槽位角色：中心 / 环。 */
    public enum Role {
        CENTER,
        RING
    }

    private final CrystalTableMenu menu;
    private final Role role;

    public CrystalTableSlot(CrystalTableMenu menu, Container container, int slot, int x, int y, Role role) {
        super(container, slot, x, y);
        this.menu = menu;
        this.role = role;
    }

    public Role getRole() {
        return this.role;
    }

    /** 当前模式下本格是否是输出预览（不落盘、不能放入、取出即结算）。 */
    public boolean isGhost() {
        return this.menu.isGhostRole(this.role);
    }

    // ------------------------------------------------------------------
    // 放入 / 显示
    // ------------------------------------------------------------------

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (this.isGhost()) {
            return false;
        }
        return this.role == Role.CENTER
                ? this.menu.isCrystalBlock(stack)
                : this.menu.isCrystal(stack);
    }

    @Override
    public ItemStack getItem() {
        if (this.isGhost()) {
            return this.menu.previewFor(this.role);
        }
        return super.getItem();
    }

    @Override
    public int getMaxStackSize() {
        return this.isGhost() ? 64 : super.getMaxStackSize();
    }

    // ------------------------------------------------------------------
    // 取出：预览格在此结算
    // ------------------------------------------------------------------

    @Override
    public boolean mayPickup(Player player) {
        if (!this.isGhost()) {
            return super.mayPickup(player);
        }
        ItemStack product = this.menu.productFor(this.role);
        if (product.isEmpty()) {
            return false;
        }
        return this.menu.canCarry(player, product);
    }

    @Override
    public ItemStack remove(int amount) {
        if (!this.isGhost()) {
            return super.remove(amount);
        }
        return this.menu.takeProduct(this.role);
    }

    // ------------------------------------------------------------------
    // 预览格不写容器
    // ------------------------------------------------------------------

    @Override
    public void set(ItemStack stack) {
        if (!this.isGhost()) {
            super.set(stack);
        }
    }

    @Override
    public void setByPlayer(ItemStack stack) {
        if (!this.isGhost()) {
            super.setByPlayer(stack);
        }
    }

    @Override
    public void setByPlayer(ItemStack stack, ItemStack previous) {
        if (!this.isGhost()) {
            super.setByPlayer(stack, previous);
        }
    }

    @Override
    public void setChanged() {
        if (!this.isGhost()) {
            super.setChanged();
        }
    }
}
