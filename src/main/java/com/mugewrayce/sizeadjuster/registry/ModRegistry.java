package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.item.ModItems;

/**
 * 模组注册总入口（W01-B 骨架）。
 *
 * <p>注册顺序：现有物品 → 新物品 → 实体 → 方块 → 创造分类。
 * 创造分类最后注册，其 displayItems 会引用前面已赋值的物品字段。
 */
public final class ModRegistry {

    private ModRegistry() {
    }

    public static void register() {
        ModItems.register();
        ItemRegistry.register();
        EntityRegistry.register();
        BlockRegistry.register();
        CreativeTabRegistry.register();
    }
}
