package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.magic.cast.StaffCaster;
import com.mugewrayce.sizeadjuster.magic.exam.ExamAttack;
import com.mugewrayce.sizeadjuster.magic.exam.ExamSpec;
import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * 试炼魔法体（W15 完整配置；W08 只做了骨架）。
 *
 * <p>与 {@link LifelessMagicBodyEntity} 共用皮肤与渲染器。考试 / 试炼用它承载
 * 草案 §5.2 的几种配置：无 AI / 仇恨发起者 / 无敌 / 20·30·50·100 血 /
 * 仇恨固定在猪身上 / 只按指定方式攻击（{@link ExamAttack}）。</p>
 *
 * <p>归属：{@code ownerUuid}（发起者）+ {@code sessionId}（会话），用于多人重叠时
 * 区分“谁的那批目标”（草案 §9.5）。这两个字段会随实体存档，重启后仍有效（D-12）。</p>
 */
public class TrialMagicBodyEntity extends AbstractMagicBodyEntity {

    private static final String[] EMPTY = new String[0];

    /** W19：召唤物冷静期（tick，3 秒）：不动、不攻击、免疫一切伤害。 */
    public static final int GRACE_TICKS = 60;

    /** W19：普通魔法水晶掉落率（考试 / 试炼体 30%，固定 1 个，不吃抢夺）。 */
    private static final float CRYSTAL_DROP_CHANCE = 0.30F;

    private UUID ownerUuid;
    private String sessionId = "";
    private final EnumSet<ExamAttack> allowedAttacks = EnumSet.noneOf(ExamAttack.class);
    private final List<UUID> pigTargets = new ArrayList<>();
    private boolean examAi;
    private boolean attackPlayers;
    private float configuredMaxHealth = 20.0F;
    private boolean goalsInstalled;
    /** W19：冷静期剩余 tick（运行期，不存档；读档一律按 0 处理）。 */
    private int graceTicks;
    /** W19：配置里的无敌（冷静期结束后恢复成这个值）。 */
    private boolean configuredInvulnerable;

    public TrialMagicBodyEntity(EntityType<? extends TrialMagicBodyEntity> type, Level level) {
        super(type, level);
        // W08 的骨架默认“站着不动 + 无敌”；有 AI / 可击杀的变体在 applyExamConfig 里改回来
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.SCALE, 1.0);
    }

    @Override
    protected void registerGoals() {
        // 目标由 applyExamConfig 在构造后动态安装（构造时还不知道配置）
    }

    /**
     * 按考试配置初始化（服务端生成时调用一次）。
     *
     * @param spec          内容表配置
     * @param owner         发起者 UUID
     * @param sessionId     所属会话
     * @param attackPlayer  true = 仇恨发起者；false = 仇恨 {@code pigTargets}
     * @param pigTargets    固定仇恨的猪（治疗类试炼）
     */
    public void applyExamConfig(ExamSpec spec, UUID owner, String sessionId,
                                boolean attackPlayer, List<UUID> pigTargets) {
        this.ownerUuid = owner;
        this.sessionId = sessionId == null ? "" : sessionId;
        this.attackPlayers = attackPlayer;
        this.pigTargets.clear();
        if (pigTargets != null) {
            this.pigTargets.addAll(pigTargets);
        }
        this.allowedAttacks.clear();
        this.allowedAttacks.addAll(spec.allowedAttacks());
        this.configuredMaxHealth = Math.max(1.0F, spec.health());
        // 先设上限再设当前值（坑 2：顺序反了会被夹到旧上限）
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.configuredMaxHealth);
        this.setHealth(this.configuredMaxHealth);
        this.examAi = spec.ai();
        this.configuredInvulnerable = spec.invulnerable();
        // W19：3 秒冷静期——不动、不攻击、免疫一切伤害；到期后按规格恢复
        this.graceTicks = GRACE_TICKS;
        this.setInvulnerable(true);
        this.setNoAi(true);
        this.setPersistenceRequired();
        this.installGoals();
    }

    /**
     * W19：冷静期倒计时。到期后恢复配置的 AI / 无敌状态。
     * 读档时 {@link #graceTicks} 一律为 0（不跨存档恢复冷静期）。
     */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || this.graceTicks <= 0) {
            return;
        }
        this.graceTicks--;
        if (this.graceTicks == 0) {
            this.setInvulnerable(this.configuredInvulnerable);
            this.setNoAi(!this.examAi);
        }
    }

    /** W19：冷静期内免疫一切伤害；{@code /kill} 与虚空仍按原版规则生效。 */
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (this.graceTicks > 0 && !PlayerRoles.isForcedKill(source)) {
            return true;
        }
        return super.isInvulnerableTo(level, source);
    }

    /**
     * W19：额外掉落 1 个普通魔法水晶，概率 30%（考试 / 试炼体）。
     *
     * <p>显式忽略 {@code hitByPlayer}：<b>抢夺不增加数量</b>；只掉 1 个，不走 loot table。
     * 属于考试会话的掉落会在 {@code ExamManager} 的死亡监听里自动收进奖励箱。</p>
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        if (this.random.nextFloat() < CRYSTAL_DROP_CHANCE) {
            this.drop(new ItemStack(MagicItems.magicCrystal), false, false);
        }
    }

    private void installGoals() {
        if (!this.examAi || this.goalsInstalled) {
            return;
        }
        this.goalsInstalled = true;
        // W18：有 AI 的试炼体会靠近目标 / 无目标时游走；TrialCastGoal 只管 LOOK，两者不冲突
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TrialCastGoal(this));
        this.goalSelector.addGoal(2, new ApproachTargetGoal(this));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8D));
        if (this.attackPlayers) {
            // 只锁发起者：Selector 第二个参数是服务端世界
            this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false,
                    (TargetingConditions.Selector) (target, serverLevel) ->
                            this.ownerUuid != null && this.ownerUuid.equals(target.getUUID())));
        }
    }

    /** 当前应当攻击的目标；没有返回 {@code null}。 */
    public LivingEntity resolveCastTarget() {
        if (!(this.level() instanceof ServerLevel level)) {
            return null;
        }
        if (this.attackPlayers && this.ownerUuid != null) {
            var player = level.getServer().getPlayerList().getPlayer(this.ownerUuid);
            if (player != null && player.isAlive() && !player.isSpectator()) {
                return player;
            }
        }
        for (UUID id : this.pigTargets) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator()) {
                return living;
            }
        }
        return null;
    }

    /** 从允许集合里随机挑一种攻击；集合为空时用点式。 */
    public ExamAttack pickAttack() {
        if (this.allowedAttacks.isEmpty()) {
            return ExamAttack.POINT;
        }
        int index = this.random.nextInt(this.allowedAttacks.size());
        int current = 0;
        for (ExamAttack attack : this.allowedAttacks) {
            if (current++ == index) {
                return attack;
            }
        }
        return ExamAttack.POINT;
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public boolean hasSession() {
        return !this.sessionId.isEmpty();
    }

    public float getConfiguredMaxHealth() {
        return this.configuredMaxHealth;
    }

    // ------------------------------------------------------------------
    // 存档：归属 / 配置必须跟着实体走，否则重启后被清扫或行为错误（D-12）
    // ------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("exam_owner", this.ownerUuid == null ? "" : this.ownerUuid.toString());
        output.putString("exam_session", this.sessionId);
        output.putBoolean("exam_ai", this.examAi);
        output.putBoolean("exam_attack_players", this.attackPlayers);
        output.putBoolean("exam_invulnerable", this.configuredInvulnerable);
        output.putFloat("exam_max_health", this.configuredMaxHealth);
        StringBuilder attacks = new StringBuilder();
        for (ExamAttack attack : this.allowedAttacks) {
            if (attacks.length() > 0) {
                attacks.append(',');
            }
            attacks.append(attack.id());
        }
        output.putString("exam_attacks", attacks.toString());
        StringBuilder pigs = new StringBuilder();
        for (UUID pig : this.pigTargets) {
            if (pigs.length() > 0) {
                pigs.append(',');
            }
            pigs.append(pig);
        }
        output.putString("exam_pigs", pigs.toString());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String owner = input.getStringOr("exam_owner", "");
        this.ownerUuid = owner.isEmpty() ? null : parseUuid(owner);
        this.sessionId = input.getStringOr("exam_session", "");
        this.examAi = input.getBooleanOr("exam_ai", false);
        this.attackPlayers = input.getBooleanOr("exam_attack_players", false);
        this.configuredInvulnerable = input.getBooleanOr("exam_invulnerable", true);
        this.configuredMaxHealth = input.getFloatOr("exam_max_health", 20.0F);
        this.allowedAttacks.clear();
        for (String part : split(input.getStringOr("exam_attacks", ""))) {
            ExamAttack attack = ExamAttack.fromId(part);
            if (attack != null) {
                this.allowedAttacks.add(attack);
            }
        }
        this.pigTargets.clear();
        for (String part : split(input.getStringOr("exam_pigs", ""))) {
            UUID pig = parseUuid(part);
            if (pig != null) {
                this.pigTargets.add(pig);
            }
        }
        if (this.configuredMaxHealth > 0.0F) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.configuredMaxHealth);
        }
        // W19：读档不恢复冷静期，按存档里的 AI / 无敌归一化
        this.graceTicks = 0;
        this.setInvulnerable(this.configuredInvulnerable);
        this.setNoAi(!this.examAi);
        this.installGoals();
    }

    private static String[] split(String value) {
        return value == null || value.isEmpty() ? EMPTY : value.split(",");
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** W18：有 AI 时靠近当前目标（16 格内、距离 >3 格时寻路，每 10 tick 重算）。 */
    private static final class ApproachTargetGoal extends Goal {

        private static final double SEARCH_RANGE = 16.0D;
        private static final double KEEP_DISTANCE = 3.0D;

        private final TrialMagicBodyEntity body;
        private int recalcTicks;

        private ApproachTargetGoal(TrialMagicBodyEntity body) {
            this.body = body;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.body.examAi) {
                return false;
            }
            LivingEntity target = this.body.resolveCastTarget();
            if (target == null) {
                return false;
            }
            double distanceSqr = this.body.distanceToSqr(target);
            return distanceSqr <= SEARCH_RANGE * SEARCH_RANGE
                    && distanceSqr > KEEP_DISTANCE * KEEP_DISTANCE;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.recalcTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = this.body.resolveCastTarget();
            if (target == null) {
                return;
            }
            if (this.recalcTicks-- <= 0) {
                this.recalcTicks = 10;
                this.body.getNavigation().moveTo(target, 1.0D);
            }
        }
    }

    /** 周期施法；独立于寻路，站着不动也能打（D-8 / 草案 §5.2）。 */
    private static final class TrialCastGoal extends Goal {

        private final TrialMagicBodyEntity body;
        private int cooldown;

        private TrialCastGoal(TrialMagicBodyEntity body) {
            this.body = body;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.body.examAi && this.body.resolveCastTarget() != null;
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
            LivingEntity target = this.body.resolveCastTarget();
            if (target == null) {
                return;
            }
            this.body.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.cooldown > 0) {
                this.cooldown--;
                return;
            }
            this.cooldown = ExamAttack.ATTACK_INTERVAL_TICKS;
            if (this.body.level() instanceof ServerLevel serverLevel) {
                StaffCaster.castTrialAttack(serverLevel, this.body, target, this.body.pickAttack());
            }
        }
    }
}
