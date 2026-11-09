package com.mugewrayce.sizeadjuster.client;

import com.mugewrayce.sizeadjuster.component.AttachmentData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 被附加书附加过的武器：面板只显示倍率（草案 §3.25，W14a）。
 *
 * <p>用语言中性的 {@code ×1 / ×2 / ×3} 字面量，不新增语言键；物理伤害数值不显示。</p>
 */
public final class AttachmentTooltip {

    private AttachmentTooltip() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                            net.minecraft.world.item.TooltipFlag flag,
                                            java.util.List<Component> lines) -> {
            int level = stack.getOrDefault(ModComponents.ATTACHMENT_DATA, AttachmentData.DEFAULT).level();
            if (level >= 1 && level <= 3) {
                lines.add(Component.literal("×" + level));
            }
        });
    }
}
