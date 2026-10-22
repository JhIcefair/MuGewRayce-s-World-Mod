package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.item.PurificationEffects;
import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 净化魔法阵（法杖施法模式，贴图同净化水晶法阵 purification_circle.png）。
 *
 * 类型（formationType，同步到客户端供渲染）：
 * - SINGLE_HEAL（单体治愈·对自己）：跟随施法者本人脚下；持续对施法者治愈。
 * - GROUP_HEAL（群体治愈）：固定于施法位置；半径 3 格、向上 5 格内实体持续治愈。
 * - GROUP_DAMAGE（群体伤害）：固定于施法位置；每秒对范围内实体 3~10 随机伤害
 *   （不能对施法者自己造成攻击伤害）。
 * - DEFENSE（防御）：竖立于玩家前方约 1 格（跟随视角 360° 旋转、平面与视线垂直），
 *   保持长按才存在，抵挡来自前方的伤害；尺寸默认身高对应直径 2 格（随身高缩放）。
 *
 * 除防御阵需保持长按外，其余阵长按召唤、松开后保留 30 秒并持续作用
 * （单体治愈在此期间继续跟随施法者）。
 */
public class PurificationFormationEntity extends Entity {

    public static final int TYPE_SINGLE_HEAL = 1;
    public static final int TYPE_GROUP_HEAL = 2;
    public static final int TYPE_GROUP_DAMAGE = 3;
    public static final int TYPE_DEFENSE = 4;

    /** icefair 自动防御魔法阵（特殊角色 icefair 自带；直径 1 格，见 IcefairShields）。 */
    public static final int TYPE_ICEFAIR_SHIELD = 5;

    /** 非防御阵松开后的保留时长：30 秒。 */
    private static final int PERSIST_TICKS = 20 * 30;

    /** 群体阵半径（格）。 */
    public static final double GROUP_RADIUS = 3.0;

    /** 群体阵向上作用高度（格）。 */
    public static final double GROUP_HEIGHT = 5.0;

    /** 防御阵与玩家的距离（格，约 0.5 格）。 */
    public static final double DEFENSE_DISTANCE = 0.5;

    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(PurificationFormationEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(PurificationFormationEntity.class, EntityDataSerializers.FLOAT);

    /** 当前生效的防御阵（玩家 UUID → 阵），供伤害拦截查询。 */
    private static final Map<UUID, PurificationFormationEntity> ACTIVE_DEFENSE = new HashMap<>();

    private UUID ownerUuid;
    private boolean persisting;
    private int persistTicksLeft;
    private int damageTickCounter;

    // ---- icefair 自动防御魔法阵（TYPE_ICEFAIR_SHIELD）服务端字段（不同步） ----
    /** 该阵朝向（世界水平 yaw，度；指向伤害来源方向）。 */
    private int shieldDirYaw;
    /** 环形槽位（0~5，-1=非环形即独立朝向）。 */
    private int shieldRingSlot = -1;
    /** 剩余维持 tick（3 秒，受击重置）。 */
    private int shieldLife;

    public PurificationFormationEntity(EntityType<? extends PurificationFormationEntity> type, Level level) {
        super(type, level);
    }

    public PurificationFormationEntity(Level level, Player owner, int formationType) {
        this(ModEntities.PURIFICATION_FORMATION, level);
        this.ownerUuid = owner.getUUID();
        this.entityData.set(DATA_TYPE, formationType);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TYPE, TYPE_SINGLE_HEAL);
        builder.define(DATA_SIZE, 2.0F);
    }

    public int getFormationType() {
        return this.entityData.get(DATA_TYPE);
    }

    /** 阵法边长（渲染用，服务端按类型/身高设置并同步）。 */
    public void setSize(float size) {
        this.entityData.set(DATA_SIZE, size);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    /** 松开后保留 30 秒并持续作用。 */
    public void startPersist() {
        this.persisting = true;
        this.persistTicksLeft = PERSIST_TICKS;
    }

    /** 初始化为 icefair 自动防御魔法阵：设定朝向（指向伤害来源方向）并重置 3 秒倒计时。 */
    public void initIcefairShield(int dirYaw) {
        this.shieldDirYaw = dirYaw;
        this.shieldRingSlot = -1;
        this.shieldLife = IcefairShields.SHIELD_LIFE_TICKS;
    }

    /** 分配环形槽位（icefair 满 6 枚围绕成环时调用），同时重置倒计时。 */
    public void setIcefairRingSlot(int slot) {
        this.shieldRingSlot = slot;
        this.shieldLife = IcefairShields.SHIELD_LIFE_TICKS;
    }

    /** 重置 3 秒维持倒计时（同方向/近方向再次受击、或环形整体受击时）。 */
    public void refreshIcefairLife() {
        this.shieldLife = IcefairShields.SHIELD_LIFE_TICKS;
    }

    /** 当前朝向（yaw，度）。 */
    public int getShieldDirYaw() {
        return this.shieldDirYaw;
    }

    /** 是否处于环形状态（icefair 满 6 枚）。 */
    public boolean isIcefairRingSlot() {
        return this.shieldRingSlot >= 0;
    }

    public static void activateDefense(UUID owner, PurificationFormationEntity formation) {
        ACTIVE_DEFENSE.put(owner, formation);
    }

    public static void deactivateDefense(UUID owner) {
        ACTIVE_DEFENSE.remove(owner);
    }

    /** 防御阵是否正在为该玩家生效（伤害拦截用）。 */
    public static boolean isDefenseActive(UUID owner) {
        PurificationFormationEntity formation = ACTIVE_DEFENSE.get(owner);
        return formation != null && formation.isAlive();
    }

    /** 仅防御阵 / icefair 自动防御阵可被投射物命中（像盾牌一样挡在前方/来源方向）。 */
    @Override
    public boolean canBeHitByProjectile() {
        int type = this.getFormationType();
        return type == TYPE_DEFENSE || type == TYPE_ICEFAIR_SHIELD;
    }

    /** 防御阵 / icefair 自动防御阵反弹投射物（原版盾牌式反弹：反转方向 + 随机转角）；
     *  icefair 自动防御阵每次抵挡（反弹）视为一次受击，重置其 3 秒倒计时（环形则整体重置）。 */
    @Override
    public ProjectileDeflection deflection(Projectile projectile) {
        int type = this.getFormationType();
        if (type == TYPE_DEFENSE) {
            return ProjectileDeflection.AIM_DEFLECT;
        }
        if (type == TYPE_ICEFAIR_SHIELD) {
            if (!this.level().isClientSide()) {
                IcefairShields.onShieldBlocked(this.ownerUuid, this);
            }
            return ProjectileDeflection.AIM_DEFLECT;
        }
        return ProjectileDeflection.NONE;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        Player owner = this.level().getPlayerByUUID(this.ownerUuid);
        if (owner == null || owner.isRemoved() || !owner.isAlive()) {
            this.discard();
            return;
        }

        int type = this.getFormationType();

        if (type == TYPE_ICEFAIR_SHIELD) {
            // icefair 自动防御魔法阵：独立逻辑（不依赖长按/持久期）
            this.tickIcefairShield(owner);
            return;
        }

        if (this.persisting) {
            if (--this.persistTicksLeft <= 0) {
                this.discard();
                return;
            }
        } else if (!owner.isUsingItem()) {
            // 未进入持久期且已松开（兜底清理）
            this.discard();
            return;
        }

        // 位置跟随
        if (type == TYPE_SINGLE_HEAL) {
            // 单体治愈：跟随施法者本人脚下
            this.setPos(owner.getX(), owner.getY() + 0.02, owner.getZ());
        } else if (type == TYPE_DEFENSE) {
            // 防御阵：玩家视线前方约 0.5 格，随视角上下左右自由转动（平面与视线垂直）
            this.setPos(owner.getEyePosition().add(owner.getLookAngle().scale(DEFENSE_DISTANCE)));
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.setSize(3.0F * (float) owner.getAttributeValue(Attributes.SCALE)); // 默认身高直径 3 格
        }

        // 效果
        if (type == TYPE_SINGLE_HEAL || type == TYPE_GROUP_HEAL) {
            this.applyHeal(owner);
        } else if (type == TYPE_GROUP_DAMAGE) {
            if (++this.damageTickCounter % 20 == 0) {
                this.applyDamage(owner);
            }
        }
    }

    /**
     * icefair 自动防御魔法阵每 tick（服务端）：
     * 倒计时递减（3 秒内未再受击即消失）；角色离开 icefair 即消失；
     * 位置跟随玩家眼睛水平前方（独立朝向）或围绕成环（环形槽位）。
     */
    private void tickIcefairShield(Player owner) {
        if (PlayerRoles.getRole(owner) != PlayerRoles.specialCode(0)) {
            this.discard(); // 已不是 icefair 角色
            return;
        }
        if (--this.shieldLife <= 0) {
            this.discard(); // 3 秒内没有再受到攻击伤害 → 消失
            return;
        }
        int yaw = this.shieldRingSlot >= 0
                ? this.shieldRingSlot * (360 / IcefairShields.MAX_SHIELDS)
                : this.shieldDirYaw;
        this.setPos(owner.getEyePosition().add(IcefairShields.yawVector(yaw).scale(IcefairShields.SHIELD_DISTANCE))
                .add(0.0, IcefairShields.SHIELD_Y_OFFSET, 0.0)); // 距离玩家 0.25 格、Y 轴下移 0.5 格
        this.setYRot(yaw);
        this.setXRot(0.0F);
        this.setSize(0.5F); // 直径 0.5 格
    }

    private void applyHeal(Player owner) {
        if (this.getFormationType() == TYPE_SINGLE_HEAL) {
            PurificationEffects.apply(owner, PurificationEffects.FORMATION_DURATION_TICKS);
        } else {
            for (LivingEntity entity : this.entitiesInRange()) {
                PurificationEffects.apply(entity, PurificationEffects.FORMATION_DURATION_TICKS);
            }
        }
    }

    private void applyDamage(Player owner) {
        float damage = 15.0F + this.random.nextInt(36); // 每秒 15~50 随机伤害
        for (LivingEntity entity : this.entitiesInRange()) {
            if (entity == owner) {
                continue; // 群体伤害不能对施法者自己造成攻击伤害
            }
            entity.hurt(this.damageSources().indirectMagic(this, owner), damage);
        }
    }

    /** 群体阵作用范围：半径 3 格水平、阵上方 5 格内的实体。 */
    private List<LivingEntity> entitiesInRange() {
        return this.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(this.getX() - GROUP_RADIUS, this.getY() - 0.5, this.getZ() - GROUP_RADIUS,
                        this.getX() + GROUP_RADIUS, this.getY() + GROUP_HEIGHT, this.getZ() + GROUP_RADIUS),
                LivingEntity::isAlive);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // 瞬态实体（noSave），不保存数据
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        // 瞬态实体（noSave），不保存数据
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }
}
