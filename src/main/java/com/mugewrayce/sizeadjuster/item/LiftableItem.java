package com.mugewrayce.sizeadjuster.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * 可举起物品：右键长按手臂前伸举平，无其他功能。
 * 默认动画 BOW（双手前伸，用于净化水晶、净化法杖）；
 * 可用构造器指定单臂动画（如 BRUSH，仅右臂举平，用于起源令牌）。
 */
public class LiftableItem extends Item {

    private static final int HOLD_DURATION = 72000;

    private final ItemUseAnimation useAnimation;

    public LiftableItem(Item.Properties properties) {
        this(properties, ItemUseAnimation.BOW);
    }

    public LiftableItem(Item.Properties properties, ItemUseAnimation useAnimation) {
        super(properties);
        this.useAnimation = useAnimation;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return HOLD_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return useAnimation;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        // 举起动作无其他功能
        return false;
    }
}
