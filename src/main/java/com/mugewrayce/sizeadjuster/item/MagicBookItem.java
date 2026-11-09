package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.component.MagicBookData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import net.minecraft.world.item.Item;

/**
 * 魔法书（W12 增加默认 {@link MagicBookData}；释放效果/耗魔留 W14）。
 *
 * <p>i 键打开设置界面；默认未选技能、范围 0、坐标 0,0,0、排除列表为空。
 * 旧书没有组件时读取方统一用 {@code getOrDefault(..., MagicBookData.DEFAULT)}。</p>
 */
public class MagicBookItem extends Item {

    public MagicBookItem(Item.Properties properties) {
        super(properties.component(ModComponents.MAGIC_BOOK_DATA, MagicBookData.DEFAULT));
    }
}
