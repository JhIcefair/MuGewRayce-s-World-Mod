package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.clientbridge.ScreenOpenerHolder;
import com.mugewrayce.sizeadjuster.component.CertData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.magic.CertLines;
import com.mugewrayce.sizeadjuster.magic.MagicDataRules;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * 魔法学习证（W12）：右键在客户端打开只读学习证界面。
 *
 * <p>界面直接读主手已同步的 {@code cert_data}；无组件 / 编号为 0 / 内容非法时
 * 显示“无效学习证”，不崩溃。创造模式直接拿到的证没有组件，即无效。</p>
 */
public class MagicStudyCertificateItem extends Item {

    public MagicStudyCertificateItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ScreenOpenerHolder.openStudyCert();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        CertData cert = stack.get(ModComponents.CERT_DATA);
        if (!MagicDataRules.isValidCert(cert)) {
            tooltip.accept(Component.translatable(
                    "gui.mugewrayce-mod.magic_study_certificate.invalid"));
            return;
        }
        tooltip.accept(Component.translatable(
                "gui.mugewrayce-mod.magic_study_certificate.number",
                String.format(Locale.ROOT, "%04d", cert.no())));
        // W17 精简：只留编号 + 去重后的类型，不再逐条列模式
        tooltip.accept(CertLines.describeTypes(cert.entries()));
    }
}
