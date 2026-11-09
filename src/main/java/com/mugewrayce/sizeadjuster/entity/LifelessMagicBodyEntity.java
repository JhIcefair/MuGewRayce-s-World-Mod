package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.magic.mana.ManaService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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
 * 没有生命的魔法体（W08 内容层 → W13 生成/击杀数值）。
 *
 * <p>共同规则见 {@link AbstractMagicBodyEntity}：只受魔法伤害、物理投射物穿透。
 * W13 补：</p>
 * <ul>
 *   <li>生命分档 <b>50 / 80</b>：{@code createAttributes} 是类型级的，不能按次变化，
 *       所以由生成方（{@link MagicBodySpawner}）在 spawn 后调 {@link #applyHealthTier}；</li>
 *   <li>击杀：<b>20 经验</b>（{@code xpReward}；注意 {@code LivingEntity#getExperienceReward} 是
 *       {@code final}，不能用 override）+ 向所在区块<b>释放 800 魔力</b>；</li>
 *   <li>尺寸 0.5x~1.5x 仍由构造函数随机（W08 已有）。</li>
 * </ul>
 */
public class LifelessMagicBodyEntity extends AbstractMagicBodyEntity {

    /** 低档（区块魔力 ≤10000）生命值。 */
    public static final int MAX_HEALTH_LOW = 50;
    /** 高档（区块魔力 >10000）生命值。 */
    public static final int MAX_HEALTH_HIGH = 80;
    /** 击杀经验。 */
    public static final int EXPERIENCE_REWARD = 20;
    /** 击杀时向所在区块释放的魔力。 */
    public static final int KILL_MANA_RELEASE = 800;

    public LifelessMagicBodyEntity(EntityType<? extends LifelessMagicBodyEntity> type, Level level) {
        super(type, level);
        double scale = 0.5 + this.random.nextDouble();
        var scaleAttribute = this.getAttribute(Attributes.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(scale);
        }
        this.xpReward = EXPERIENCE_REWARD;
    }

    /** 基础属性仍给 20（类型级默认），实际生命由生成方按档设置为 50 / 80。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 50.0)
                .add(Attributes.SCALE, 1.0);
    }

    /** 生成后按档设置最大生命并回满。 */
    public void applyHealthTier(int maxHealth) {
        var healthAttribute = this.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(maxHealth);
        }
        this.setHealth(maxHealth);
    }

    /** 击杀：向所在区块释放 800 魔力（掉落只有 20 经验，不掉物品）。 */
    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel server) {
            ManaService.add(server, this.blockPosition(), KILL_MANA_RELEASE);
        }
        super.die(source);
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
