package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.magic.MagicDamage;
import com.mugewrayce.sizeadjuster.magic.MagicParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
 * 普通魔法杖 / 魔法书发射的无贴图魔法弹（W14b）。
 *
 * <p>点式、单发线式、持续线式与治疗线式共用；外观由 W14c 的
 * {@code magic_point} / {@code magic_line} 粒子在客户端拖尾表现，本身不渲染
 * （{@code MagicBoltRenderer} 为空实现）。</p>
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
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.isAlive()) {
            float scale = this.getKind() == KIND_LINE ? 0.8F : 1.0F;
            this.level().addParticle(new MagicParticleOptions(this.getColor(), scale, this.getKind()),
                    this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }
}
