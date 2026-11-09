package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.registry.BlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * 模组创造模式分类注册。
 * 分类"mugewrayce"（中文"沐玖芸萱"）：所有新添加物品的统一分类。
 * 注：原版 TOP 行已占用位置 0~6，本分类置于 TOP 行第 7 位。
 */
public final class ModCreativeTabs {

    public static final String TAB_ID = "mugewrayce";

    private ModCreativeTabs() {
    }

    public static void register() {
        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, TAB_ID));

        CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 7)
                .title(Component.translatable("itemGroup." + TAB_ID))
                .icon(() -> new ItemStack(ModItems.originToken))
                .displayItems((displayParameters, output) -> {
                    output.accept(ModItems.equipoiseCrystal);
                    output.accept(ModItems.equipoiseStaff);
                    output.accept(ModItems.equipoiseSword);
                    output.accept(ModItems.equipoiseBow);
                    output.accept(ModItems.zeroReset);
                    output.accept(ModItems.bookOfLing);
                    output.accept(ModItems.originToken);
                    output.accept(ModItems.baishanBag);
                    output.accept(ModItems.equipoiseHalo);
                    output.accept(ModItems.lifeConversionToken);

                    // W08 魔法体系 12 件（顺序按接口约定 §1.2）
                    output.accept(MagicItems.manaDetector);
                    output.accept(MagicItems.magicStaff);
                    output.accept(MagicItems.magicCrystal);
                    output.accept(MagicItems.eternalCrystal);
                    output.accept(MagicItems.magicStudyCertificate);
                    output.accept(MagicItems.magicBook);
                    output.accept(MagicItems.beginnerExamApplication);
                    output.accept(MagicItems.proficiencyCertificate);
                    output.accept(MagicItems.trialApplication);
                    output.accept(MagicItems.promotionCertificate);
                    // W10：附加书 I/II/III 各占一格
                    output.accept(AttachmentBookItem.createStack(MagicItems.attachmentBook, 1));
                    output.accept(AttachmentBookItem.createStack(MagicItems.attachmentBook, 2));
                    output.accept(AttachmentBookItem.createStack(MagicItems.attachmentBook, 3));
                    output.accept(MagicItems.lifeMigrationStone);

                    // W08 的三个方块物品
                    output.accept(BlockRegistry.MAGIC_STUDY_LECTERN_ITEM);
                    output.accept(BlockRegistry.MAGIC_ATTACHMENT_TABLE_ITEM);
                    output.accept(BlockRegistry.MAGIC_AGGREGATION_TABLE_ITEM);
                    output.accept(BlockRegistry.ADVANCED_MAGIC_STUDY_LECTERN_ITEM);
                })
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabKey, tab);
    }
}
