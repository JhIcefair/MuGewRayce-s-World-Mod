package com.mugewrayce.sizeadjuster.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Predicate;

/**
 * 通用容器菜单基类（W09）。
 *
 * <p>本类只属于 common 侧，<b>不得 import 任何 client / Minecraft / Screen 类</b>。
 * 它统一解决三件最容易做错的事：</p>
 * <ol>
 *   <li>玩家背包 36 格必须绑定真实的 {@link Inventory}：物品栏 9×3（index 9..35）+
 *       快捷栏 9×1（index 0..8）；</li>
 *   <li>固定槽位区间：容器 0..containerSize-1，玩家物品栏 containerSize..containerSize+26，
 *       快捷栏 containerSize+27..containerSize+35；</li>
 *   <li>提供公共 {@code quickMoveStack} 骨架，子类只需声明容器槽位数即可。</li>
 * </ol>
 *
 * <p>{@code stillValid} 同时覆盖方块容器（BlockEntity / ContainerLevelAccess）与物品容器。</p>
 */
public abstract class AbstractModMenu extends AbstractContainerMenu {

    /** 玩家物品栏 9×3 的行数。 */
    public static final int PLAYER_INVENTORY_ROWS = 3;
    /** 每行 9 格。 */
    public static final int SLOTS_PER_ROW = 9;
    /** 物品栏 27 格。 */
    public static final int PLAYER_INVENTORY_SIZE = PLAYER_INVENTORY_ROWS * SLOTS_PER_ROW;
    /** 快捷栏 9 格。 */
    public static final int HOTBAR_SIZE = 9;
    /** 玩家侧总格数 36。 */
    public static final int PLAYER_SLOTS = PLAYER_INVENTORY_SIZE + HOTBAR_SIZE;
    /** 26.1 原版布局中快捷栏相对物品栏首行的 y 偏移（140 → 198）。 */
    public static final int HOTBAR_Y_OFFSET = 58;

    protected final Inventory playerInventory;
    protected final Container container;

    private final int containerSlotCount;
    private final ContainerLevelAccess blockAccess;
    private final Block block;

    private int playerInventoryStart = -1;
    private int hotbarStart = -1;

    protected AbstractModMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                              Container container, int containerSlotCount) {
        this(type, containerId, playerInventory, container, containerSlotCount, null, null);
    }

    protected AbstractModMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                              Container container, int containerSlotCount,
                              ContainerLevelAccess blockAccess, Block block) {
        super(type, containerId);
        this.playerInventory = playerInventory;
        this.container = container;
        this.containerSlotCount = containerSlotCount;
        this.blockAccess = blockAccess;
        this.block = block;
    }

    // ---------------------------------------------------------------------
    // 槽位辅助
    // ---------------------------------------------------------------------

    /** 添加一个自定义槽位。 */
    protected Slot addModSlot(Slot slot) {
        return this.addSlot(slot);
    }

    /** 添加“只放 1 个”的槽位（{@link Slot#getMaxStackSize()} 返回 1）。 */
    protected Slot addOneItemSlot(Container container, int slot, int x, int y) {
        return this.addSlot(ModSlot.oneItem(container, slot, x, y));
    }

    /** 添加带类型限制、且最多放 1 个的槽位。 */
    protected Slot addOneItemSlot(Container container, int slot, int x, int y,
                                  Predicate<ItemStack> mayPlace) {
        return this.addSlot(new ModSlot(container, slot, x, y, 1, mayPlace));
    }

    /** 添加只出不进的产物槽。 */
    protected Slot addOutputSlot(Container container, int slot, int x, int y) {
        return this.addSlot(ModSlot.outputOnly(container, slot, x, y));
    }

    /**
     * 添加玩家真实背包 36 格。
     *
     * <p>物品栏 27 格使用 {@code 9 + row * 9 + col}（主背包 index 9..35），
     * 快捷栏 9 格使用 index 0..8；两者都直接绑定 {@code player.getInventory()}，
     * 不复制、不另造容器。</p>
     *
     * @param x 物品栏首列 x
     * @param y 物品栏首行 y
     */
    protected void addPlayerInventory(int x, int y) {
        this.addPlayerInventory(x, y, HOTBAR_Y_OFFSET);
    }

    /**
     * 添加玩家真实背包 36 格，并显式指定快捷栏相对物品栏的 y 偏移。
     */
    protected void addPlayerInventory(int x, int y, int hotbarYOffset) {
        if (this.playerInventoryStart >= 0) {
            throw new IllegalStateException("Player inventory already added");
        }
        this.playerInventoryStart = this.slots.size();

        for (int row = 0; row < PLAYER_INVENTORY_ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                this.addSlot(new Slot(this.playerInventory,
                        9 + row * SLOTS_PER_ROW + col,
                        x + col * 18,
                        y + row * 18));
            }
        }

        this.hotbarStart = this.slots.size();
        for (int col = 0; col < HOTBAR_SIZE; col++) {
            this.addSlot(new Slot(this.playerInventory, col, x + col * 18, y + hotbarYOffset));
        }
    }

    /** 可选：给客户端同步简单标量（复杂数据请用自定义包）。 */
    protected void addModDataSlots(ContainerData data) {
        this.addDataSlots(data);
    }

    // ---------------------------------------------------------------------
    // 槽位区间
    // ---------------------------------------------------------------------

    public int getContainerSlotCount() {
        return this.containerSlotCount;
    }

    public int getPlayerInventoryStart() {
        return this.playerInventoryStart;
    }

    public int getHotbarStart() {
        return this.hotbarStart;
    }

    /** 该槽位是否属于玩家侧（物品栏或快捷栏）。 */
    public boolean isPlayerSlot(int slotIndex) {
        return this.playerInventoryStart >= 0
                && slotIndex >= this.playerInventoryStart
                && slotIndex < this.slots.size();
    }

    // ---------------------------------------------------------------------
    // 快速移动
    // ---------------------------------------------------------------------

    /**
     * 公共快速移动骨架：
     * <ul>
     *   <li>容器 → 玩家背包（优先快捷栏，原版 reverse 语义）；</li>
     *   <li>玩家背包 → 容器；</li>
     *   <li>物品栏 ⇄ 快捷栏。</li>
     * </ul>
     *
     * <p>子类若有特殊槽位（产物格、只出不进格等），覆写
     * {@link #quickMoveFromContainer(ItemStack, int)} /
     * {@link #quickMoveToContainer(ItemStack, int)} 即可。</p>
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        this.requirePlayerInventory();

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;

        if (index < this.containerSlotCount) {
            moved = this.quickMoveFromContainer(stack, index);
        } else {
            moved = this.quickMoveToContainer(stack, index);
            if (!moved) {
                if (index < this.hotbarStart) {
                    // 物品栏 → 快捷栏
                    moved = this.moveItemStackTo(stack, this.hotbarStart, this.slots.size(), false);
                } else {
                    // 快捷栏 → 物品栏
                    moved = this.moveItemStackTo(stack, this.playerInventoryStart, this.hotbarStart, false);
                }
            }
        }

        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** 容器槽 → 玩家背包；默认优先快捷栏。 */
    protected boolean quickMoveFromContainer(ItemStack stack, int index) {
        return this.moveItemStackTo(stack, this.playerInventoryStart, this.slots.size(), true);
    }

    /** 玩家背包 → 容器槽；默认按容器槽区间正序。 */
    protected boolean quickMoveToContainer(ItemStack stack, int index) {
        return this.moveItemStackTo(stack, 0, this.containerSlotCount, false);
    }

    private void requirePlayerInventory() {
        if (this.playerInventoryStart < 0) {
            throw new IllegalStateException("addPlayerInventory(...) was not called");
        }
    }

    // ---------------------------------------------------------------------
    // 有效性
    // ---------------------------------------------------------------------

    /**
     * 方块容器：优先使用 {@link ContainerLevelAccess}（方块仍在、玩家未走远）；
     * 其次是 BlockEntity 自身的 stillValid；物品容器则要求仍是打开它的玩家。
     */
    @Override
    public boolean stillValid(Player player) {
        if (this.blockAccess != null && this.block != null) {
            return stillValid(this.blockAccess, player, this.block);
        }
        if (this.container instanceof BlockEntity blockEntity) {
            return Container.stillValidBlockEntity(blockEntity, player);
        }
        return player == this.playerInventory.player && !player.isRemoved();
    }
}
