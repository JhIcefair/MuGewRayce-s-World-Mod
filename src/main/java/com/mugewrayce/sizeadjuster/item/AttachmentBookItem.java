package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.component.AttachmentBookData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * 魔法附加书（W10）：自身带 I/II/III 等级，供附加台分支 B 使用。
 *
 * <p>旧 W08 造出的书没有 {@code attachment_book_data}，读取时按 I 级处理；
 * 武器上的倍率仍写 {@code attachment_data}，不要混用。</p>
 */
public class AttachmentBookItem extends Item {

    public AttachmentBookItem(Item.Properties properties) {
        super(properties.component(ModComponents.ATTACHMENT_BOOK_DATA, AttachmentBookData.DEFAULT));
    }

    /** 创造分类用：生成指定等级（1~3）的附加书。 */
    public static ItemStack createStack(Item item, int level) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.ATTACHMENT_BOOK_DATA,
                new AttachmentBookData(Math.max(1, Math.min(3, level))));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        int level = stack.getOrDefault(ModComponents.ATTACHMENT_BOOK_DATA, AttachmentBookData.DEFAULT).level();
        tooltip.accept(Component.translatable(
                "tooltip.mugewrayce-mod.magic_attachment_book.level", roman(level)));
    }

    private static String roman(int level) {
        return switch (level) {
            case 2 -> "II";
            case 3 -> "III";
            default -> "I";
        };
    }
}
