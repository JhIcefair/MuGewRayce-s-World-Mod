package com.mugewrayce.sizeadjuster.block;

import com.mugewrayce.sizeadjuster.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 魔法学习讲台 BlockEntity：5 格（学习证 / 证明认证 / 无限附加台 / 无限聚合台 / 输出 A）。 */
public class MagicStudyLecternBlockEntity extends AbstractMagicContainerBlockEntity {

    public static final int SIZE = 5;

    public MagicStudyLecternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_STUDY_LECTERN, pos, state, SIZE);
    }
}
