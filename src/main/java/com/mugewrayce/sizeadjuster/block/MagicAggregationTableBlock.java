package com.mugewrayce.sizeadjuster.block;

import com.mojang.serialization.MapCodec;
import com.mugewrayce.sizeadjuster.container.MagicAggregationTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 魔法聚合台（W10 起：方块右键打开 GUI + 5 秒服务端计时）。 */
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof MagicAggregationTableBlockEntity table) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new MagicAggregationTableMenu(
                            id, inventory, table,
                            ContainerLevelAccess.create(level, pos), this),
                    Component.translatable("container.mugewrayce-mod.magic_aggregation_table")));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.MAGIC_AGGREGATION_TABLE) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof MagicAggregationTableBlockEntity table
                    && tickLevel instanceof ServerLevel server) {
                table.tick(server);
            }
        };
    }
}
