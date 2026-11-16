package com.mugewrayce.sizeadjuster.block;

import com.mojang.serialization.MapCodec;
import com.mugewrayce.sizeadjuster.container.CrystalTableMenu;
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
 * 水晶拆合台（W20）：右键打开拆合界面，水平四朝向，无 ticker。
 *
 * <p>拆分（中心放水晶块 → 环上预览 8 个水晶）与合成（环上各放 1 个同种水晶 →
 * 中心预览 1 个水晶块）都在 {@link CrystalTableMenu} + {@link CrystalTableBlockEntity} 里；
 * 方块本身只负责开界面。</p>
 */
public class CrystalTableBlock extends AbstractFacingMagicBlock {

    public static final MapCodec<CrystalTableBlock> CODEC =
            BlockBehaviour.simpleCodec(CrystalTableBlock::new);

    public CrystalTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrystalTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof CrystalTableBlockEntity table) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new CrystalTableMenu(
                            id, inventory, table,
                            ContainerLevelAccess.create(level, pos), this),
                    Component.translatable("container.mugewrayce-mod.crystal_table")));
        }
        return InteractionResult.SUCCESS;
    }
}
