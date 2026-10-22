package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
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
                    output.accept(ModItems.purificationCrystal);
                    output.accept(ModItems.purificationStaff);
                    output.accept(ModItems.purificationHolySword);
                    output.accept(ModItems.purificationLongSpear);
                    output.accept(ModItems.purificationHolyBow);
                    output.accept(ModItems.zeroReset);
                    output.accept(ModItems.bookOfLing);
                    output.accept(ModItems.originToken);
                    output.accept(ModItems.baishanBag);
                    output.accept(ModItems.purificationHalo);
                    output.accept(ModItems.lifeConversionToken);
                })
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabKey, tab);
    }
}
