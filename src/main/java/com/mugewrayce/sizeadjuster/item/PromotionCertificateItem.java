package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.component.PromotionData;
import com.mugewrayce.sizeadjuster.magic.CertLines;
import com.mugewrayce.sizeadjuster.magic.MagicDataRules;
import com.mugewrayce.sizeadjuster.magic.MagicLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * 魔法试炼晋级认证（W17）：显示“晋级目标等级 + 绑定的类型 / 模式 / 具体项”。
 *
 * <p>注册名与组件都不变（{@code magic_trial_promotion_certificate} / {@code promotion_data}）；
 * 本类只补 tooltip。无组件（创造模式直接拿到的空认证）显示“无效认证”。</p>
 */
public class PromotionCertificateItem extends Item {

    public PromotionCertificateItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        PromotionData data = stack.get(ModComponents.PROMOTION_DATA);
        if (!MagicDataRules.isValidPromotion(data)) {
            tooltip.accept(Component.translatable("tooltip.mugewrayce-mod.promotion.invalid"));
            return;
        }
        tooltip.accept(Component.translatable("tooltip.mugewrayce-mod.promotion.target",
                levelName(data.targetLevel())));
        tooltip.accept(CertLines.describe(data.type(), data.mode(), data.sub(), data.targetLevel()));
    }

    /** 目标等级显示名：I~V 用罗马数字，初学者走语言键（有效认证不会出现初学者）。 */
    private static Component levelName(int numericLevel) {
        MagicLevel level = MagicLevel.fromNumericLevel(numericLevel);
        if (level == null) {
            return Component.literal(Integer.toString(numericLevel));
        }
        return level.isBeginner()
                ? Component.translatable(level.langKey())
                : Component.literal(level.enName());
    }
}
