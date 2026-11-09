package com.mugewrayce.sizeadjuster.block;

import com.mugewrayce.sizeadjuster.component.CrystalData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.magic.SoundFx;
import com.mugewrayce.sizeadjuster.magic.mana.ManaCost;
import com.mugewrayce.sizeadjuster.magic.mana.ManaService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 魔法聚合台 BlockEntity：1 格产物 + 合成计时 + 合成后的对外释放（W10 / W13）。
 *
 * <p>计时必须放在 BlockEntity，而不是菜单：玩家关闭 GUI / 死亡后合成要继续；
 * 方块被破坏时 BE 被移除，计时自然消失，且由于<b>完成时才扣费</b>，
 * 不会出现“破坏了还扣 30000”。</p>
 *
 * <p>W13 起：</p>
 * <ul>
 *   <li>产物写入 {@code CrystalData(30000, false, false)}（“合出 30000”）；</li>
 *   <li>每合成一个水晶，设置 {@link #RELEASE_TOTAL} 的释放预算：<b>每秒每区块 100</b>，
 *       作用于附近 3×3；某区块距默认最大值 &lt;{@link #RELEASE_STOP_DISTANCE} 就不再接收；
 *       9 个区块全部满/预算耗尽即清空；</li>
 *   <li>释放状态<b>不入存档</b>：破坏即丢，重新放下也不继续。</li>
 * </ul>
 */
public class MagicAggregationTableBlockEntity extends AbstractMagicContainerBlockEntity {

    public static final int SIZE = 1;

    /** 固定 5 秒（100 tick）。 */
    public static final int CRAFT_TICKS = 100;

    /** 每个水晶的释放总量上限（9×100×5 = 正好 5 秒）。 */
    public static final int RELEASE_TOTAL = 4500;
    /** 每秒每区块释放量。 */
    public static final int RELEASE_PER_CHUNK = 100;
    /** 目标区块距默认最大值不到该值就停止接收。 */
    public static final int RELEASE_STOP_DISTANCE = 300;
    /** 释放节奏：每秒一次。 */
    public static final int RELEASE_INTERVAL_TICKS = 20;

    public static final int HINT_NONE = 0;
    public static final int HINT_NO_MANA = 3;
    public static final int HINT_OUTPUT_BLOCKED = 4;

    private boolean crafting;
    private int craftTicks;
    private int hintCode;
    /** 释放预算：不持久化（破坏即丢）。 */
    private int releaseBudget;
    private int releaseTick;

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

    /** 剩余释放预算（自检/诊断用；不入存档）。 */
    public int getReleaseBudget() {
        return this.releaseBudget;
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

    /** 服务端每 tick 调用：合成倒计时 + 每秒释放。 */
    public void tick(ServerLevel level) {
        if (this.crafting) {
            if (this.craftTicks > 0) {
                this.craftTicks--;
            }
            if (this.craftTicks <= 0) {
                this.finishCraft(level);
            } else {
                this.setChanged();
            }
        }
        if (this.releaseBudget > 0) {
            this.releaseTick++;
            if (this.releaseTick >= RELEASE_INTERVAL_TICKS) {
                this.releaseTick = 0;
                this.releaseSecond(level);
            }
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
            this.setItem(0, newCrystal());
        } else {
            output.grow(1);
            this.setChanged();
        }
        // 每合成一个水晶：4500 释放预算（逐秒每区块 100）。
        this.releaseBudget = RELEASE_TOTAL;
        this.releaseTick = 0;
        this.crafting = false;
        this.craftTicks = 0;
        this.hintCode = HINT_NONE;
        this.setChanged();
        SoundFx.at(level, this.getBlockPos(), SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.2F);
    }

    /** 产物：普通魔法水晶，带 30000 魔力（W12 之前产的是默认 0）。 */
    private static ItemStack newCrystal() {
        ItemStack stack = new ItemStack(MagicItems.magicCrystal);
        stack.set(ModComponents.CRYSTAL_DATA, new CrystalData(CrystalData.CAPACITY, false, false));
        return stack;
    }

    /**
     * 每秒释放：附近 3×3 每区块 +100，预算 -100；距默认最大值 &lt;300 的区块不再接收；
     * 预算耗尽或 9 个区块全部满 → 清空释放状态。
     */
    private void releaseSecond(ServerLevel level) {
        ChunkPos center = ChunkPos.containing(this.getBlockPos());
        boolean allFull = true;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                ChunkPos target = new ChunkPos(center.x() + dx, center.z() + dz);
                int max = ManaService.getMax(level, target.x(), target.z());
                int current = ManaService.get(level, target.x(), target.z());
                if (max - current < RELEASE_STOP_DISTANCE) {
                    continue;
                }
                allFull = false;
                if (this.releaseBudget <= 0) {
                    continue;
                }
                int amount = Math.min(RELEASE_PER_CHUNK, this.releaseBudget);
                ManaService.addChunk(level, target, amount);
                this.releaseBudget -= amount;
            }
        }
        if (this.releaseBudget <= 0 || allFull) {
            this.releaseBudget = 0;
            this.releaseTick = 0;
            this.setChanged();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Crafting", this.crafting);
        output.putInt("CraftTicks", this.craftTicks);
        output.putInt("HintCode", this.hintCode);
        // 释放预算/节奏有意不保存：破坏或重载即丢，重新放下不继续（草案 §4.3）。
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
