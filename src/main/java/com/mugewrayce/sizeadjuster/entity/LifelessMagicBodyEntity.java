package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.magic.cast.StaffCaster;
import com.mugewrayce.sizeadjuster.magic.exam.ExamAttack;
import com.mugewrayce.sizeadjuster.magic.mana.ManaService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

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

    /** W19：自发魔法攻击的射程（格）。 */
    private static final double CAST_RANGE = 16.0D;
    /** W19：两发魔法弹的间隔（tick，3 秒）。 */
    private static final int CAST_INTERVAL_TICKS = 60;
    /** W19：每发魔法弹的伤害。 */
    private static final float CAST_DAMAGE = 4.0F;

    /** W19：普通魔法水晶掉落率（固定 1 个，不吃抢夺）。 */
    private static final float CRYSTAL_DROP_CHANCE = 0.10F;

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

    /**
     * W19：额外掉落 1 个普通魔法水晶，概率 10%。
     *
     * <p>显式忽略 {@code hitByPlayer}：<b>抢夺不增加数量</b>；只掉 1 个，不走 loot table。</p>
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        if (this.random.nextFloat() < CRYSTAL_DROP_CHANCE) {
            this.drop(new ItemStack(MagicItems.magicCrystal), false, false);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // W19：先放魔法，再近战（16 格内每 3 秒一发点式魔法弹；贴脸时仍会挥击）
        this.goalSelector.addGoal(1, new LifelessCastGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * W19：没有生命的魔法体也会用魔法——目标在 16 格内时每 3 秒放一发点式魔法弹。
     *
     * <p>目标来自 {@link NearestAttackableTargetGoal}（不另写搜索）；只占 LOOK 旗标，
     * 与近战 / 游走互不阻塞。冷却只存在 goal 实例里，不写存档。</p>
     */
    private static final class LifelessCastGoal extends Goal {

        private final LifelessMagicBodyEntity body;
        private int cooldown;

        private LifelessCastGoal(LifelessMagicBodyEntity body) {
            this.body = body;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player target = this.playerTarget();
            if (target == null) {
                return false;
            }
            return this.body.distanceToSqr(target) <= CAST_RANGE * CAST_RANGE;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Player target = this.playerTarget();
            if (target == null) {
                return;
            }
            this.body.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.cooldown > 0) {
                this.cooldown--;
                return;
            }
            this.cooldown = CAST_INTERVAL_TICKS;
            if (this.body.level() instanceof ServerLevel serverLevel) {
                StaffCaster.castTrialAttack(serverLevel, this.body, target, ExamAttack.POINT, CAST_DAMAGE);
            }
        }

        /** 当前目标必须是存活、非创造 / 非旁观的玩家。 */
        private Player playerTarget() {
            LivingEntity target = this.body.getTarget();
            if (!(target instanceof Player player) || !player.isAlive()
                    || player.isSpectator() || player.isCreative()) {
                return null;
            }
            return player;
        }
    }
}
