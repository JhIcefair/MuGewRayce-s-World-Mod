package com.mugewrayce.sizeadjuster.container;

import com.mugewrayce.sizeadjuster.block.MagicAggregationTableBlockEntity;
import com.mugewrayce.sizeadjuster.magic.mana.ManaCost;
import com.mugewrayce.sizeadjuster.menu.AbstractModMenu;
import com.mugewrayce.sizeadjuster.registry.MenuRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;

/**
 * 魔法聚合台菜单（W10）：1 个产物格 + 玩家真实背包 36 格 = 37 格。
 *
 * <p>按钮：
 * id 0 = 「普通」选项键（切换式，服务端存 selected）；
 * id 1 = 「合成」键（开始 5 秒合成；完成扣费在 BlockEntity 里做）。</p>
 *
 * <p>简单状态走 {@link ContainerData}：0=selected、1=crafting、2=remainingSeconds、3=hintCode。
 * 服务端 {@code get} 读取实时状态，客户端读取同步数组。</p>
 */
public class MagicAggregationTableMenu extends AbstractModMenu {

    public static final int SIZE = 1;

    public static final int BUTTON_TOGGLE = 0;
    public static final int BUTTON_CRAFT = 1;

    public static final int HINT_NONE = 0;
    public static final int HINT_SELECT_CRYSTAL = 1;
    public static final int HINT_CRAFTING = 2;
    public static final int HINT_NO_MANA = MagicAggregationTableBlockEntity.HINT_NO_MANA;
    public static final int HINT_OUTPUT_BLOCKED = MagicAggregationTableBlockEntity.HINT_OUTPUT_BLOCKED;

    public static final int SLOT_OUTPUT = 0;

    public static final MenuType<MagicAggregationTableMenu> TYPE =
            MenuRegistry.registerMenu("magic_aggregation_table", MagicAggregationTableMenu::new);

    private final MagicAggregationTableBlockEntity table;
    private final AggregationData data = new AggregationData();

    private boolean selected;
    private int hintCode = HINT_NONE;

    /** 客户端：MenuType 工厂用。 */
    public MagicAggregationTableMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(SIZE), null, null);
    }

    /** 服务端：绑定真实 BlockEntity 容器 + 方块访问。 */
    public MagicAggregationTableMenu(int id, Inventory playerInventory, Container container,
                                     ContainerLevelAccess access, Block block) {
        super(TYPE, id, playerInventory, container, SIZE, access, block);
        this.table = container instanceof MagicAggregationTableBlockEntity aggregation ? aggregation : null;

        this.addOutputSlot(container, SLOT_OUTPUT, 111, 75);
        this.addPlayerInventory(39, 166);
        this.addModDataSlots(this.data);
    }

    // ------------------------------------------------------------------
    // 按钮
    // ------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide()) {
            return false;
        }
        return switch (id) {
            case BUTTON_TOGGLE -> this.toggleOption();
            case BUTTON_CRAFT -> this.tryStartCraft(player);
            default -> false;
        };
    }

    private boolean toggleOption() {
        this.selected = !this.selected;
        this.clearHint();
        return true;
    }

    private boolean tryStartCraft(Player player) {
        this.clearHint();
        if (!this.selected) {
            this.hintCode = HINT_SELECT_CRYSTAL;
            return true;
        }
        if (this.table == null) {
            return false;
        }
        if (this.table.isCrafting()) {
            this.hintCode = HINT_CRAFTING;
            return true;
        }
        if (!this.table.canAcceptCrystal()) {
            this.hintCode = HINT_OUTPUT_BLOCKED;
            return true;
        }
        if (!(player.level() instanceof ServerLevel server)
                || !ManaCost.canPayAggregation(server, this.table.getBlockPos())) {
            this.hintCode = HINT_NO_MANA;
            return true;
        }

        // 只开始计时，不在开始时扣费；完成时由 BlockEntity 调 payAggregation。
        this.table.startCraft();
        this.hintCode = HINT_NONE;
        return true;
    }

    // ------------------------------------------------------------------
    // 状态读取（服务端实时 / 客户端同步数组）
    // ------------------------------------------------------------------

    public boolean isSelected() {
        return this.data.get(0) != 0;
    }

    public boolean isCrafting() {
        return this.data.get(1) != 0;
    }

    public int getRemainingSeconds() {
        return this.data.get(2);
    }

    public int getHintCode() {
        return this.data.get(3);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        // 只清菜单级提示；BlockEntity 的完成失败提示由 data 的实时读取提供，
        // 不能被 setChanged() 触发的 slotsChanged 顺手清掉。
        this.hintCode = HINT_NONE;
    }

    private void clearHint() {
        this.hintCode = HINT_NONE;
        if (this.table != null) {
            this.table.clearHint();
        }
    }

    private int liveHintCode() {
        if (this.hintCode != HINT_NONE) {
            return this.hintCode;
        }
        return this.table == null ? HINT_NONE : this.table.getHintCode();
    }

    /**
     * 4 项同步数据。服务端读取菜单 / BlockEntity 的实时值；客户端读取同步数组。
     */
    private final class AggregationData implements ContainerData {

        private final int[] synced = new int[4];

        @Override
        public int get(int index) {
            if (MagicAggregationTableMenu.this.playerInventory.player.level().isClientSide()) {
                return index >= 0 && index < this.synced.length ? this.synced[index] : 0;
            }
            return switch (index) {
                case 0 -> MagicAggregationTableMenu.this.selected ? 1 : 0;
                case 1 -> MagicAggregationTableMenu.this.table != null
                        && MagicAggregationTableMenu.this.table.isCrafting() ? 1 : 0;
                case 2 -> MagicAggregationTableMenu.this.table == null
                        ? 0 : MagicAggregationTableMenu.this.table.remainingSeconds();
                case 3 -> MagicAggregationTableMenu.this.liveHintCode();
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
            return 4;
        }
    }
}
