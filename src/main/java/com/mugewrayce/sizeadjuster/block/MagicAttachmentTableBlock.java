package com.mugewrayce.sizeadjuster.block;

import com.mojang.serialization.MapCodec;
import com.mugewrayce.sizeadjuster.container.MagicAttachmentTableMenu;
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

/** 魔法附加台（W10 起：方块右键打开 GUI；合成/附加为瞬时服务端流程）。 */
public class MagicAttachmentTableBlock extends BaseEntityBlock {

    public static final MapCodec<MagicAttachmentTableBlock> CODEC =
            BlockBehaviour.simpleCodec(MagicAttachmentTableBlock::new);

    public MagicAttachmentTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicAttachmentTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof MagicAttachmentTableBlockEntity table) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new MagicAttachmentTableMenu(
                            id, inventory, table,
                            ContainerLevelAccess.create(level, pos), this),
                    Component.translatable("container.mugewrayce-mod.magic_attachment_table")));
        }
        return InteractionResult.SUCCESS;
    }
}
