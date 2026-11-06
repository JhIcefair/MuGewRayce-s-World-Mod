package com.mugewrayce.sizeadjuster.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** 魔法学习讲台（W08b：方块 + 容器；GUI 留 W11）。 */
public class MagicStudyLecternBlock extends BaseEntityBlock {

    public static final MapCodec<MagicStudyLecternBlock> CODEC =
            BlockBehaviour.simpleCodec(MagicStudyLecternBlock::new);

    public MagicStudyLecternBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicStudyLecternBlockEntity(pos, state);
    }
}
