package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.item.ModItems;

/**
 * 模组注册总入口（W01-B 骨架，W03 起加入组件）。
 *
 * <p>注册顺序：<b>数据组件</b> → 现有物品 → 新物品 → 实体 → 方块 → 创造分类。
 * 组件必须最先注册：W07/W09 的物品属性会直接引用组件类型；
 * 创造分类最后注册，其 displayItems 会引用前面已赋值的物品字段。
 */
public final class ModRegistry {

    private ModRegistry() {
    }

    public static void register() {
        ComponentRegistry.register();
        ModItems.register();
        ItemRegistry.register();
        EntityRegistry.register();
        BlockRegistry.register();
        CreativeTabRegistry.register();
    }
}
