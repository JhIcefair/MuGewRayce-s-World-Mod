package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.block.AdvancedMagicStudyLecternBlock;
import com.mugewrayce.sizeadjuster.block.CrystalBlock;
import com.mugewrayce.sizeadjuster.block.CrystalTableBlock;
import com.mugewrayce.sizeadjuster.block.MagicAggregationTableBlock;
import com.mugewrayce.sizeadjuster.block.MagicAttachmentTableBlock;
import com.mugewrayce.sizeadjuster.block.MagicStudyLecternBlock;
import com.mugewrayce.sizeadjuster.block.ModBlockEntities;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/**
 * 方块注册落点（W08b；W20 增水晶块 / 水晶拆合台）。
 *
 * <p>顺序固定：3 个魔法方块 → 3 个 BlockItem → 3 个 BlockEntityType；W16 追加高级讲台、
 * W20 追加水晶块与水晶拆合台（各自方块 → BlockItem）。BlockItem 必须晚于方块；
 * BlockEntityType 必须晚于方块。注册总入口再把本类放在 {@link ItemRegistry} 之前。</p>
 *
 * <p>魔法方块按普通木制功能方块处理：{@code strength(2.5F, 2.5F)}、斧类可挖、需要正确工具才掉落。
 * 水晶块例外：{@code strength(1.5F, 6.0F)}、镐类可挖、紫水晶音效（见
 * {@link #registerBlock(String, BlockBehaviour.Properties, Function)}）。</p>
 */
public final class BlockRegistry {

    public static Block MAGIC_STUDY_LECTERN;
    public static Block MAGIC_ATTACHMENT_TABLE;
    public static Block MAGIC_AGGREGATION_TABLE;
    public static Block ADVANCED_MAGIC_STUDY_LECTERN;
    /** W20：水晶块（无方向、六面同贴图、紫水晶音效、镐类可挖）。 */
    public static Block CRYSTAL_BLOCK;
    /** W20：水晶拆合台（水平四朝向、木系音效）。 */
    public static Block CRYSTAL_TABLE;

    public static Item MAGIC_STUDY_LECTERN_ITEM;
    public static Item MAGIC_ATTACHMENT_TABLE_ITEM;
    public static Item MAGIC_AGGREGATION_TABLE_ITEM;
    public static Item ADVANCED_MAGIC_STUDY_LECTERN_ITEM;
    public static Item CRYSTAL_BLOCK_ITEM;
    public static Item CRYSTAL_TABLE_ITEM;

    private BlockRegistry() {
    }

    public static void register() {
        MAGIC_STUDY_LECTERN = registerBlock("magic_study_lectern", MagicStudyLecternBlock::new);
        MAGIC_ATTACHMENT_TABLE = registerBlock("magic_attachment_table", MagicAttachmentTableBlock::new);
        MAGIC_AGGREGATION_TABLE = registerBlock("magic_aggregation_table", MagicAggregationTableBlock::new);
        ADVANCED_MAGIC_STUDY_LECTERN = registerBlock(
                "advanced_magic_study_lectern", AdvancedMagicStudyLecternBlock::new);
        // W20：水晶块用自定义属性（挖掘速度同原石、紫水晶音效），不走魔法方块的木系默认值
        CRYSTAL_BLOCK = registerBlock("crystal_block",
                BlockBehaviour.Properties.of()
                        .strength(1.5F, 6.0F)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.AMETHYST),
                CrystalBlock::new);
        CRYSTAL_TABLE = registerBlock("crystal_table", CrystalTableBlock::new);

        MAGIC_STUDY_LECTERN_ITEM = registerBlockItem("magic_study_lectern", MAGIC_STUDY_LECTERN);
        MAGIC_ATTACHMENT_TABLE_ITEM = registerBlockItem("magic_attachment_table", MAGIC_ATTACHMENT_TABLE);
        MAGIC_AGGREGATION_TABLE_ITEM = registerBlockItem("magic_aggregation_table", MAGIC_AGGREGATION_TABLE);
        ADVANCED_MAGIC_STUDY_LECTERN_ITEM = registerBlockItem(
                "advanced_magic_study_lectern", ADVANCED_MAGIC_STUDY_LECTERN);
        CRYSTAL_BLOCK_ITEM = registerBlockItem("crystal_block", CRYSTAL_BLOCK);
        CRYSTAL_TABLE_ITEM = registerBlockItem("crystal_table", CRYSTAL_TABLE);

        ModBlockEntities.register();
    }

    private static Block registerBlock(String id, Function<BlockBehaviour.Properties, Block> factory) {
        return registerBlock(id, BlockBehaviour.Properties.of()
                .strength(2.5F, 2.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.WOOD), factory);
    }

    /**
     * W20：可传自定义属性的方块注册（水晶块要用自己的硬度 / 音效组）。
     *
     * <p>两种入口都会补 {@code setId(key)}：26.1 的方块也必须先写 id 再构造。</p>
     */
    private static Block registerBlock(String id, BlockBehaviour.Properties properties,
                                       Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    private static Item registerBlockItem(String id, Block block) {
        return ItemRegistry.registerBlockItem(id, properties -> new BlockItem(block, properties));
    }
}
