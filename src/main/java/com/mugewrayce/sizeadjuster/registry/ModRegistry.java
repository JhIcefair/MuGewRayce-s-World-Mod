package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.item.ModItems;

/**
 * 模组注册总入口。
 *
 * <p>注册顺序（W09 起）：<b>数据组件 → ModItems（1.x 物品）→ 方块/BlockItem/BlockEntityType
 * → ItemRegistry（12 件魔法物品）→ 实体 → 菜单 → 创造分类</b>。</p>
 *
 * <ul>
 *   <li>组件必须最先注册：物品属性会直接引用组件类型；</li>
 *   <li>BlockItem 必须晚于对应方块；BlockEntityType 也必须晚于方块；</li>
 *   <li>菜单在物品/方块/实体之后注册，因为菜单可能引用已注册的方块与物品；</li>
 *   <li>创造分类最后注册，其 displayItems 会引用前面已赋值的物品字段。</li>
 * </ul>
 */
public final class ModRegistry {

    private ModRegistry() {
    }

    public static void register() {
        ComponentRegistry.register();
        ModItems.register();
        BlockRegistry.register();
        ItemRegistry.register();
        EntityRegistry.register();
        MenuRegistry.register();
        CreativeTabRegistry.register();
    }
}
