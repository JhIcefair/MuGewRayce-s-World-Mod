package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.component.ProficiencyData;
import com.mugewrayce.sizeadjuster.magic.CertLines;
import com.mugewrayce.sizeadjuster.magic.MagicDataRules;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * 魔法熟练证明（W18）：显示“升到 I 级 + 绑定的类型 / 模式 / 具体项”。
 *
 * <p>注册名与组件都不变（{@code magic_proficiency_certificate} / {@code proficiency_data}）；
 * 本类只补 tooltip。无组件或字段不合法（创造模式直接拿到的空证明）显示“无效证明”。</p>
 */
public class ProficiencyCertificateItem extends Item {

    public ProficiencyCertificateItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        ProficiencyData data = stack.get(ModComponents.PROFICIENCY_DATA);
        if (!MagicDataRules.isValidProficiency(data)) {
            tooltip.accept(Component.translatable("tooltip.mugewrayce-mod.proficiency.invalid"));
            return;
        }
        tooltip.accept(Component.translatable("tooltip.mugewrayce-mod.proficiency.target"));
        tooltip.accept(CertLines.describe(data.type(), data.mode(), data.sub(), 1));
    }
}
