package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
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
 * 方块注册落点（W08b）。
 *
 * <p>顺序固定：3 个方块 → 3 个 BlockItem → 3 个 BlockEntityType。
 * BlockItem 必须晚于方块；BlockEntityType 必须晚于方块。注册总入口再把本类放在
 * {@link ItemRegistry} 之前。</p>
 *
 * <p>方块按普通木制功能方块处理：`strength(2.5F, 2.5F)`、斧类可挖、需要正确工具才掉落；
 * 具体挖掘工具草案未规定，按交接单建议记录。</p>
 */
public final class BlockRegistry {

    public static Block MAGIC_STUDY_LECTERN;
    public static Block MAGIC_ATTACHMENT_TABLE;
    public static Block MAGIC_AGGREGATION_TABLE;

    public static Item MAGIC_STUDY_LECTERN_ITEM;
    public static Item MAGIC_ATTACHMENT_TABLE_ITEM;
    public static Item MAGIC_AGGREGATION_TABLE_ITEM;

    private BlockRegistry() {
    }

    public static void register() {
        MAGIC_STUDY_LECTERN = registerBlock("magic_study_lectern", MagicStudyLecternBlock::new);
        MAGIC_ATTACHMENT_TABLE = registerBlock("magic_attachment_table", MagicAttachmentTableBlock::new);
        MAGIC_AGGREGATION_TABLE = registerBlock("magic_aggregation_table", MagicAggregationTableBlock::new);

        MAGIC_STUDY_LECTERN_ITEM = registerBlockItem("magic_study_lectern", MAGIC_STUDY_LECTERN);
        MAGIC_ATTACHMENT_TABLE_ITEM = registerBlockItem("magic_attachment_table", MAGIC_ATTACHMENT_TABLE);
        MAGIC_AGGREGATION_TABLE_ITEM = registerBlockItem("magic_aggregation_table", MAGIC_AGGREGATION_TABLE);

        ModBlockEntities.register();
    }

    private static Block registerBlock(String id, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id));
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .strength(2.5F, 2.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.WOOD)
                .setId(key);
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties));
    }

    private static Item registerBlockItem(String id, Block block) {
        return ItemRegistry.register(id, properties -> new BlockItem(block, properties));
    }
}
