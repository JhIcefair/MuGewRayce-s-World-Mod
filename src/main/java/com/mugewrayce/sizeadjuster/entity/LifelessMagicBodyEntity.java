package com.mugewrayce.sizeadjuster.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 没有生命的魔法体（W08a）。
 *
 * <p>本任务只做：实体类型、玩家模型渲染、敌对 AI 骨架、只受魔法伤害、远程物理投射物穿透、
 * 每 3 分钟生成调度骨架。区块魔力概率/3000/5000 消耗/50/80 生命分档留 W13；攻击模式留 W14；
 * 掉落 20 经验与 800 魔力释放留 W13。</p>
 *
 * <p>生成时以玩家默认尺寸为基准随机 0.5x~1.5x（沿用 {@link Attributes#SCALE}）。</p>
 */
public class LifelessMagicBodyEntity extends AbstractMagicBodyEntity {

    public LifelessMagicBodyEntity(EntityType<? extends LifelessMagicBodyEntity> type, Level level) {
        super(type, level);
        double scale = 0.5 + this.random.nextDouble();
        var scaleAttribute = this.getAttribute(Attributes.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(scale);
        }
    }

    /** W13 接 50/80 生命分档；W08 先用基础值，不伪造区块魔力。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 50.0)
                .add(Attributes.SCALE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
}
