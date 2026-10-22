package com.mugewrayce.sizeadjuster.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 净化魔法球（Purification Orb）：净化魔法体系的直线飞行投射物实体，
 * 由不同物品发射（用 variant 区分，渲染贴图与行为不同）：
 * - VARIANT_STAFF（净化法杖）：单击 50 伤害 / 长按充能 75 伤害 + 爆炸；
 *   命中方块：TNT 规模爆炸（受 mobGriefing 防爆模式约束）。
 * - VARIANT_ZERO_RESET（归零）：命中实体 2000 伤害；命中方块：打破方块
 *   （受 mobGriefing 防爆模式约束）后消失；贴图不同（ls\16.png）。
 *
 * 两种变体均为直线飞行（指哪打哪）、速度 4.5 格/tick（原版满蓄力箭 3.0 的 1.5 倍）、
 * 飞行距离超过 1024 格消失。实体尺寸 0.25（与物品掉落物一致），渲染为面向相机的平面贴图。
 */
public class PurificationOrbEntity extends AbstractStraightProjectile {

    /** 变体：净化法杖发射。 */
    public static final int VARIANT_STAFF = 0;

    /** 变体：归零发射（对应纹理 purification_orb_zero.png）。 */
    public static final int VARIANT_ZERO_RESET = 1;

    /** 飞行速度：原版满蓄力弓箭速度 3.0 的 1.5 倍。 */
    public static final float VELOCITY = 4.5F;

    /** 单击模式（法杖）：命中实体伤害。 */
    private static final float TAP_ENTITY_DAMAGE = 50.0F;

    /** 充能模式（法杖）：命中实体伤害。 */
    private static final float CHARGED_ENTITY_DAMAGE = 75.0F;

    /** 充能模式（法杖）命中实体的小规模爆炸半径（不破坏方块）。 */
    private static final float CHARGED_ENTITY_EXPLOSION_RADIUS = 2.0F;

    /** 充能模式（法杖）命中方块的爆炸半径（TNT 规模，破坏方块）。 */
    private static final float CHARGED_BLOCK_EXPLOSION_RADIUS = 4.0F;

    /** 归零变体：命中实体伤害。 */
    private static final float ZERO_RESET_ENTITY_DAMAGE = 2000.0F;

    /** 变体（同步到客户端，渲染器据此选择贴图）。 */
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(PurificationOrbEntity.class, EntityDataSerializers.INT);

    /** 发射模式（仅法杖变体使用）：1=单击（小威力），2=充能（大威力）。 */
    private int fireMode = 1;

    public PurificationOrbEntity(EntityType<? extends PurificationOrbEntity> type, Level level) {
        super(type, level);
        this.maxDistance = 1024.0;
    }

    /**
     * 便捷构造：由净化法杖创建。
     *
     * @param charging true=充能悬停（长按期间），false=直接发射
     */
    public PurificationOrbEntity(Level level, LivingEntity owner, boolean charging) {
        this(ModEntities.PURIFICATION_ORB, level);
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

    /** 长按充能完成（法杖变体）：由净化法杖调用，转为发射状态（大威力模式，直线发射）。 */
    public void launch(LivingEntity shooter) {
        this.charging = false;
        this.fireMode = 2;
        this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, VELOCITY, 0.0F);
    }

    /** 单击立即发射（法杖变体，小威力模式，直线发射）。 */
    public void fireTap(LivingEntity shooter) {
        this.charging = false;
        this.fireMode = 1;
        this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, VELOCITY, 0.0F);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (!this.level().isClientSide()) {
            Entity target = hitResult.getEntity();
            if (this.getVariant() == VARIANT_ZERO_RESET) {
                // 归零：2000 伤害
                target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), ZERO_RESET_ENTITY_DAMAGE);
            } else {
                // 法杖：按单击/充能模式
                target.hurt(this.damageSources().indirectMagic(this, this.getOwner()),
                        this.fireMode == 2 ? CHARGED_ENTITY_DAMAGE : TAP_ENTITY_DAMAGE);
                if (this.fireMode == 2) {
                    // 小规模爆炸：不破坏方块，仅冲击/伤害周围
                    this.level().explode(this,
                            hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z,
                            CHARGED_ENTITY_EXPLOSION_RADIUS, ExplosionInteraction.NONE);
                }
            }
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        if (!this.level().isClientSide()) {
            if (this.getVariant() == VARIANT_ZERO_RESET) {
                // 归零：打破方块（受 mobGriefing 防爆模式约束；硬度 -1 的方块如基岩不可破坏），然后消失
                if (this.canBreakBlocks()) {
                    BlockPos pos = hitResult.getBlockPos();
                    BlockState state = this.level().getBlockState(pos);
                    if (state.getDestroySpeed(this.level(), pos) >= 0.0F) {
                        this.level().destroyBlock(pos, true, this, 3);
                    }
                }
            } else if (this.fireMode == 2) {
                // 法杖充能：TNT 规模爆炸；开启防爆（mobGriefing=false）时改为不破坏方块的爆炸
                ExplosionInteraction interaction =
                        this.canBreakBlocks() ? ExplosionInteraction.TNT : ExplosionInteraction.NONE;
                this.level().explode(this,
                        hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z,
                        CHARGED_BLOCK_EXPLOSION_RADIUS, interaction);
            }
        }
        this.discard();
    }
}
