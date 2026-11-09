package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.component.StaffData;
import com.mugewrayce.sizeadjuster.component.StaffSelectionData;
import com.mugewrayce.sizeadjuster.magic.CertLines;
import com.mugewrayce.sizeadjuster.magic.cast.StaffCaster;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * 普通魔法杖（W08 注册 / W12 当前选择组件 / W14b 施法）。
 *
 * <p>右键读 {@code staff_selection_data} + {@code staff_data.entries}，服务端校验后施法；
 * 耗魔走 {@code ManaService.tryConsume}。持续线式 / 普通防御为按住施法。</p>
 */
public class MagicStaffItem extends Item {

    /** 按住施法的最大使用时长（与净化法杖一致）。 */
    private static final int USE_DURATION = 72000;

    public MagicStaffItem(Item.Properties properties) {
        super(properties
                .component(ModComponents.STAFF_DATA, StaffData.DEFAULT)
                .component(ModComponents.STAFF_SELECTION_DATA, StaffSelectionData.DEFAULT));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return StaffCaster.use(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        StaffCaster.onUseTick(level, entity, stack, remainingUseDuration);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        return StaffCaster.releaseUsing(level, entity, stack, timeLeft);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        StaffData staff = stack.getOrDefault(ModComponents.STAFF_DATA, StaffData.DEFAULT);
        if (staff.boundCertNo() > 0) {
            tooltip.accept(Component.translatable("tooltip.mugewrayce-mod.magic_staff.bound",
                    String.format(Locale.ROOT, "%04d", staff.boundCertNo())));
        } else {
            tooltip.accept(Component.translatable("tooltip.mugewrayce-mod.magic_staff.unbound"));
        }
        if (staff.entries().isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.mugewrayce-mod.magic_staff.empty"));
            return;
        }
        // W17 精简：只留学习证编号 + 去重后的类型，不再列“已附加（N 项）”与逐条模式
        tooltip.accept(CertLines.describeTypes(staff.entries()));
    }
}
