package com.mugewrayce.sizeadjuster.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 水晶块（W20）：无方向、六面同贴图的普通方块。
 *
 * <p>属性在 {@code BlockRegistry} 里给：{@code strength(1.5F, 6.0F)}（挖掘速度同原石）、
 * {@code requiresCorrectToolForDrops()}（空手不掉落，任何材质镐都算正确工具）、
 * 音效组 {@code SoundType.AMETHYST}（原版紫水晶）。</p>
 *
 * <p>掉落 4~9 个水晶，<b>不吃时运与精准采集</b>：战利品表里只有
 * {@code set_count(uniform 4~9)}，既没有 {@code apply_bonus} 也没有 silk_touch 分支。</p>
 */
public class CrystalBlock extends Block {

    public static final MapCodec<CrystalBlock> CODEC =
            BlockBehaviour.simpleCodec(CrystalBlock::new);

    public CrystalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
