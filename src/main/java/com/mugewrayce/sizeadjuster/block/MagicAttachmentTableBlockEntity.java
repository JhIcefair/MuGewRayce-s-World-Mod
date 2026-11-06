package com.mugewrayce.sizeadjuster.block;

import com.mugewrayce.sizeadjuster.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 魔法附加台 BlockEntity：5 格（①学习证/附加书，②③④材料，⑤产物/中间）。 */
public class MagicAttachmentTableBlockEntity extends AbstractMagicContainerBlockEntity {

    public static final int SIZE = 5;

    public MagicAttachmentTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_ATTACHMENT_TABLE, pos, state, SIZE);
    }
}
