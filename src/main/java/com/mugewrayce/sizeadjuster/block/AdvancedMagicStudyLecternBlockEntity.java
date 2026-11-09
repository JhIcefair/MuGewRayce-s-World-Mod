package com.mugewrayce.sizeadjuster.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 高级魔法学习讲台 BlockEntity：5 格（与讲台完全相同的槽位结构）。 */
public class AdvancedMagicStudyLecternBlockEntity extends AbstractMagicContainerBlockEntity {

    public static final int SIZE = 5;

    public AdvancedMagicStudyLecternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_MAGIC_STUDY_LECTERN, pos, state, SIZE);
    }
}
