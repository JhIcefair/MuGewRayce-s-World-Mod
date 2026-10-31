package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.item.ModCreativeTabs;

/**
 * 物品组（创造分类）注册落点（W01-B 骨架）。
 * 当前先委托 1.x 的 {@link ModCreativeTabs}：分类 {@code mugewrayce} 位于 TOP 行第 7 位，
 * 中文名“沐玖芸萱”。
 */
public final class CreativeTabRegistry {

    private CreativeTabRegistry() {
    }

    public static void register() {
        ModCreativeTabs.register();
    }
}
