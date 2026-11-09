package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

/**
 * 物品注册落点（W01-B 骨架）。
 * 2.0.0 新物品从这里进入；1.x 物品仍由 {@code ModItems} 注册。
 * W01-B 只建立落点与注册辅助方法，不保留任何常驻/临时物品。
 */
public final class ItemRegistry {

    private ItemRegistry() {
    }

    public static void register() {
        // W08 起 12 件魔法体系非方块物品统一委托 MagicItems。
        MagicItems.register();
    }

    /**
     * 统一物品注册入口：26.1 要求先 {@code setId(key)} 再构造，否则抛 "Item id not set"。
     */
    public static Item register(String id, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    /**
     * 方块物品专用：26.1 的 {@code BlockItem} 不再自动套 {@code block.} 前缀，
     * 必须显式 {@code useBlockDescriptionPrefix()}，否则 tooltip 显示 {@code item.*} 原始键。
     */
    public static Item registerBlockItem(String id, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id));
        return Registry.register(BuiltInRegistries.ITEM, key,
                factory.apply(new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }
}
