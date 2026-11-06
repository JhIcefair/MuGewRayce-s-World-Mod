package com.mugewrayce.sizeadjuster.block;

import com.mojang.serialization.MapCodec;
import com.mugewrayce.sizeadjuster.container.MagicStudyLecternMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 魔法学习讲台（W08b 方块 + 容器；W11 起右键打开 41 格 GUI）。 */
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof MagicStudyLecternBlockEntity lectern) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new MagicStudyLecternMenu(
                            id, inventory, lectern,
                            ContainerLevelAccess.create(level, pos), this),
                    Component.translatable("container.mugewrayce-mod.magic_study_lectern")));
        }
        return InteractionResult.SUCCESS;
    }
}
