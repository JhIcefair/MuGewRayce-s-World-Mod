package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.component.ApplicationData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.magic.exam.ExamManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

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
