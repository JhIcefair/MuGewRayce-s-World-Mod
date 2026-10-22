package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.entity.PurificationCircleEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 净化水晶：右键长按举起（BOW 动画），举起期间持续为使用者提供增益状态效果，
 * 并在玩家脚下生成净化法阵（跟随玩家移动）；松开后效果维持 60 秒、法阵消失。
 * 增益效果定义见 PurificationEffects（力量X/生命恢复20/抗性提升50/抗火/伤害吸收X/饱和）。
 */
public class CrystalItem extends LiftableItem {

    /**
     * 每个玩家当前的净化法阵。
     * 注意：物品实例是全局共享的（单个 Item 对象），必须按玩家 UUID 记录，不能存字段。
     */
    private static final Map<UUID, PurificationCircleEntity> CIRCLES = new HashMap<>();

    public CrystalItem(Item.Properties properties) {
        super(properties);
    }

    /** 长按期间：每 tick 施加效果（保持不消失），并生成/跟随净化法阵。 */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel && entity instanceof Player player) {
            PurificationEffects.apply(player, PurificationEffects.CRYSTAL_DURATION_TICKS);
            // 净化法阵：没有则生成于脚下，有则跟随玩家移动
            PurificationCircleEntity circle = CIRCLES.get(player.getUUID());
            if (circle == null || !circle.isAlive()) {
                circle = new PurificationCircleEntity(level, player);
                circle.setPos(player.getX(), player.getY() + 0.02, player.getZ());
                serverLevel.addFreshEntity(circle);
                CIRCLES.put(player.getUUID(), circle);
            } else {
                circle.setPos(player.getX(), player.getY() + 0.02, player.getZ());
            }
        }
    }

    /** 松开后：效果维持 60 秒（再次以 60 秒时长施加），净化法阵消失。 */
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide() && level instanceof ServerLevel && entity instanceof Player player) {
            PurificationEffects.apply(player, PurificationEffects.CRYSTAL_DURATION_TICKS);
            PurificationCircleEntity circle = CIRCLES.remove(player.getUUID());
            if (circle != null && circle.isAlive()) {
                circle.discard();
            }
        }
        return false;
    }
}
