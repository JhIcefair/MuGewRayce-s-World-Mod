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

/**
 * 高级魔法学习讲台（W16）：与讲台共用菜单/屏幕，仅「更新学习证」不受条件限制。
 *
 * <p>方块本身仅通过创造分类获得（无配方）；不检查使用者是否创造模式（D-1）。</p>
 */
public class AdvancedMagicStudyLecternBlock extends AbstractFacingMagicBlock {

    public static final MapCodec<AdvancedMagicStudyLecternBlock> CODEC =
            BlockBehaviour.simpleCodec(AdvancedMagicStudyLecternBlock::new);

    public AdvancedMagicStudyLecternBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedMagicStudyLecternBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof AdvancedMagicStudyLecternBlockEntity lectern) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new MagicStudyLecternMenu(
                            id, inventory, lectern,
                            ContainerLevelAccess.create(level, pos), this, true),
                    Component.translatable(
                            "container.mugewrayce-mod.advanced_magic_study_lectern")));
        }
        return InteractionResult.SUCCESS;
    }
}
