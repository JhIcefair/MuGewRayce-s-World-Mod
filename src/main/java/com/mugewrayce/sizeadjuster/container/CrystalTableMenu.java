package com.mugewrayce.sizeadjuster.container;

import com.mugewrayce.sizeadjuster.block.CrystalTableBlockEntity;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.menu.AbstractModMenu;
import com.mugewrayce.sizeadjuster.menu.CrystalTableSlot;
import com.mugewrayce.sizeadjuster.registry.BlockRegistry;
import com.mugewrayce.sizeadjuster.registry.MenuRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 水晶拆合台菜单（W20）：9 个容器格（0=中心，1~8=环）+ 玩家真实背包 36 格。
 *
 * <p>两个按钮（{@link #clickMenuButton}）：
 * id 0 = 拆分、id 1 = 合成；选中状态由服务端 {@link CrystalTableBlockEntity#getMode()} 决定，
 * 通过 {@link ContainerData} 索引 0 回读，客户端只负责显示。</p>
 *
 * <p>交易规则（草案由 W20 交接单 §2.5.4 定稿）：</p>
 * <ul>
 *   <li>拆分：中心扣 1 个水晶块 → 一次给出 <b>8 个水晶</b>；</li>
 *   <li>合成：环上 8 格各扣 1 个<b>同种</b>水晶 → 一次给出 <b>1 个水晶块</b>；</li>
 *   <li>预览格只在取出时结算，拿不下（光标放不下）时不扣料并给提示。</li>
 * </ul>
 *
 * <p>提示码走 {@link ContainerData} 索引 1；{@link #HINT_NONE} 表示无提示。</p>
 */
public class CrystalTableMenu extends AbstractModMenu {

    public static final int SIZE = CrystalTableBlockEntity.SIZE;
    public static final int SLOT_CENTER = CrystalTableBlockEntity.SLOT_CENTER;

    public static final int BUTTON_SPLIT = 0;
    public static final int BUTTON_CRAFT = 1;

    public static final int HINT_NONE = 0;
    public static final int HINT_NEED_BLOCK = 1;
    public static final int HINT_NEED_CRYSTALS = 2;
    public static final int HINT_NOT_SAME = 3;
    public static final int HINT_OUTPUT_BLOCKED = 4;
    public static final int HINT_INPUT_FULL = 5;

    /** 拆分一次给多少个水晶。 */
    public static final int SPLIT_CRYSTALS = 8;

    /** 中心格坐标（按 ls\46\46.png 实测）。 */
    private static final int CENTER_X = 111;
    private static final int CENTER_Y = 75;

    /** 环 8 格坐标：N, NE, E, SE, S, SW, W, NW。 */
    private static final int[] RING_X = {111, 142, 155, 142, 111, 80, 67, 80};
    private static final int[] RING_Y = {31, 44, 75, 106, 119, 106, 75, 44};

    public static final MenuType<CrystalTableMenu> TYPE =
            MenuRegistry.registerMenu("crystal_table", CrystalTableMenu::new);

    private final CrystalTableBlockEntity table;
    private final CrystalData data = new CrystalData();

    private int hintCode = HINT_NONE;

    /** 客户端：MenuType 工厂用。 */
    public CrystalTableMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(SIZE), null, null);
    }

    /** 服务端：绑定真实 BlockEntity 容器 + 方块访问。 */
    public CrystalTableMenu(int id, Inventory playerInventory, Container container,
                            ContainerLevelAccess access, Block block) {
        super(TYPE, id, playerInventory, container, SIZE, access, block);
        this.table = container instanceof CrystalTableBlockEntity crystalTable ? crystalTable : null;

        this.addSlot(new CrystalTableSlot(this, container, SLOT_CENTER,
                CENTER_X, CENTER_Y, CrystalTableSlot.Role.CENTER));
        for (int i = 0; i < RING_X.length; i++) {
            this.addSlot(new CrystalTableSlot(this, container, i + 1,
                    RING_X[i], RING_Y[i], CrystalTableSlot.Role.RING));
        }

        this.addPlayerInventory(39, 166);
        this.addModDataSlots(this.data);
    }

    // ------------------------------------------------------------------
    // 模式
    // ------------------------------------------------------------------

    /** 当前模式（服务端取 BlockEntity，客户端取同步数据）。 */
    public int getMode() {
        return this.data.get(0);
    }

    public boolean isSplit() {
        return this.getMode() == CrystalTableBlockEntity.MODE_SPLIT;
    }

    /** 该角色在当前模式下是否是输出预览。 */
    public boolean isGhostRole(CrystalTableSlot.Role role) {
        boolean center = role == CrystalTableSlot.Role.CENTER;
        return this.isSplit() ? !center : center;
    }

    public int getHintCode() {
        return this.data.get(1);
    }

    public void setHint(int hintCode) {
        this.hintCode = hintCode;
    }

    private void clearHint() {
        this.hintCode = HINT_NONE;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide() || this.table == null) {
            return false;
        }
        return switch (id) {
            case BUTTON_SPLIT -> this.switchMode(CrystalTableBlockEntity.MODE_SPLIT);
            case BUTTON_CRAFT -> this.switchMode(CrystalTableBlockEntity.MODE_CRAFT);
            default -> false;
        };
    }

    /**
     * 切换模式。
     *
     * <p>切换后“当前真实输入侧”会变成预览并不可见，所以只要那一侧还有东西就拒绝切换，
     * 提示玩家先取走——避免物品被藏起来（交接单 §2.5.4-5）。</p>
     */
    private boolean switchMode(int target) {
        this.clearHint();
        if (this.table.getMode() == target) {
            return true;
        }
        if (!this.isInputSideEmpty(this.table.getMode())) {
            this.hintCode = HINT_INPUT_FULL;
            return true;
        }
        this.table.setMode(target);
        return true;
    }

    /** 当前模式下真实输入侧是否为空：拆分看中心，合成看环。 */
    private boolean isInputSideEmpty(int mode) {
        if (mode == CrystalTableBlockEntity.MODE_SPLIT) {
            return this.container.getItem(SLOT_CENTER).isEmpty();
        }
        for (int i = 1; i < SIZE; i++) {
            if (!this.container.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 槽位查询 / 结算（由 CrystalTableSlot 调用）
    // ------------------------------------------------------------------

    public boolean isCrystalBlock(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(BlockRegistry.CRYSTAL_BLOCK_ITEM);
    }

    public boolean isCrystal(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(MagicItems.crystal);
    }

    /** 预览格显示什么（条件不满足返回空）。 */
    public ItemStack previewFor(CrystalTableSlot.Role role) {
        if (role == CrystalTableSlot.Role.CENTER) {
            return this.isSplit() ? ItemStack.EMPTY : this.craftPreview();
        }
        return this.isSplit() ? this.splitPreview() : ItemStack.EMPTY;
    }

    /** 拆分预览：中心有水晶块时，环上每格显示 1 个水晶。 */
    private ItemStack splitPreview() {
        return this.isCrystalBlock(this.container.getItem(SLOT_CENTER))
                ? new ItemStack(MagicItems.crystal) : ItemStack.EMPTY;
    }

    /** 合成预览：环上 8 格都有同种水晶时，中心显示 1 个水晶块。 */
    private ItemStack craftPreview() {
        return this.craftInputsValid() ? new ItemStack(BlockRegistry.CRYSTAL_BLOCK_ITEM) : ItemStack.EMPTY;
    }

    /** 结算产物（不执行交易）：拆分 8 个水晶，合成 1 个水晶块。 */
    public ItemStack productFor(CrystalTableSlot.Role role) {
        if (role == CrystalTableSlot.Role.CENTER) {
            return this.isSplit() ? ItemStack.EMPTY : this.craftPreview();
        }
        if (!this.isSplit()) {
            return ItemStack.EMPTY;
        }
        ItemStack preview = this.splitPreview();
        return preview.isEmpty() ? ItemStack.EMPTY : preview.copyWithCount(SPLIT_CRYSTALS);
    }

    /**
     * 执行一次交易并返回产物；条件不满足返回空并记录提示。
     *
     * <p>只在 {@code CrystalTableSlot#remove} 里调用（即玩家真的取走时）。</p>
     */
    public ItemStack takeProduct(CrystalTableSlot.Role role) {
        if (role == CrystalTableSlot.Role.CENTER) {
            return this.isSplit() ? ItemStack.EMPTY : this.takeCraft();
        }
        return this.isSplit() ? this.takeSplit() : ItemStack.EMPTY;
    }

    private ItemStack takeSplit() {
        ItemStack input = this.container.getItem(SLOT_CENTER);
        if (!this.isCrystalBlock(input)) {
            this.hintCode = HINT_NEED_BLOCK;
            return ItemStack.EMPTY;
        }
        this.container.removeItem(SLOT_CENTER, 1);
        this.container.setChanged();
        this.clearHint();
        return new ItemStack(MagicItems.crystal, SPLIT_CRYSTALS);
    }

    private ItemStack takeCraft() {
        if (!this.craftInputsValid()) {
            this.hintCode = this.craftHint();
            return ItemStack.EMPTY;
        }
        for (int i = 1; i < SIZE; i++) {
            this.container.removeItem(i, 1);
        }
        this.container.setChanged();
        this.clearHint();
        return new ItemStack(BlockRegistry.CRYSTAL_BLOCK_ITEM, 1);
    }

    /** 环上 8 格都非空、且是同种水晶。 */
    private boolean craftInputsValid() {
        ItemStack first = this.container.getItem(1);
        if (!this.isCrystal(first)) {
            return false;
        }
        for (int i = 2; i < SIZE; i++) {
            ItemStack stack = this.container.getItem(i);
            if (!this.isCrystal(stack) || !ItemStack.isSameItemSameComponents(first, stack)) {
                return false;
            }
        }
        return true;
    }

    private int craftHint() {
        ItemStack first = this.container.getItem(1);
        if (!this.isCrystal(first)) {
            return HINT_NEED_CRYSTALS;
        }
        for (int i = 2; i < SIZE; i++) {
            ItemStack stack = this.container.getItem(i);
            if (!this.isCrystal(stack)) {
                return HINT_NEED_CRYSTALS;
            }
            if (!ItemStack.isSameItemSameComponents(first, stack)) {
                return HINT_NOT_SAME;
            }
        }
        return HINT_NEED_CRYSTALS;
    }

    /** 取产物时光标是否放得下（空光标 / 同物品且不超上限）。 */
    public boolean canCarry(Player player, ItemStack product) {
        if (product == null || product.isEmpty()) {
            return false;
        }
        ItemStack carried = this.getCarried();
        if (carried.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(carried, product)) {
            this.hintCode = HINT_OUTPUT_BLOCKED;
            return false;
        }
        if (carried.getCount() + product.getCount() > carried.getMaxStackSize()) {
            this.hintCode = HINT_OUTPUT_BLOCKED;
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 快捷移动：预览格一律不参与
    // ------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= 0 && index < this.slots.size()) {
            if (this.slots.get(index) instanceof CrystalTableSlot slot && slot.isGhost()) {
                return ItemStack.EMPTY;
            }
        }
        return super.quickMoveStack(player, index);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        // 输入变化后旧提示已经不对了；预览格不触发这里（setChanged 被拦）。
        this.hintCode = HINT_NONE;
    }

    // ------------------------------------------------------------------
    // 同步数据：0=mode，1=hintCode
    // ------------------------------------------------------------------

    private final class CrystalData implements ContainerData {

        private final int[] synced = new int[2];

        @Override
        public int get(int index) {
            if (CrystalTableMenu.this.playerInventory.player.level().isClientSide()) {
                return index >= 0 && index < this.synced.length ? this.synced[index] : 0;
            }
            return switch (index) {
                case 0 -> CrystalTableMenu.this.table == null
                        ? CrystalTableBlockEntity.MODE_SPLIT : CrystalTableMenu.this.table.getMode();
                case 1 -> CrystalTableMenu.this.hintCode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < this.synced.length) {
                this.synced[index] = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
