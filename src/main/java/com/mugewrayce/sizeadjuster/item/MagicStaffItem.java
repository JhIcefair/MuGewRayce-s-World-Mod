package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.component.StaffData;
import net.minecraft.world.item.Item;

/**
 * 普通魔法杖（W08 只注册与默认组件）。
 *
 * <p>默认 {@link StaffData#DEFAULT}（未绑定学习证、无附加内容）；合成/绑定/升级 GUI 在 W10，
 * 施法在 W14。读取组件时后续任务统一用 {@code getOrDefault}，不要直接 {@code get}。</p>
 */
public class MagicStaffItem extends Item {

    public MagicStaffItem(Item.Properties properties) {
        super(properties.component(ModComponents.STAFF_DATA, StaffData.DEFAULT));
    }
}
