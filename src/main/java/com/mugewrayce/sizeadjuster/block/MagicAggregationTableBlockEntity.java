package com.mugewrayce.sizeadjuster.block;

import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.magic.mana.ManaCost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 魔法聚合台 BlockEntity：1 格产物 + 合成计时（W10）。
 *
 * <p>计时必须放在 BlockEntity，而不是菜单：玩家关闭 GUI / 死亡后合成要继续；
 * 方块被破坏时 BE 被移除，计时自然消失，且由于<b>完成时才扣费</b>，
 * 不会出现“破坏了还扣 30000”。</p>
 *
 * <p>W10 的 {@link ManaCost} 是 stub（返回 false），所以真实运行中不会产生产物；
 * 真实 30000 扣费与对外释放 4500 由 W13 实现。</p>
 */
public class MagicAggregationTableBlockEntity extends AbstractMagicContainerBlockEntity {

    public static final int SIZE = 1;

    /** 固定 5 秒（100 tick）。 */
    public static final int CRAFT_TICKS = 100;

    public static final int HINT_NONE = 0;
    public static final int HINT_NO_MANA = 3;
    public static final int HINT_OUTPUT_BLOCKED = 4;

    private boolean crafting;
    private int craftTicks;
    private int hintCode;

    public MagicAggregationTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_AGGREGATION_TABLE, pos, state, SIZE);
    }

    public boolean isCrafting() {
        return this.crafting;
    }

    public int getCraftTicks() {
        return this.craftTicks;
    }

    /** 剩余秒数：100 tick → 5，99 → 5，80 → 4，……，1 → 1，0 → 0。 */
    public int remainingSeconds() {
        return (this.craftTicks + 19) / 20;
    }

    public int getHintCode() {
        return this.hintCode;
    }

    public void clearHint() {
        this.hintCode = HINT_NONE;
    }

    /** 产物格能否再接收 1 个普通魔法水晶。 */
    public boolean canAcceptCrystal() {
        ItemStack output = this.getItem(0);
        if (output.isEmpty()) {
            return true;
        }
        return output.is(MagicItems.magicCrystal)
                && output.getCount() < output.getMaxStackSize();
    }

    /** 开始一次 5 秒合成；不在此扣费。 */
    public void startCraft() {
        this.crafting = true;
        this.craftTicks = CRAFT_TICKS;
        this.hintCode = HINT_NONE;
        this.setChanged();
    }

    /** 服务端每 tick 调用。 */
    public void tick(ServerLevel level) {
        if (!this.crafting) {
            return;
        }
        if (this.craftTicks > 0) {
            this.craftTicks--;
        }
        if (this.craftTicks <= 0) {
            this.finishCraft(level);
        } else {
            this.setChanged();
        }
    }

    /**
     * 完成合成：先检查产物格，再在完成时扣 30000；失败只清计时并留下提示码。
     */
    private void finishCraft(ServerLevel level) {
        if (!this.canAcceptCrystal()) {
            this.crafting = false;
            this.craftTicks = 0;
            this.hintCode = HINT_OUTPUT_BLOCKED;
            this.setChanged();
            return;
        }

        if (!ManaCost.payAggregation(level, this.getBlockPos())) {
            this.crafting = false;
            this.craftTicks = 0;
            this.hintCode = HINT_NO_MANA;
            this.setChanged();
            return;
        }

        ItemStack output = this.getItem(0);
        if (output.isEmpty()) {
            this.setItem(0, new ItemStack(MagicItems.magicCrystal));
        } else {
            output.grow(1);
            this.setChanged();
        }
        this.crafting = false;
        this.craftTicks = 0;
        this.hintCode = HINT_NONE;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Crafting", this.crafting);
        output.putInt("CraftTicks", this.craftTicks);
        output.putInt("HintCode", this.hintCode);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.crafting = input.getBooleanOr("Crafting", false);
        this.craftTicks = input.getIntOr("CraftTicks", 0);
        this.hintCode = input.getIntOr("HintCode", HINT_NONE);
        if (this.crafting && this.craftTicks <= 0) {
            this.craftTicks = 1;
        }
    }
}
