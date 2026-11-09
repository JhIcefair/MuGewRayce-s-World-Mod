package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.entity.EquipoiseCircleEntity;
import com.mugewrayce.sizeadjuster.magic.SoundFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
 * 增益效果定义见 EquipoiseEffects（力量X/生命恢复20/抗性提升50/抗火/伤害吸收X/饱和）。
 */
public class EquipoiseCrystalItem extends LiftableItem {

    /**
     * 每个玩家当前的净化法阵。
     * 注意：物品实例是全局共享的（单个 Item 对象），必须按玩家 UUID 记录，不能存字段。
     */
    private static final Map<UUID, EquipoiseCircleEntity> CIRCLES = new HashMap<>();

    public EquipoiseCrystalItem(Item.Properties properties) {
        super(properties);
    }

    /** W17：开始长按 → 起手音效（原版音效复用，服务端播放）。 */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        SoundFx.at(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.2F);
        return super.use(level, player, hand);
    }

    /** 长按期间：每 tick 施加效果（保持不消失），并生成/跟随净化法阵。 */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel && entity instanceof Player player) {
            EquipoiseEffects.apply(player, EquipoiseEffects.CRYSTAL_DURATION_TICKS);
            // 净化法阵：没有则生成于脚下，有则跟随玩家移动
            EquipoiseCircleEntity circle = CIRCLES.get(player.getUUID());
            if (circle == null || !circle.isAlive()) {
                circle = new EquipoiseCircleEntity(level, player);
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
            // W17：松手结束 → 收束音效
            SoundFx.at(player, SoundEvents.BEACON_DEACTIVATE, 0.5F, 1.4F);
            EquipoiseEffects.apply(player, EquipoiseEffects.CRYSTAL_DURATION_TICKS);
            EquipoiseCircleEntity circle = CIRCLES.remove(player.getUUID());
            if (circle != null && circle.isAlive()) {
                circle.discard();
            }
        }
        return false;
    }

    /** 玩家离开时清理其净化法阵（W01 4.4，避免静态表残留到下一次会话）。 */
    public static void clearPlayer(UUID owner) {
        EquipoiseCircleEntity circle = CIRCLES.remove(owner);
        if (circle != null && circle.isAlive()) {
            circle.discard();
        }
    }
}
