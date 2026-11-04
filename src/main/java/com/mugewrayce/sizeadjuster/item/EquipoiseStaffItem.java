package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.entity.EquipoiseFormationEntity;
import com.mugewrayce.sizeadjuster.entity.EquipoiseOrbEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
 * 净化法杖：按 {@link StaffCastModes} 的 7 种模式施法。
 *
 * <p>模式选择 GUI 留 W16；本任务没有模式选择入口，默认点式（{@link StaffCastModes#MODE_ORB}）。</p>
 *
 * <ul>
 *   <li><b>点式</b>：单击立即发射 50 伤害魔法球；长按充能后发射 75 伤害 + 小爆炸
 *       （命中方块 TNT 规模爆炸，受 mobGriefing 约束）。</li>
 *   <li><b>单发线式</b>：松开右键时沿视线发射 {@value EquipoiseOrbEntity#LINE_SINGLE_ORBS} 个
 *       高频点（每点 13，合计 65），形成一段“线段”；不破块。</li>
 *   <li><b>持续线式</b>：长按期间每 tick 发射一个高频点（每点 10）；视角移动时方向最多每秒更新一次；
 *       不破块、不消耗魔力值。</li>
 *   <li><b>单体治愈</b>：脚下 2 格阵，跟随自己，每 tick 刷新 3 秒治愈效果。</li>
 *   <li><b>群体治愈 / 群体伤害</b>：固定落点，边长 6 格（半径 3、上方 5）。</li>
 *   <li><b>普通防御</b>：竖立视线前 0.5 格、随视角、直径随身高；保持长按才存在。</li>
 * </ul>
 *
 * <p>除防御阵外，其余阵法松开后保留 30 秒并持续作用。</p>
 */
public class EquipoiseStaffItem extends LiftableItem {

    /** 点式充能 / 阵法召唤阈值（tick，1 秒 = 20 tick）。 */
    private static final int CHARGE_THRESHOLD_TICKS = 10;

    /** 群体阵落点射程（格）。 */
    private static final double AIM_RANGE = 256.0;

    /** 持续线式方向更新间隔（tick）：视角移动后 1 秒才更新一次方向。 */
    private static final int LINE_AIM_UPDATE_TICKS = 20;

    /** 每个玩家当前充能中的魔法球。物品实例全局共享 → 按玩家 UUID 记录。 */
    private static final Map<UUID, EquipoiseOrbEntity> CHARGING_ORBS = new HashMap<>();

    /** 每个玩家当前召唤的阵法。物品实例全局共享 → 按玩家 UUID 记录。 */
    private static final Map<UUID, EquipoiseFormationEntity> FORMATIONS = new HashMap<>();

    /** 每个玩家当前持续线式的锁定方向。 */
    private static final Map<UUID, SustainedLine> SUSTAINED_LINES = new HashMap<>();

    /** 每个玩家待发射的单发线式点（松开后逐 tick 发射，形成有限高频的“线段”）。 */
    private static final Map<UUID, SingleLineBurst> SINGLE_LINES = new HashMap<>();

    /** 注册服务端 tick：推进单发线式的逐 tick 点发射（不依赖玩家继续按住）。 */
    public static void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tickSingleLineBursts());
    }

    public EquipoiseStaffItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // 清理上一次遗留的充能球/阵法（如玩家中途死亡/断线未能正常释放）
        EquipoiseOrbEntity staleOrb = CHARGING_ORBS.remove(player.getUUID());
        if (staleOrb != null && staleOrb.isAlive()) {
            staleOrb.discard();
        }
        EquipoiseFormationEntity staleFormation = FORMATIONS.remove(player.getUUID());
        if (staleFormation != null && staleFormation.isAlive()) {
            staleFormation.discard();
        }
        EquipoiseFormationEntity.deactivateDefense(player.getUUID());
        SUSTAINED_LINES.remove(player.getUUID());
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return;
        }
        int heldTicks = getUseDuration(stack, entity) - remainingUseDuration;
        int mode = StaffCastModes.get(player);
        switch (mode) {
            case StaffCastModes.MODE_ORB -> {
                if (heldTicks >= CHARGE_THRESHOLD_TICKS) {
                    tickChargingOrb(serverLevel, player);
                }
            }
            // 单发线式：松开时统一发射，按住期间不做事
            case StaffCastModes.MODE_LINE_SINGLE -> {
            }
            case StaffCastModes.MODE_LINE_SUSTAINED -> tickSustainedLine(serverLevel, player);
            default -> {
                if (heldTicks >= CHARGE_THRESHOLD_TICKS) {
                    tickFormation(serverLevel, player, mode);
                }
            }
        }
    }

    private void tickChargingOrb(ServerLevel level, Player player) {
        EquipoiseOrbEntity orb = CHARGING_ORBS.get(player.getUUID());
        if (orb == null || !orb.isAlive()) {
            orb = new EquipoiseOrbEntity(level, player, true);
            orb.setPos(inFrontOfStaff(player));
            level.addFreshEntity(orb);
            CHARGING_ORBS.put(player.getUUID(), orb);
        } else {
            // 充能球跟随法杖前端
            orb.setPos(inFrontOfStaff(player));
        }
    }

    /**
     * 持续线式：每 tick 一个高频点（10 伤害）；方向在施法开始时锁定，
     * 视角移动后最多每秒更新一次（草案 §6.3 的“每秒一次方向更新”）。
     */
    private void tickSustainedLine(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle().normalize();
        SustainedLine line = SUSTAINED_LINES.get(player.getUUID());
        if (line == null) {
            line = new SustainedLine(look);
            SUSTAINED_LINES.put(player.getUUID(), line);
        } else if (line.direction.distanceToSqr(look) <= 1.0E-4) {
            // 视角回到锁定方向：取消“待更新”计时
            line.ticksSinceChange = -1;
        } else {
            // 从开始出现视角移动那一刻计时：1 秒内方向不变，1 秒后按当时指向发射
            if (line.ticksSinceChange < 0) {
                line.ticksSinceChange = 0;
            }
            line.ticksSinceChange++;
            if (line.ticksSinceChange >= LINE_AIM_UPDATE_TICKS) {
                line.direction = look;
                line.ticksSinceChange = -1;
            }
        }

        EquipoiseOrbEntity orb = new EquipoiseOrbEntity(level, player, false);
        orb.configureLine(EquipoiseOrbEntity.LINE_SUSTAINED_DAMAGE, false);
        orb.setPos(player.getEyePosition()
                .subtract(0.0, 0.1, 0.0)
                .add(line.direction.scale(0.6)));
        orb.fireAlong(line.direction);
        level.addFreshEntity(orb);
    }

    /**
     * 单发线式：松开时登记 {@value EquipoiseOrbEntity#LINE_SINGLE_ORBS} 个高频点，
     * 由服务端 tick 逐 tick 发射（每点 13 伤害，合计 65）。点从同一枪口沿锁定方向依次发出，
     * 形成有限高频的“线段”；不破块。
     */
    private void releaseSingleLine(ServerLevel level, Player player) {
        SINGLE_LINES.put(player.getUUID(), new SingleLineBurst(level,
                player.getLookAngle().normalize(), EquipoiseOrbEntity.LINE_SINGLE_ORBS));
    }

    /** 推进所有待发射的单发线式点；玩家离线 / 死亡则丢弃剩余点。 */
    private static void tickSingleLineBursts() {
        if (SINGLE_LINES.isEmpty()) {
            return;
        }
        var iterator = SINGLE_LINES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            SingleLineBurst burst = entry.getValue();
            Player player = burst.level.getPlayerByUUID(entry.getKey());
            if (player == null || player.isRemoved() || !player.isAlive()) {
                iterator.remove();
                continue;
            }
            EquipoiseOrbEntity orb = new EquipoiseOrbEntity(burst.level, player, false);
            orb.configureLine(EquipoiseOrbEntity.LINE_SINGLE_ORB_DAMAGE, false);
            orb.setPos(player.getEyePosition().subtract(0.0, 0.1, 0.0));
            orb.fireAlong(burst.direction);
            burst.level.addFreshEntity(orb);
            if (--burst.remaining <= 0) {
                iterator.remove();
            }
        }
    }

    private void tickFormation(ServerLevel level, Player player, int mode) {
        EquipoiseFormationEntity formation = FORMATIONS.get(player.getUUID());
        if (formation == null || !formation.isAlive()) {
            formation = spawnFormation(level, player, mode);
            if (formation == null) {
                return;
            }
            level.addFreshEntity(formation);
            FORMATIONS.put(player.getUUID(), formation);
            if (mode == StaffCastModes.MODE_DEFENSE) {
                EquipoiseFormationEntity.activateDefense(player.getUUID(), formation);
            }
        }
        // 位置跟随由阵法实体自身 tick 处理（单体治愈跟随自己 / 防御跟随视角 / 群体固定）
    }

    private EquipoiseFormationEntity spawnFormation(ServerLevel level, Player player, int mode) {
        EquipoiseFormationEntity formation = new EquipoiseFormationEntity(level, player,
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
                        EquipoiseFormationEntity.DEFENSE_DISTANCE));
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
        switch (mode) {
            case StaffCastModes.MODE_ORB -> releaseOrb(serverLevel, player, timeLeft);
            case StaffCastModes.MODE_LINE_SINGLE -> releaseSingleLine(serverLevel, player);
            case StaffCastModes.MODE_LINE_SUSTAINED -> SUSTAINED_LINES.remove(player.getUUID());
            default -> releaseFormation(player, mode);
        }
        return true;
    }

    private void releaseOrb(ServerLevel level, Player player, int timeLeft) {
        int heldTicks = getUseDuration(player.getMainHandItem(), player) - timeLeft;
        EquipoiseOrbEntity charging = CHARGING_ORBS.remove(player.getUUID());
        if (heldTicks < CHARGE_THRESHOLD_TICKS) {
            // 单击：立即发射一枚小魔法球（50 伤害，直线）
            EquipoiseOrbEntity orb = new EquipoiseOrbEntity(level, player, false);
            orb.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            orb.fireTap(player);
            level.addFreshEntity(orb);
        } else if (charging != null && charging.isAlive()) {
            // 长按充能完成：发射充能中的魔法球（75 伤害 + 爆炸，直线）
            charging.launch(player);
        }
    }

    private void releaseFormation(Player player, int mode) {
        EquipoiseFormationEntity formation = FORMATIONS.remove(player.getUUID());
        if (formation == null || !formation.isAlive()) {
            return;
        }
        if (mode == StaffCastModes.MODE_DEFENSE) {
            // 防御阵必须保持长按：松开即消失
            formation.discard();
            EquipoiseFormationEntity.deactivateDefense(player.getUUID());
        } else {
            // 其他阵：松开后保留 30 秒并持续作用
            formation.startPersist();
        }
    }

    /** 玩家离开时清理其充能魔法球、阵法与持续线式状态（W01 4.4，避免静态表残留到下一次会话）。 */
    public static void clearPlayer(UUID owner) {
        EquipoiseOrbEntity orb = CHARGING_ORBS.remove(owner);
        if (orb != null && orb.isAlive()) {
            orb.discard();
        }
        EquipoiseFormationEntity formation = FORMATIONS.remove(owner);
        if (formation != null && formation.isAlive()) {
            formation.discard();
        }
        SUSTAINED_LINES.remove(owner);
        SINGLE_LINES.remove(owner);
    }

    private static int modeToFormationType(int mode) {
        return switch (mode) {
            case StaffCastModes.MODE_SINGLE_HEAL -> EquipoiseFormationEntity.TYPE_SINGLE_HEAL;
            case StaffCastModes.MODE_GROUP_HEAL -> EquipoiseFormationEntity.TYPE_GROUP_HEAL;
            case StaffCastModes.MODE_GROUP_DAMAGE -> EquipoiseFormationEntity.TYPE_GROUP_DAMAGE;
            case StaffCastModes.MODE_DEFENSE -> EquipoiseFormationEntity.TYPE_DEFENSE;
            default -> EquipoiseFormationEntity.TYPE_SINGLE_HEAL;
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

    /** 持续线式的方向锁定状态（每玩家一份）。 */
    private static final class SustainedLine {
        private Vec3 direction;
        /** 视角开始偏离锁定方向后的 tick 数；-1 表示当前没有待更新。 */
        private int ticksSinceChange = -1;

        private SustainedLine(Vec3 direction) {
            this.direction = direction;
        }
    }

    /** 单发线式的待发射点（每玩家一份）。 */
    private static final class SingleLineBurst {
        private final ServerLevel level;
        private final Vec3 direction;
        private int remaining;

        private SingleLineBurst(ServerLevel level, Vec3 direction, int remaining) {
            this.level = level;
            this.direction = direction;
            this.remaining = remaining;
        }
    }
}
