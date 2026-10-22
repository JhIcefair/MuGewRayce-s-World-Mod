package com.mugewrayce.sizeadjuster.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 净化圣弓：套用原版弓功能，但无需背包中有弓箭即可发射（始终提供原版箭，
 * 且右键使用时不检查背包弓箭）。
 */
public class HolyBowItem extends BowItem {

    public HolyBowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // 原版 use() 在背包无箭时直接返回 FAIL，这里跳过该检查
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity shooter, int timeLeft) {
        if (!(shooter instanceof Player player)) {
            return false;
        }
        // 无需背包有箭：直接使用原版箭
        ItemStack projectile = new ItemStack(Items.ARROW);
        int i = this.getUseDuration(stack, shooter) - timeLeft;
        float power = getPowerForTime(i);
        if (power < 0.1F) {
            return false;
        }
        List<ItemStack> list = draw(stack, projectile, player);
        if (level instanceof ServerLevel serverLevel) {
            if (!list.isEmpty()) {
                this.shoot(serverLevel, player, player.getUsedItemHand(), stack, list,
                        power * 3.0F, 1.0F, power >= 1.0F, null);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }
}
