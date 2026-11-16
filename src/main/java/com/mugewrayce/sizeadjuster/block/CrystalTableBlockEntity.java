package com.mugewrayce.sizeadjuster.block;

import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 水晶拆合台的 BlockEntity（W20）：9 格容器（0=中心，1~8=周围环）+ 一个模式字段。
 *
 * <p>模式：{@code 0=拆分}（中心是真实输入，环是输出预览）、
 * {@code 1=合成}（环是真实输入，中心是输出预览）。模式存 NBT，破坏方块后内容物消失
 * （与既有三张台一致）。</p>
 *
 * <p>预览格不落盘：它们由 {@code CrystalTableSlot} 按模式实时算出，
 * 真实输入永远只在另一侧，因此漏斗也按 {@link #canPlaceItem(int, ItemStack)} 被挡在预览侧外。</p>
 */
public class CrystalTableBlockEntity extends AbstractMagicContainerBlockEntity {

    /** 容器格数：0=中心，1~8=环。 */
    public static final int SIZE = 9;

    /** 中心格下标。 */
    public static final int SLOT_CENTER = 0;

    /** 模式：拆分（中心放水晶块 → 环上得 8 个水晶）。 */
    public static final int MODE_SPLIT = 0;

    /** 模式：合成（环上各放 1 个同种水晶 → 中心得 1 个水晶块）。 */
    public static final int MODE_CRAFT = 1;

    private int mode = MODE_SPLIT;

    public CrystalTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_TABLE, pos, state, SIZE);
    }

    public int getMode() {
        return this.mode;
    }

    /** 切换模式；调用方（菜单）负责先做“将要变成预览的一侧必须为空”的检查。 */
    public void setMode(int mode) {
        if (mode != MODE_SPLIT && mode != MODE_CRAFT) {
            return;
        }
        if (this.mode == mode) {
            return;
        }
        this.mode = mode;
        this.setChanged();
    }

    /** 该格在当前模式下是不是“真实输入”（否则是输出预览）。 */
    public boolean isInputSlot(int slot) {
        boolean center = slot == SLOT_CENTER;
        return this.mode == MODE_SPLIT ? center : !center;
    }

    /** 漏斗 / 自动化：只允许往真实输入侧放对应物品，挡住往预览格塞东西。 */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SIZE || stack.isEmpty() || !this.isInputSlot(slot)) {
            return false;
        }
        if (slot == SLOT_CENTER) {
            return stack.is(BlockRegistry.CRYSTAL_BLOCK_ITEM);
        }
        return stack.is(MagicItems.crystal);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Mode", this.mode);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int loaded = input.getIntOr("Mode", MODE_SPLIT);
        this.mode = loaded == MODE_CRAFT ? MODE_CRAFT : MODE_SPLIT;
    }
}
