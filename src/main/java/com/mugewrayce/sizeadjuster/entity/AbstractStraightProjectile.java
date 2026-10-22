package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 直线飞行投射物基类（净化魔法球 / 净化箭矢共用）：
 * - 无重力、无空气阻力，严格沿初速度方向直线飞行（指哪打哪）；
 * - 每 tick 移动并做命中判定；累计飞行距离超过 maxDistance 后直接消失；
 * - 提供充能悬停状态（charging，长按充能时用）：不移动、不判定命中，位置由外部每 tick 设置；
 * - 提供 mobGriefing 防爆判断，子类的方块破坏效果应受其约束。
 */
public abstract class AbstractStraightProjectile extends Projectile {

    /** 最大飞行距离（格），超出直接消失。 */
    protected double maxDistance = 1024.0;

    /** 充能悬停状态：不移动、不判定命中，位置由外部每 tick 设置（服务端）。 */
    protected boolean charging;

    /** 已飞行距离（格），用于 maxDistance 消失判定。 */
    private double travelledDistance;

    protected AbstractStraightProjectile(EntityType<? extends AbstractStraightProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // 无需要同步的数据
    }

    /**
     * 本模组魔法球/归零发射的是"魔法伤害"投射物，应能命中魔法体（含 icefair）玩家；
     * 但 26.1 默认 canHitEntity 会先查 target.canBeHitByProjectile()（对魔法体玩家为 false，
     * 用于让"物理投射物直接穿过"），故在此对魔法体玩家额外放开（仍排除骑乘同载具者/自身）。
     */
    @Override
    protected boolean canHitEntity(Entity target) {
        if (super.canHitEntity(target)) {
            return true;
        }
        return target instanceof Player player
                && !player.level().isClientSide()
                && target != this.getOwner()
                && PlayerRoles.isMagicBody(PlayerRoles.getRole(player))
                && !target.isSpectator()
                && target.isAlive();
    }

    @Override
    public void tick() {
        if (this.charging) {
            // 充能悬停：不移动、不判定命中；位置由外部每 tick 设置（服务端），客户端按同步位置渲染
            if (!this.level().isClientSide()) {
                Entity owner = this.getOwner();
                if (owner == null || owner.isRemoved() || !owner.isAlive()) {
                    this.discard();
                }
            }
            super.tick();
            return;
        }

        Vec3 delta = this.getDeltaMovement();
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        Vec3 target = hitResult.getType() != HitResult.Type.MISS
                ? hitResult.getLocation()
                : this.position().add(delta);
        this.setPos(target);
        this.updateRotation();
        super.tick();

        if (hitResult.getType() != HitResult.Type.MISS && this.isAlive()) {
            this.hitTargetOrDeflectSelf(hitResult);
        }

        this.travelledDistance += delta.length();
        if (this.travelledDistance >= this.maxDistance && this.isAlive()) {
            this.discard();
        }
    }

    /**
     * 防爆检查：原版 gamerule mobGriefing 关闭时（即开启防爆模式），
     * 禁止本投射物破坏方块（爆炸也不得破坏方块）。
     */
    protected boolean canBreakBlocks() {
        if (this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getGameRules().get(GameRules.MOB_GRIEFING);
        }
        return false;
    }
}
