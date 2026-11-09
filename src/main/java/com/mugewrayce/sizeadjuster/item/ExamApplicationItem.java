package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.component.ApplicationData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.magic.CastMode;
import com.mugewrayce.sizeadjuster.magic.CastSubMode;
import com.mugewrayce.sizeadjuster.magic.MagicLevel;
import com.mugewrayce.sizeadjuster.magic.MagicType;
import com.mugewrayce.sizeadjuster.magic.exam.ExamManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * 考试 / 试炼申请书（W15；两份共用，按 {@code application_data.targetLevel} 区分）。
 *
 * <p>手持右键方块 / 地面开启；<b>只有开启成功才消耗 1 个</b>（D-1 / D-16，创造模式同样消耗）。
 * 非法物品（创造直接拿的、没组件的）不消耗、不开启。</p>
 */
public class ExamApplicationItem extends Item {

    private final boolean beginner;

    /**
     * @param beginner true = 初学魔法考试申请书（targetLevel 必须为 1）；
     *                 false = 魔法试炼申请书（targetLevel 2~5）
     */
    public ExamApplicationItem(boolean beginner, Properties properties) {
        super(properties);
        this.beginner = beginner;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return this.tryStart(player, context.getLevel(), context.getItemInHand(),
                context.getClickedPos());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer)) {
            // 客户端只回摆动；真正的开启在服务端
            return InteractionResult.SUCCESS;
        }
        return this.tryStart(player, serverLevel, player.getItemInHand(hand),
                player.blockPosition());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        ApplicationData data = stack.get(ModComponents.APPLICATION_DATA);
        // 无组件的无效申请书不显示“目标：”行（W16 §2.5 / §14）。
        if (data == null || data.type().isEmpty() || data.mode().isEmpty()) {
            return;
        }
        tooltip.accept(Component.translatable(
                "tooltip.mugewrayce-mod.exam_application.target", target(data)));
    }

    private static Component target(ApplicationData data) {
        MutableComponent line = Component.empty();
        MagicType type = MagicType.fromId(data.type());
        line.append(type == null ? Component.literal(data.type())
                : Component.translatable(type.langKey()));
        CastMode mode = CastMode.fromId(data.mode());
        line.append(" · ");
        line.append(mode == null ? Component.literal(data.mode())
                : Component.translatable(mode.langKey()));
        if (mode != null && mode != CastMode.POINT
                && data.sub() != null && !data.sub().isEmpty()) {
            CastSubMode sub = CastSubMode.fromId(data.sub());
            line.append(" · ");
            line.append(sub == null ? Component.literal(data.sub())
                    : Component.translatable(sub.langKey()));
        }
        line.append("  ");
        MagicLevel level = MagicLevel.fromNumericLevel(data.targetLevel());
        if (level == null || level.isBeginner()) {
            line.append(Component.translatable(
                    "tooltip.mugewrayce-mod.exam_application.beginner"));
        } else {
            line.append(Component.literal(level.enName()));
        }
        return line;
    }

    private InteractionResult tryStart(Player player, Level level, ItemStack stack, net.minecraft.core.BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        ApplicationData data = stack.get(ModComponents.APPLICATION_DATA);
        boolean matchesItem = data != null && (this.beginner == (data.targetLevel() <= 1));
        if (!matchesItem) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "command.mugewrayce.item.invalid", stack.getHoverName()));
            return InteractionResult.SUCCESS;
        }
        if (ExamManager.start(serverPlayer, pos, stack)) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
