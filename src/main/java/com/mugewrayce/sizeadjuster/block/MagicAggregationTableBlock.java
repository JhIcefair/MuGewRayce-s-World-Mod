package com.mugewrayce.sizeadjuster.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** 魔法聚合台（W08b：方块 + 容器；GUI 留 W10）。 */
public class MagicAggregationTableBlock extends BaseEntityBlock {

    public static final MapCodec<MagicAggregationTableBlock> CODEC =
            BlockBehaviour.simpleCodec(MagicAggregationTableBlock::new);

    public MagicAggregationTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicAggregationTableBlockEntity(pos, state);
    }
}
