package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.component.MagicBookData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.clientbridge.ScreenOpenerHolder;
import com.mugewrayce.sizeadjuster.magic.cast.BookCaster;
import com.mugewrayce.sizeadjuster.magic.cast.TimePauseZone;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 魔法书（W12 设置界面 / W14b 右键释放入口 / W17 时间暂停改长按维持）。
 *
 * <p>右键：未选技能 → 提示“未选择技能”。</p>
 * <ul>
 *   <li><b>时间暂停</b>：按住右键即启动区域（客户端与服务端都进入“使用中”，
 *       服务端调 {@link BookCaster#cast} 启动；松手立即结束），右键不再打开排除目标界面，
 *       排除名单只在 i 键设置界面里编辑；</li>
 *   <li><b>空间传送</b>：右键打开坐标界面，确认后发包传送（保持 W14b 行为）。</li>
 * </ul>
 */
public class MagicBookItem extends Item {

    /** 按住右键的最大使用时长（约 1 小时，足够维持一次时间暂停）。 */
    private static final int USE_DURATION = 72000;

    public MagicBookItem(Item.Properties properties) {
        super(properties.component(ModComponents.MAGIC_BOOK_DATA, MagicBookData.DEFAULT));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        MagicBookData data = stack.getOrDefault(ModComponents.MAGIC_BOOK_DATA, MagicBookData.DEFAULT);
        if (data.skill() == null || data.skill().isEmpty()) {
            if (level.isClientSide()) {
                player.sendSystemMessage(
                        Component.translatable("message.mugewrayce-mod.no_skill_selected"));
            }
            return InteractionResult.SUCCESS;
        }
        if (MagicBookData.SKILL_TIME_PAUSE.equals(data.skill())) {
            // W17：长按维持——两端都 startUsingItem，服务端负责启动区域（启动耗魔按现有逻辑）
            player.startUsingItem(hand);
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                BookCaster.cast(serverPlayer, data);
            }
            return InteractionResult.CONSUME;
        }
        if (level.isClientSide() && MagicBookData.SKILL_SPACE_TELEPORT.equals(data.skill())) {
            ScreenOpenerHolder.openMagicBookTeleport();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        // 服务端兜底：区域自行结束（走远 / 扣不起魔力 / 被清理）后立即停止“使用中”
        if (level instanceof ServerLevel && entity instanceof ServerPlayer player
                && !TimePauseZone.isActive(player.getUUID())) {
            player.stopUsingItem();
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        // W17 §7 默认决定：松手立即结束，不保留剩余时间
        if (level instanceof ServerLevel && entity instanceof ServerPlayer player) {
            TimePauseZone.end(player.getUUID());
            return true;
        }
        return false;
    }
}
