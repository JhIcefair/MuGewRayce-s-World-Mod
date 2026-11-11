package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.magic.MagicDamage;
import com.mugewrayce.sizeadjuster.magic.MagicParticleOptions;
import com.mugewrayce.sizeadjuster.magic.SoundFx;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.HashSet;
import java.util.Set;

/**
 * 普通魔法杖 / 魔法书发射的魔法弹（W14b；W19 起点式改为实体球）。
 *
 * <p>点式、单发线式、持续线式与治疗线式共用：</p>
 * <ul>
 *   <li><b>点式</b>（{@link #KIND_POINT}）：不再每 tick 生成粒子，改由 {@code MagicBoltRenderer}
 *       渲染一枚面向相机的贴图球（{@code textures/entity/magic_orb.png}，按魔法颜色 tint），
 *       即“球在飞”而不是一条粒子拖尾；</li>
 *   <li><b>线式</b>（{@link #KIND_LINE}）：保留每 tick 的 {@code magic_line} 粒子拖尾。</li>
 * </ul>
 *
 * <p>蓄力（{@link #setCharging(boolean)}）：不移动、不判定命中，位置由施法方每 tick 设置；
 * 该状态<b>必须同步到客户端</b>（{@code DATA_CHARGING}），否则客户端会按普通投射物自行推进。</p>
 *
 * <p>伤害走 {@code mugewrayce-mod:magic_damage}；治疗直接 {@link LivingEntity#heal(float)}，
 * 并在 IV/V 时附带生命恢复。</p>
 */
public class MagicBoltEntity extends AbstractStraightProjectile {

    /** 粒子种类：点式（38.png）。 */
    public static final int KIND_POINT = 0;
    /** 粒子种类：线式（39.png）。 */
    public static final int KIND_LINE = 1;

    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(MagicBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(MagicBoltEntity.class, EntityDataSerializers.INT);
    /** W19：蓄力悬停状态；基类字段不同步，客户端必须跟着服务端一致。 */
    private static final EntityDataAccessor<Boolean> DATA_CHARGING =
            SynchedEntityData.defineId(MagicBoltEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage;
    private float heal;
    private boolean healMode;
    private boolean pierce;
    private boolean bypassInvulnerability;
    private boolean applyRegen;
    private int regenTicks;
    private int regenAmplifier;

    /** 穿透模式下已命中的实体，避免同一枚弹重复结算。 */
    private final Set<Integer> piercedEntityIds = new HashSet<>();

    public MagicBoltEntity(EntityType<? extends MagicBoltEntity> type, Level level) {
        super(type, level);
        this.maxDistance = 64.0;
    }

    public MagicBoltEntity(Level level, LivingEntity owner) {
        this(ModEntities.MAGIC_BOLT, level);
        this.setOwner(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLOR, 0xFFFFFFFF);
        builder.define(DATA_KIND, KIND_POINT);
        builder.define(DATA_CHARGING, false);
    }

    /** W19：设置蓄力悬停（同时写同步字段，客户端据此停住）。 */
    public void setCharging(boolean charging) {
        this.charging = charging;
        this.entityData.set(DATA_CHARGING, charging);
    }

    public boolean isCharging() {
        return this.charging;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_CHARGING.equals(key)) {
            this.charging = this.entityData.get(DATA_CHARGING);
        }
    }

    public void setColor(int color) {
        this.entityData.set(DATA_COLOR, color);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setKind(int kind) {
        this.entityData.set(DATA_KIND, kind);
    }

    public int getKind() {
        return this.entityData.get(DATA_KIND);
    }

    public void setDamage(float damage) {
        this.damage = damage;
        this.healMode = false;
    }

    public float getDamage() {
        return this.damage;
    }

    public void setHeal(float heal, int regenTicks, int regenAmplifier) {
        this.heal = heal;
        this.healMode = true;
        this.damage = 0.0F;
        this.pierce = false;
        this.bypassInvulnerability = false;
        this.applyRegen = regenTicks > 0;
        this.regenTicks = regenTicks;
        this.regenAmplifier = regenAmplifier;
    }

    public boolean isHealMode() {
        return this.healMode;
    }

    public void setPierce(boolean pierce) {
        this.pierce = pierce;
    }

    public boolean isPierce() {
        return this.pierce;
    }

    public void setBypassInvulnerability(boolean bypassInvulnerability) {
        this.bypassInvulnerability = bypassInvulnerability;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return !this.piercedEntityIds.contains(target.getId()) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (!this.level().isClientSide()) {
            Entity target = hitResult.getEntity();
            // W19：点式命中音（线式每 tick 命中，不能发声，否则变成噪音）
            if (this.getKind() == KIND_POINT) {
                SoundFx.at(this, SoundEvents.AMETHYST_CLUSTER_HIT, 0.6F, 1.4F);
            }
            if (this.healMode) {
                if (target instanceof LivingEntity living) {
                    living.heal(this.heal);
                    if (this.applyRegen) {
                        living.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                                this.regenTicks, this.regenAmplifier));
                    }
                }
                this.discard();
                return;
            }
            if (this.bypassInvulnerability && target instanceof LivingEntity living) {
                living.invulnerableTime = 0;
            }
            target.hurt(MagicDamage.of((ServerLevel) this.level(), this, this.getOwner()), this.damage);
        }
        if (this.pierce) {
            this.piercedEntityIds.add(hitResult.getEntity().getId());
        } else {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        // W19：点式命中方块音（线式同样不能发声）
        if (!this.level().isClientSide() && this.getKind() == KIND_POINT) {
            SoundFx.at(this, SoundEvents.AMETHYST_BLOCK_HIT, 0.5F, 1.3F);
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        // W19：点式改为渲染实体球（MagicBoltRenderer），只有线式保留粒子拖尾
        if (this.level().isClientSide() && this.isAlive() && this.getKind() == KIND_LINE) {
            this.level().addParticle(new MagicParticleOptions(this.getColor(), 0.8F, KIND_LINE),
                    this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }
}
