package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.entity.PurificationFormationEntity;
import com.mugewrayce.sizeadjuster.entity.PurificationOrbEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 净化法杖：按切换 GUI 选择的施法模式释放（StaffCastModes）。
 *
 * MODE_ORB（魔法球）：两种发射方式——
 * 1. 单击右键（< 充能阈值）：立即发射一枚小魔法球（50 伤害；命中方块或超 1024 格消失）；
 * 2. 长按右键（≥ 充能阈值）：魔法球先出现在法杖前充能悬停，松开后发射
 *    （75 伤害 + 小爆炸；命中方块 TNT 规模爆炸；超 1024 格消失）。
 *
 * 阵法模式（长按召唤）：
 * - 单体治愈：对自己使用，跟随自己脚下；
 * - 群体治愈 / 群体伤害：固定于施法落点（半径 3 格、上方 5 格）；
 * - 防御：保持长按才存在，竖立于玩家前方约 1 格并跟随视角。
 * 除防御阵外，其余阵松开后保留 30 秒并持续作用。
 */
public class StaffItem extends LiftableItem {

    /** 充能/召唤阈值（tick，1 秒 = 20 tick）。 */
    private static final int CHARGE_THRESHOLD_TICKS = 10;

    /** 群体阵落点射程（格）。 */
    private static final double AIM_RANGE = 256.0;

    /** 每个玩家当前充能中的魔法球。物品实例全局共享 → 按玩家 UUID 记录。 */
    private static final Map<UUID, PurificationOrbEntity> CHARGING_ORBS = new HashMap<>();

    /** 每个玩家当前召唤的阵法。物品实例全局共享 → 按玩家 UUID 记录。 */
    private static final Map<UUID, PurificationFormationEntity> FORMATIONS = new HashMap<>();

    public StaffItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // 清理上一次遗留的充能球/阵法（如玩家中途死亡/断线未能正常释放）
        PurificationOrbEntity staleOrb = CHARGING_ORBS.remove(player.getUUID());
        if (staleOrb != null && staleOrb.isAlive()) {
            staleOrb.discard();
        }
        PurificationFormationEntity staleFormation = FORMATIONS.remove(player.getUUID());
        if (staleFormation != null && staleFormation.isAlive()) {
            staleFormation.discard();
        }
        PurificationFormationEntity.deactivateDefense(player.getUUID());
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return;
        }
        int heldTicks = getUseDuration(stack, entity) - remainingUseDuration;
        if (heldTicks < CHARGE_THRESHOLD_TICKS) {
            return;
        }

        int mode = StaffCastModes.get(player);
        if (mode == StaffCastModes.MODE_ORB) {
            tickChargingOrb(serverLevel, player);
        } else {
            tickFormation(serverLevel, player, mode);
        }
    }

    private void tickChargingOrb(ServerLevel level, Player player) {
        PurificationOrbEntity orb = CHARGING_ORBS.get(player.getUUID());
        if (orb == null || !orb.isAlive()) {
            orb = new PurificationOrbEntity(level, player, true);
            orb.setPos(inFrontOfStaff(player));
            level.addFreshEntity(orb);
            CHARGING_ORBS.put(player.getUUID(), orb);
        } else {
            // 充能球跟随法杖前端
            orb.setPos(inFrontOfStaff(player));
        }
    }

    private void tickFormation(ServerLevel level, Player player, int mode) {
        PurificationFormationEntity formation = FORMATIONS.get(player.getUUID());
        if (formation == null || !formation.isAlive()) {
            formation = spawnFormation(level, player, mode);
            if (formation == null) {
                return;
            }
            level.addFreshEntity(formation);
            FORMATIONS.put(player.getUUID(), formation);
            if (mode == StaffCastModes.MODE_DEFENSE) {
                PurificationFormationEntity.activateDefense(player.getUUID(), formation);
            }
        }
        // 位置跟随由阵法实体自身 tick 处理（单体治愈跟随自己 / 防御跟随视角 / 群体固定）
    }

    private PurificationFormationEntity spawnFormation(ServerLevel level, Player player, int mode) {
        PurificationFormationEntity formation = new PurificationFormationEntity(level, player,
                modeToFormationType(mode));
        switch (mode) {
            case StaffCastModes.MODE_SINGLE_HEAL -> {
                // 单体治愈：对自己使用，跟随自己脚下；同净化水晶法阵大小
                formation.setPos(player.getX(), player.getY() + 0.02, player.getZ());
                formation.setSize(2.0F);
            }
            case StaffCastModes.MODE_GROUP_HEAL, StaffCastModes.MODE_GROUP_DAMAGE -> {
                Vec3 pos = pickGroupPosition(level, player);
                formation.setPos(pos.x, pos.y + 0.02, pos.z);
                formation.setSize(6.0F); // 群体：半径 3 格 → 6x6
            }
            case StaffCastModes.MODE_DEFENSE -> {
                Vec3 pos = player.getEyePosition().add(player.getLookAngle().scale(
                        PurificationFormationEntity.DEFENSE_DISTANCE));
                formation.setPos(pos);
                formation.setYRot(player.getYRot());
                formation.setXRot(player.getXRot());
                formation.setSize(defenseSize(player));
            }
            default -> {
            }
        }
        return formation;
    }

    /** 防御阵尺寸：默认身高（scale=1）对应直径 3 格，随身高缩放。 */
    private static float defenseSize(Player player) {
        return 3.0F * (float) player.getAttributeValue(Attributes.SCALE);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return false;
        }
        int mode = StaffCastModes.get(player);
        if (mode == StaffCastModes.MODE_ORB) {
            releaseOrb(serverLevel, player, timeLeft);
        } else {
            releaseFormation(player, mode);
        }
        return true;
    }

    private void releaseOrb(ServerLevel level, Player player, int timeLeft) {
        int heldTicks = getUseDuration(player.getMainHandItem(), player) - timeLeft;
        PurificationOrbEntity charging = CHARGING_ORBS.remove(player.getUUID());
        if (heldTicks < CHARGE_THRESHOLD_TICKS) {
            // 单击：立即发射一枚小魔法球（50 伤害，直线）
            PurificationOrbEntity orb = new PurificationOrbEntity(level, player, false);
            orb.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            orb.fireTap(player);
            level.addFreshEntity(orb);
        } else if (charging != null && charging.isAlive()) {
            // 长按充能完成：发射充能中的魔法球（75 伤害 + 爆炸，直线）
            charging.launch(player);
        }
    }

    private void releaseFormation(Player player, int mode) {
        PurificationFormationEntity formation = FORMATIONS.remove(player.getUUID());
        if (formation == null || !formation.isAlive()) {
            return;
        }
        if (mode == StaffCastModes.MODE_DEFENSE) {
            // 防御阵必须保持长按：松开即消失
            formation.discard();
            PurificationFormationEntity.deactivateDefense(player.getUUID());
        } else {
            // 其他阵：松开后保留 30 秒并持续作用
            formation.startPersist();
        }
    }

    private static int modeToFormationType(int mode) {
        return switch (mode) {
            case StaffCastModes.MODE_SINGLE_HEAL -> PurificationFormationEntity.TYPE_SINGLE_HEAL;
            case StaffCastModes.MODE_GROUP_HEAL -> PurificationFormationEntity.TYPE_GROUP_HEAL;
            case StaffCastModes.MODE_GROUP_DAMAGE -> PurificationFormationEntity.TYPE_GROUP_DAMAGE;
            case StaffCastModes.MODE_DEFENSE -> PurificationFormationEntity.TYPE_DEFENSE;
            default -> PurificationFormationEntity.TYPE_SINGLE_HEAL;
        };
    }

    /** 群体阵落点：沿视线射线与方块的交点（无则取射线末端）。 */
    private static Vec3 pickGroupPosition(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(AIM_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() != HitResult.Type.MISS ? hit.getLocation() : end;
    }

    /** 法杖前端（玩家视线前方约 1.3 格、略低于视线）的充能球位置。 */
    private static Vec3 inFrontOfStaff(Player player) {
        return player.getEyePosition()
                .add(player.getLookAngle().scale(1.3))
                .subtract(0.0, 0.4, 0.0);
    }
}
