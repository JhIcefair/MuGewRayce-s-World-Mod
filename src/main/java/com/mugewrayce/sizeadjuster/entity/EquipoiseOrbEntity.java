package com.mugewrayce.sizeadjuster.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * 净化魔法球（Equipoise Orb）：净化系的直线飞行投射物实体。
 *
 * <p>同一个实体 ID，用 <b>变体</b> 只区分渲染贴图；伤害与命中行为由发射时配置：</p>
 * <ul>
 *   <li>法杖点式：贴图 ls\15.png，50 伤害；</li>
 *   <li>法杖充能：贴图 ls\15.png，75 伤害 + 小爆炸；命中方块 TNT 规模爆炸（受 mobGriefing 约束）；</li>
 *   <li>归零：贴图 ls\16.png，2000 伤害；命中方块破块后消失（受 mobGriefing 约束，硬度 -1 不可破坏）；</li>
 *   <li>法杖线式：贴图 ls\16.png；单发线式由若干高频点组成（每点 13，合计 65），
 *       持续线式每 tick 一点（每点 10）；线式不破块、不爆炸，命中方块即消失。</li>
 * </ul>
 *
 * <p>全部变体均为直线飞行（指哪打哪）、速度 4.5 格/tick（原版满蓄力箭 3.0 的 1.5 倍）、
 * 飞行距离超过 1024 格消失。实体尺寸 0.25（与物品掉落物一致），渲染为面向相机的平面贴图。</p>
 */
public class EquipoiseOrbEntity extends AbstractStraightProjectile {

    /** 变体：净化法杖发射（贴图 equipoise_orb.png，ls\15.png）。 */
    public static final int VARIANT_STAFF = 0;

    /** 变体：归零 / 线式发射（贴图 equipoise_orb_zero.png，ls\16.png）。 */
    public static final int VARIANT_ZERO_RESET = 1;

    /** 飞行速度：原版满蓄力弓箭速度 3.0 的 1.5 倍。 */
    public static final float VELOCITY = 4.5F;

    /** 点式（法杖）：命中实体伤害。 */
    public static final float TAP_ENTITY_DAMAGE = 50.0F;

    /** 充能（法杖）：命中实体伤害。 */
    public static final float CHARGED_ENTITY_DAMAGE = 75.0F;

    /** 单发线式总伤害（由 {@link #LINE_SINGLE_ORBS} 个高频点分摊）。 */
    public static final float LINE_SINGLE_DAMAGE = 65.0F;

    /** 单发线式的点数（有限高频的点式，形成“线段”）。 */
    public static final int LINE_SINGLE_ORBS = 5;

    /** 单发线式每个点的伤害：65 / 5 = 13。 */
    public static final float LINE_SINGLE_ORB_DAMAGE = LINE_SINGLE_DAMAGE / LINE_SINGLE_ORBS;

    /** 持续线式每 tick 每点伤害。 */
    public static final float LINE_SUSTAINED_DAMAGE = 10.0F;

    /** 充能模式（法杖）命中实体的小规模爆炸半径（不破坏方块）。 */
    private static final float CHARGED_ENTITY_EXPLOSION_RADIUS = 2.0F;

    /** 充能模式（法杖）命中方块的爆炸半径（TNT 规模，破坏方块）。 */
    private static final float CHARGED_BLOCK_EXPLOSION_RADIUS = 4.0F;

    /** 归零变体：命中实体伤害。 */
    private static final float ZERO_RESET_ENTITY_DAMAGE = 2000.0F;

    /** 变体（同步到客户端，渲染器据此选择贴图）。 */
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(EquipoiseOrbEntity.class, EntityDataSerializers.INT);

    /** 命中实体伤害（发射前配置；服务端结算，不需同步）。 */
    private float damage = TAP_ENTITY_DAMAGE;

    /** 是否穿透实体（线式用）：同一实体只结算一次，命中后继续飞行。 */
    private boolean pierce;

    /** 命中方块是否破块（仅归零变体）。 */
    private boolean breakBlocks;

    /** 命中实体是否触发小爆炸（仅法杖充能）。 */
    private boolean chargedExplosion;

    /** 线式伤害是否忽略受击无敌帧（持续线式要求每 tick 结算 10 点）。 */
    private boolean bypassInvulnerability;

    /** 已结算过的实体 ID（穿透时防止同一实体被同一枚魔法球重复命中）。 */
    private final Set<Integer> piercedEntityIds = new HashSet<>();

    public EquipoiseOrbEntity(EntityType<? extends EquipoiseOrbEntity> type, Level level) {
        super(type, level);
        this.maxDistance = 1024.0;
    }

    /**
     * 便捷构造：由净化法杖创建。
     *
     * @param charging true=充能悬停（长按期间），false=直接发射
     */
    public EquipoiseOrbEntity(Level level, LivingEntity owner, boolean charging) {
        this(ModEntities.EQUIPOISE_ORB, level);
        this.setOwner(owner);
        this.charging = charging;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VARIANT, VARIANT_STAFF);
    }

    /** 设置变体（发射前调用，随生成包同步到客户端）。 */
    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, variant);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public float getDamage() {
        return this.damage;
    }

    public boolean isPierce() {
        return this.pierce;
    }

    /** 配置为法杖点式（50 伤害，贴图 ls\15.png）。 */
    public void configureStaffTap() {
        this.setVariant(VARIANT_STAFF);
        this.damage = TAP_ENTITY_DAMAGE;
        this.pierce = false;
        this.breakBlocks = false;
        this.chargedExplosion = false;
        this.bypassInvulnerability = false;
    }

    /** 配置为法杖充能（75 伤害 + 小爆炸，贴图 ls\15.png）。 */
    public void configureStaffCharged() {
        this.setVariant(VARIANT_STAFF);
        this.damage = CHARGED_ENTITY_DAMAGE;
        this.pierce = false;
        this.breakBlocks = false;
        this.chargedExplosion = true;
        this.bypassInvulnerability = false;
    }

    /** 配置为归零（2000 伤害 + 破块，贴图 ls\16.png）。 */
    public void configureZeroReset() {
        this.setVariant(VARIANT_ZERO_RESET);
        this.damage = ZERO_RESET_ENTITY_DAMAGE;
        this.pierce = false;
        this.breakBlocks = true;
        this.chargedExplosion = false;
        this.bypassInvulnerability = false;
    }

    /**
     * 配置为线式（贴图 ls\16.png）。
     *
     * @param damage 单点伤害
     * @param pierce 是否穿透实体
     */
    public void configureLine(float damage, boolean pierce) {
        this.setVariant(VARIANT_ZERO_RESET);
        this.damage = damage;
        this.pierce = pierce;
        this.breakBlocks = false;
        this.chargedExplosion = false;
        this.bypassInvulnerability = true;
    }

    /** 长按充能完成（法杖）：转为发射状态（大威力模式，直线发射）。 */
    public void launch(LivingEntity shooter) {
        this.charging = false;
        this.configureStaffCharged();
        this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, VELOCITY, 0.0F);
    }

    /** 单击立即发射（法杖，小威力模式，直线发射）。 */
    public void fireTap(LivingEntity shooter) {
        this.charging = false;
        this.configureStaffTap();
        this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, VELOCITY, 0.0F);
    }

    /** 按给定方向发射（线式用；方向在施法期间可被锁定）。 */
    public void fireAlong(Vec3 direction) {
        this.charging = false;
        this.shoot(direction.x, direction.y, direction.z, VELOCITY, 0.0F);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        // 穿透模式下，同一枚魔法球不重复命中同一实体
        return !this.piercedEntityIds.contains(target.getId()) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (!this.level().isClientSide()) {
            Entity target = hitResult.getEntity();
            // 线式要求“每 tick 10 点”/“单发合计 65 点”，需绕过受击无敌帧，否则后续点会被吞掉
            if (this.bypassInvulnerability && target instanceof LivingEntity living) {
                living.invulnerableTime = 0;
            }
            target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.damage);
            if (this.chargedExplosion) {
                // 小规模爆炸：不破坏方块，仅冲击/伤害周围
                this.level().explode(this,
                        hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z,
                        CHARGED_ENTITY_EXPLOSION_RADIUS, ExplosionInteraction.NONE);
            }
        }
        if (this.pierce) {
            this.piercedEntityIds.add(hitResult.getEntity().getId());
        } else {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        if (!this.level().isClientSide()) {
            if (this.breakBlocks) {
                // 归零：打破方块（受 mobGriefing 防爆模式约束；硬度 -1 的方块如基岩不可破坏），然后消失
                if (this.canBreakBlocks()) {
                    BlockPos pos = hitResult.getBlockPos();
                    BlockState state = this.level().getBlockState(pos);
                    if (state.getDestroySpeed(this.level(), pos) >= 0.0F) {
                        this.level().destroyBlock(pos, true, this, 3);
                    }
                }
            } else if (this.chargedExplosion) {
                // 法杖充能：TNT 规模爆炸；开启防爆（mobGriefing=false）时改为不破坏方块的爆炸
                ExplosionInteraction interaction =
                        this.canBreakBlocks() ? ExplosionInteraction.TNT : ExplosionInteraction.NONE;
                this.level().explode(this,
                        hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z,
                        CHARGED_BLOCK_EXPLOSION_RADIUS, interaction);
            }
        }
        // 线式/点式：命中方块即消失（不插在地上）
        this.discard();
    }
}
