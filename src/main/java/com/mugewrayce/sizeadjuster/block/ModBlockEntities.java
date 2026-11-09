package com.mugewrayce.sizeadjuster.block;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.registry.BlockRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 三个魔法方块的 BlockEntityType 注册（W08b）。
 *
 * <p>ID 与方块 ID 相同（接口约定 §1.3）；必须在方块与 BlockItem 之后注册。</p>
 */
public final class ModBlockEntities {

    public static BlockEntityType<MagicStudyLecternBlockEntity> MAGIC_STUDY_LECTERN;
    public static BlockEntityType<MagicAttachmentTableBlockEntity> MAGIC_ATTACHMENT_TABLE;
    public static BlockEntityType<MagicAggregationTableBlockEntity> MAGIC_AGGREGATION_TABLE;
    public static BlockEntityType<AdvancedMagicStudyLecternBlockEntity> ADVANCED_MAGIC_STUDY_LECTERN;

    private ModBlockEntities() {
    }

    public static void register() {
        MAGIC_STUDY_LECTERN = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                key("magic_study_lectern"),
                FabricBlockEntityTypeBuilder.create(
                        MagicStudyLecternBlockEntity::new, BlockRegistry.MAGIC_STUDY_LECTERN).build());

        MAGIC_ATTACHMENT_TABLE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                key("magic_attachment_table"),
                FabricBlockEntityTypeBuilder.create(
                        MagicAttachmentTableBlockEntity::new, BlockRegistry.MAGIC_ATTACHMENT_TABLE).build());

        MAGIC_AGGREGATION_TABLE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                key("magic_aggregation_table"),
                FabricBlockEntityTypeBuilder.create(
                        MagicAggregationTableBlockEntity::new, BlockRegistry.MAGIC_AGGREGATION_TABLE).build());

        ADVANCED_MAGIC_STUDY_LECTERN = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                key("advanced_magic_study_lectern"),
                FabricBlockEntityTypeBuilder.create(
                        AdvancedMagicStudyLecternBlockEntity::new,
                        BlockRegistry.ADVANCED_MAGIC_STUDY_LECTERN).build());
    }

    private static ResourceKey<BlockEntityType<?>> key(String id) {
        return ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id));
    }
}
