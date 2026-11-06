package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.component.CrystalData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import net.minecraft.world.item.Item;

/**
 * 普通魔法水晶（W08 只注册与默认组件）。
 *
 * <p>默认 {@code CrystalData(0, false, false)}；设置 GUI 在 W12，容量/供能/消耗在 W13。</p>
 */
public class MagicCrystalItem extends Item {

    public MagicCrystalItem(Item.Properties properties) {
        super(properties.component(ModComponents.CRYSTAL_DATA, CrystalData.DEFAULT));
    }
}
