package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * icefair 特殊角色自带的自动防御魔法（服务端，仅对 icefair 角色生效）：
 *
 * 当处于该角色的玩家被来自外界的攻击伤害命中时（不止魔法伤害），
 * 优先执行自动防御魔法阵逻辑：在受伤方向、距离玩家 0.25 格处召唤一枚
 * 小的（直径 0.5 格）净化魔法防御阵（与净化法杖召唤的防御魔法阵同特性：
 * 竖立于来源方向、可抵挡伤害并反弹投射物），并抵挡该方向伤害
 * （火焰/冰冻/溺水/凋零效果等环境型伤害除外——它们不触发，见 PlayerRoles）；
 * 该阵维持 3 秒，若 3 秒内同方向/近方向再次受击 → 该阵 3 秒倒计时重置；
 * 若偏离角度较大的另一方向受击 → 另行生成一枚独立工作的魔法阵；
 * 魔法阵满 4 枚后，四枚魔法阵竖着围绕在玩家周围（每 90° 一枚成环，
 * 实现全向防御），此后来自任何方向的伤害都会被魔法阵抵挡，
 * 整体维持 3 秒（每次受击整体重置），直到 3 秒内没有再受到攻击伤害，全部消失。
 *
 * 位置跟随/倒计时/朝向由每枚魔法阵实体自身的 tick 处理
 * （见 PurificationFormationEntity.tickIcefairShield）；本类负责：
 * 伤害到来时的"抵挡/生成/重置"判定、环形布阵、以及角色离开时的清理。
 */
public final class IcefairShields {

    /** 单枚魔法阵维持时长：3 秒。 */
    public static final int SHIELD_LIFE_TICKS = 20 * 3;

    /** 魔法阵数量上限：满 4 枚转为全向围绕（竖着围绕玩家成环，每 90° 一枚）。 */
    public static final int MAX_SHIELDS = 4;

    /** 魔法阵与玩家的距离（格，0.25 格；不同于法杖防御阵的 0.5 格）。 */
    public static final double SHIELD_DISTANCE = 0.25;

    /** 魔法阵生成位置相对玩家眼睛在 Y 轴的下移量（格，向下 0.25 格）。 */
    public static final double SHIELD_Y_OFFSET = -0.25;

    /** 视作"同方向/近方向"的最大夹角（度）。 */
    private static final int COVER_HALF_ANGLE_DEGREES = 60;

    /** 玩家 UUID → 其当前的自动防御魔法阵列表。 */
    private static final Map<UUID, List<PurificationFormationEntity>> SHIELDS = new HashMap<>();

    private IcefairShields() {
    }

    /**
     * 处理一次作用于 icefair 玩家的、来自外界的攻击伤害（触发自动防御魔法阵）。
     * 仅当命中来源是"攻击"（近战/投射物等，DamageSource 带有直接攻击实体）时调用，
     * 环境型伤害（火焰/冰冻/溺水/凋零效果/摔落等）不在此列（由伤害拦截层面处理）。
     *
     * 当受到这类攻击时，优先执行自动防御魔法阵逻辑：在伤害来源方向召唤一枚
     * 小的净化魔法防御阵（直径 1 格，与法杖防御阵同特性），并抵挡该方向伤害。
     *
     * @return false=本次伤害被自动防御魔法阵抵挡（不生效）；true=伤害照常生效
     */
    public static boolean onAttackDamage(ServerLevel level, Player player, DamageSource source) {
        UUID uuid = player.getUUID();
        Entity attacker = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
        if (attacker == player) {
            return true; // 自己造成的伤害不触发（防止自触发循环）
        }
        Vec3 from = attacker != null ? attacker.position() : source.getSourcePosition();
        if (from == null) {
            return true; // 无法确定伤害来源方向，不生成、不抵挡
        }
        Vec3 toSource = from.subtract(player.position());
        if (toSource.horizontalDistance() < 0.01) {
            return true; // 无水平方向（正上/正下等），不生成、不抵挡
        }
        int attackYaw = yawFrom(toSource);

        List<PurificationFormationEntity> shields = active(uuid);

        if (shields.size() >= MAX_SHIELDS) {
            // 环形状态：任何方向伤害都会被魔法阵抵挡，整体维持 3 秒（受击即整体重置）
            for (PurificationFormationEntity shield : shields) {
                shield.refreshIcefairLife();
            }
            return false;
        }

        // 同方向/近方向已有魔法阵 → 由它抵挡本次伤害并重置其 3 秒倒计时
        for (PurificationFormationEntity shield : shields) {
            if (angleDistance(shield.getShieldDirYaw(), attackYaw) <= COVER_HALF_ANGLE_DEGREES) {
                shield.refreshIcefairLife();
                return false;
            }
        }

        // 新的明显不同方向：优先执行魔法阵逻辑——召唤新的独立魔法阵并抵挡本次伤害
        PurificationFormationEntity shield = spawnShield(level, player, attackYaw);
        shields.add(shield);
        if (shields.size() >= MAX_SHIELDS) {
            // 满 4 枚 → 竖着围绕在玩家周围成环（每 90° 一枚，全向防御）
            arrangeRing(shields);
        }
        return false;
    }

    /** 在玩家朝向伤害来源的方向、距离玩家 0.25 格处生成一枚直径 0.5 格的防御魔法阵（与法杖防御阵同特性）。 */
    private static PurificationFormationEntity spawnShield(ServerLevel level, Player player, int yaw) {
        PurificationFormationEntity shield = new PurificationFormationEntity(
                level, player, PurificationFormationEntity.TYPE_ICEFAIR_SHIELD);
        shield.initIcefairShield(yaw);
        shield.setSize(0.5F); // 直径 0.5 格
        shield.setPos(player.getEyePosition().add(
                yawVector(yaw).scale(SHIELD_DISTANCE)).add(0.0, SHIELD_Y_OFFSET, 0.0)); // 距离玩家 0.25 格、Y 轴下移 0.5 格
        shield.setYRot(yaw);
        shield.setXRot(0.0F);
        level.addFreshEntity(shield);
        return shield;
    }

    /** 满 4 枚：按每 90° 均匀分配，四枚竖着围绕玩家成环（全向防御）。 */
    private static void arrangeRing(List<PurificationFormationEntity> shields) {
        for (int i = 0; i < shields.size(); i++) {
            int yaw = i * (360 / MAX_SHIELDS);
            shields.get(i).initIcefairShield(yaw);
            shields.get(i).setIcefairRingSlot(i);
        }
    }

    /** 取玩家当前存活的自动防御魔法阵列表（惰性清理已消失的实体）。 */
    private static List<PurificationFormationEntity> active(UUID uuid) {
        List<PurificationFormationEntity> shields = SHIELDS.get(uuid);
        if (shields == null) {
            shields = new ArrayList<>();
            SHIELDS.put(uuid, shields);
            return shields;
        }
        shields.removeIf(shield -> !shield.isAlive());
        if (shields.isEmpty()) {
            SHIELDS.remove(uuid);
            shields = new ArrayList<>();
            SHIELDS.put(uuid, shields);
        }
        return shields;
    }

    /** 清理玩家的全部自动防御魔法阵（如切换角色离开 icefair 时）。 */
    public static void clearShields(UUID uuid) {
        List<PurificationFormationEntity> shields = SHIELDS.remove(uuid);
        if (shields != null) {
            for (PurificationFormationEntity shield : shields) {
                if (shield.isAlive()) {
                    shield.discard();
                }
            }
        }
    }

    /**
     * 某枚自动防御魔法阵抵挡了一次攻击（如反弹了投射物）时调用：
     * 同方向/近方向再次受击 → 重置该阵 3 秒倒计时；
     * 环形状态 → 整体重置（任何方向的攻击都被环形魔法阵抵挡）。
     */
    public static void onShieldBlocked(UUID ownerUuid, PurificationFormationEntity shield) {
        List<PurificationFormationEntity> shields = SHIELDS.get(ownerUuid);
        if (shields == null) {
            return;
        }
        shields.removeIf(s -> !s.isAlive());
        if (shields.isEmpty()) {
            SHIELDS.remove(ownerUuid);
            return;
        }
        if (shields.size() >= MAX_SHIELDS) {
            // 环形：整体维持 3 秒（受击即整体重置）
            for (PurificationFormationEntity s : shields) {
                s.refreshIcefairLife();
            }
        } else {
            shield.refreshIcefairLife();
        }
    }

    /** 由（水平）向量求原版 yaw（度，-180~180）。 */
    public static int yawFrom(Vec3 vector) {
        return (int) Math.round(Math.toDegrees(Math.atan2(-vector.x, vector.z)));
    }

    /** 原版 yaw（度）→ 水平单位向量（x = -sin, z = cos）。 */
    public static Vec3 yawVector(int yawDegrees) {
        double rad = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
    }

    /** 两个 yaw 角之差（0~180 度）。 */
    private static int angleDistance(int a, int b) {
        int d = Math.abs(a - b) % 360;
        return d > 180 ? 360 - d : d;
    }
}
