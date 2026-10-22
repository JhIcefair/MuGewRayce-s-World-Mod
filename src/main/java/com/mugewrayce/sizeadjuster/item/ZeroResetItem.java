package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.entity.PurificationOrbEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 归零：右键发射净化魔法球（归零变体，贴图 ls\16.png）。
 * - 单击：立即发射一枚；
 * - 长按：进入使用状态由 onUseTick 控制连发，连发间隔 2 tick（0.1 秒）
 *   ——原版按住右键每 4 tick 才重复一次 use()，本物品改为每 2 tick，快一倍。
 * - 弹道/命中/消失等见 PurificationOrbEntity 归零变体
 *   （直线飞行、2000 伤害、命中方块破块后消失、1024 格消失）。
 */
public class ZeroResetItem extends Item {

    private static final float SHOOT_VELOCITY = PurificationOrbEntity.VELOCITY;
    private static final float SHOOT_INACCURACY = 0.0F;

    /** 连发间隔（tick）：原版按住右键每 4 tick 重复一次，本物品每 2 tick（快一倍）。 */
    private static final int FIRE_INTERVAL_TICKS = 2;

    /** 使用状态持续时间（配合 onUseTick 自控连发节奏）。 */
    private static final int USE_DURATION = 72000;

    public ZeroResetItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // 单击：立即发射一发
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            fireOrb(serverLevel, player);
        }
        // 进入使用状态：长按时由 onUseTick 按连发间隔持续发射
        // （使用期间原版 4 tick 右键延迟不再介入，节奏完全由本物品控制）
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return;
        }
        int elapsed = USE_DURATION - remainingUseDuration;
        // elapsed: 1,2,3,... → 在 2,4,6,... 各发射一发 → 连发间隔 2 tick
        if (elapsed > 0 && elapsed % FIRE_INTERVAL_TICKS == 0) {
            fireOrb(serverLevel, player);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        return false;
    }

    private void fireOrb(ServerLevel level, Player player) {
        PurificationOrbEntity orb = new PurificationOrbEntity(level, player, false);
        orb.setVariant(PurificationOrbEntity.VARIANT_ZERO_RESET);
        orb.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        orb.shootFromRotation(player, player.getXRot(), player.getYRot(),
                0.0F, SHOOT_VELOCITY, SHOOT_INACCURACY);
        level.addFreshEntity(orb);
    }
}
