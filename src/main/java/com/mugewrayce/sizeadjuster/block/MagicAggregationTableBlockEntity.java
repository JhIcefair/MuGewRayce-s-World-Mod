package com.mugewrayce.sizeadjuster.block;

import com.mugewrayce.sizeadjuster.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 魔法聚合台 BlockEntity：1 格（产物格）。 */
public class MagicAggregationTableBlockEntity extends AbstractMagicContainerBlockEntity {

    public static final int SIZE = 1;

    public MagicAggregationTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_AGGREGATION_TABLE, pos, state, SIZE);
    }
}
