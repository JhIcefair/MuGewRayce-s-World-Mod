package com.mugewrayce.sizeadjuster.magic.cast;

import com.mugewrayce.sizeadjuster.component.CertEntry;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.component.StaffData;
import com.mugewrayce.sizeadjuster.component.StaffSelectionData;
import com.mugewrayce.sizeadjuster.entity.MagicBoltEntity;
import com.mugewrayce.sizeadjuster.entity.MagicFormationEntity;
import com.mugewrayce.sizeadjuster.entity.EquipoiseFormationEntity;
import com.mugewrayce.sizeadjuster.magic.CastMode;
import com.mugewrayce.sizeadjuster.magic.CastSubMode;
import com.mugewrayce.sizeadjuster.magic.MagicDataRules;
import com.mugewrayce.sizeadjuster.magic.MagicNumbers;
import com.mugewrayce.sizeadjuster.magic.MagicPalette;
import com.mugewrayce.sizeadjuster.magic.MagicSkill;
import com.mugewrayce.sizeadjuster.magic.MagicType;
import com.mugewrayce.sizeadjuster.magic.SoundFx;
import com.mugewrayce.sizeadjuster.magic.exam.ExamAttack;
import com.mugewrayce.sizeadjuster.magic.mana.ManaService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 普通魔法杖的施法入口（W14b）。
 *
 * <p>右键读取 {@code staff_selection_data} + {@code staff_data.entries}，
 * 服务端重新校验三元组与魔力后执行；失败不产生任何副作用。</p>
 *
 * <ul>
 *   <li>点式 / 单发线式 / 魔法阵 / 技能：右键一次性；</li>
 *   <li>持续线式 / 普通防御：按住；持续线式每刻一点，防御阵每刻维持；</li>
 *   <li>耗魔按草案 §7.2；净化法杖不走本类。</li>
 * </ul>
 */
public final class StaffCaster {

    private static final int SUSTAINED_AIM_UPDATE_TICKS = 20;
    private static final int SECOND_TICKS = 20;
    private static final double REACH = 64.0D;

    /** W18：点式 / 魔法阵的蓄力阈值（tick，与净化法杖一致）。 */
    private static final int CHARGE_TICKS = 10;

    private static final Map<UUID, SustainedState> SUSTAINED = new HashMap<>();
    private static final Map<UUID, MagicFormationEntity> DEFENSES = new HashMap<>();
    /** W18：点式 / 非防御魔法阵的蓄力状态（服务端按玩家记录）。 */
    private static final Map<UUID, ChargeState> CHARGES = new HashMap<>();

    private StaffCaster() {
    }

    public static InteractionResult use(Level level, Player player, InteractionHand hand) {
        Selection selection = Selection.read(player.getMainHandItem());
        if (selection == null) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            // 客户端也要进入“使用中”，否则 onUseTick / 使用动画不会走
            if (selection.isHold() || selection.isCharging()) {
                player.startUsingItem(hand);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }
        if (selection.isHold()) {
            if (!tryPay(serverPlayer, selection.startCost(), false)) {
                return InteractionResult.SUCCESS;
            }
            if (selection.isSustained()) {
                SUSTAINED.put(player.getUUID(), new SustainedState(selection, player.getLookAngle().normalize()));
                SoundFx.at(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.4F);
            } else {
                spawnFormation(serverLevel, serverPlayer, selection, true);
                SoundFx.at(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.9F);
            }
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (selection.isSkill()) {
            SkillEffects.Plan plan = SkillEffects.plan(serverLevel, serverPlayer, selection.skill());
            if (plan == null) {
                return InteractionResult.SUCCESS;
            }
            if (!tryPay(serverPlayer, plan.cost(), false)) {
                return InteractionResult.SUCCESS;
            }
            SoundFx.at(player, SoundEvents.EVOKER_CAST_SPELL, 0.8F, 1.2F);
            plan.action().run();
            return InteractionResult.SUCCESS;
        }
        if (selection.isCharging()) {
            // W18：点式 / 非防御魔法阵改为“按下进入蓄力，松手结算”；
            // 单击与长按的分流在 releaseUsing 里按 hold 的 tick 数判定。
            CHARGES.remove(player.getUUID());
            CHARGES.put(player.getUUID(), new ChargeState(selection));
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (!tryPay(serverPlayer, selection.startCost(), false)) {
            return InteractionResult.SUCCESS;
        }
        castOnce(serverLevel, serverPlayer, selection);
        return InteractionResult.SUCCESS;
    }

    public static void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return;
        }
        ChargeState charge = CHARGES.get(player.getUUID());
        if (charge != null) {
            // W18：蓄力满 10 tick 播一次“充能完成”音效；不重复发射
            int held = stack.getUseDuration(player) - remainingUseDuration;
            if (held >= CHARGE_TICKS && !charge.notified) {
                charge.notified = true;
                SoundFx.at(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.5F);
            }
            return;
        }
        SustainedState sustained = SUSTAINED.get(player.getUUID());
        if (sustained != null) {
            tickSustained(serverLevel, player, sustained);
            return;
        }
        MagicFormationEntity defense = DEFENSES.get(player.getUUID());
        if (defense != null) {
            tickDefense(serverLevel, player, defense);
        }
    }

    public static boolean releaseUsing(Level level, LivingEntity entity, ItemStack stack, int timeLeft) {
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        if (level instanceof ServerLevel serverLevel) {
            ChargeState charge = CHARGES.remove(player.getUUID());
            if (charge != null) {
                // W18：按住时长 = 总时长 − 剩余时长（不受释放时序影响）
                int held = stack.getUseDuration(player) - timeLeft;
                resolveCharge(serverLevel, player, charge.selection, held);
                return true;
            }
        }
        SUSTAINED.remove(player.getUUID());
        MagicFormationEntity defense = DEFENSES.remove(player.getUUID());
        if (defense != null && defense.isAlive()) {
            EquipoiseFormationEntity.deactivateDefense(player.getUUID());
            defense.discard();
        }
        return true;
    }

    /**
     * W18：蓄力结算。
     *
     * <ul>
     *   <li>点式：不足 10 tick 按原数值施放一发；满 10 tick 伤害 {@code ceil(base × 1.1)}。</li>
     *   <li>魔法阵（非防御）：不足 10 tick 不生成、不扣魔；满 10 tick 生成并保留 30 秒。</li>
     * </ul>
     */
    private static void resolveCharge(ServerLevel level, ServerPlayer player, Selection selection, int held) {
        if (selection.mode() == CastMode.POINT) {
            if (!tryPay(player, selection.startCost(), false)) {
                return;
            }
            castPoint(level, player, selection, held >= CHARGE_TICKS);
            return;
        }
        if (held < CHARGE_TICKS) {
            return; // 魔法阵：点一下不生成，也不扣启动耗魔
        }
        if (!tryPay(player, selection.startCost(), false)) {
            return;
        }
        spawnFormation(level, player, selection, false);
        SoundFx.at(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.8F);
    }

    /** W18：点式一发（充能时伤害 +10%，向上取整）；治疗点式不加成。 */
    private static void castPoint(ServerLevel level, ServerPlayer player, Selection selection, boolean charged) {
        int color = MagicPalette.colorFor(selection.type());
        SoundFx.at(player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.7F, 1.2F);
        if (selection.type() == MagicType.HEAL) {
            spawnHealBolt(level, player, color, MagicBoltEntity.KIND_POINT,
                    MagicNumbers.pointHeal(selection.level()), selection.level());
            return;
        }
        float damage = MagicNumbers.pointDamage(selection.level());
        if (charged) {
            damage = (float) Math.ceil(damage * 1.1F);
        }
        spawnDamageBolt(level, player, color, MagicBoltEntity.KIND_POINT, damage, false);
    }

    public static void clearPlayer(UUID playerId) {
        SUSTAINED.remove(playerId);
        CHARGES.remove(playerId);
        MagicFormationEntity defense = DEFENSES.remove(playerId);
        if (defense != null && defense.isAlive()) {
            EquipoiseFormationEntity.deactivateDefense(playerId);
            defense.discard();
        }
    }

    private static void castOnce(ServerLevel level, ServerPlayer player, Selection selection) {
        int color = MagicPalette.colorFor(selection.type());
        if (selection.mode() == CastMode.FORMATION) {
            SoundFx.at(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.8F);
        } else {
            SoundFx.at(player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.7F, 1.2F);
        }
        if (selection.mode() == CastMode.POINT) {
            if (selection.type() == MagicType.HEAL) {
                spawnHealBolt(level, player, color, MagicBoltEntity.KIND_POINT,
                        MagicNumbers.pointHeal(selection.level()), selection.level());
            } else {
                spawnDamageBolt(level, player, color, MagicBoltEntity.KIND_POINT,
                        MagicNumbers.pointDamage(selection.level()), false);
            }
            return;
        }
        if (selection.mode() == CastMode.LINE && selection.sub() == CastSubMode.LINE_SINGLE) {
            if (selection.type() == MagicType.HEAL) {
                spawnHealBolt(level, player, color, MagicBoltEntity.KIND_LINE,
                        MagicNumbers.lineSingleHeal(selection.level()), selection.level());
            } else {
                Vec3 look = player.getLookAngle().normalize();
                Vec3 eye = player.getEyePosition().subtract(0.0, 0.1, 0.0);
                for (int i = 0; i < MagicNumbers.LINE_SINGLE_ORBS; i++) {
                    MagicBoltEntity bolt = new MagicBoltEntity(level, player);
                    bolt.setPos(eye.add(look.scale(0.5D * i)));
                    bolt.setColor(color);
                    bolt.setKind(MagicBoltEntity.KIND_LINE);
                    bolt.setDamage(MagicNumbers.lineSingleOrbDamage(selection.level()));
                    bolt.setBypassInvulnerability(true);
                    bolt.shoot(look.x, look.y, look.z, 4.5F, 0.0F);
                    level.addFreshEntity(bolt);
                }
            }
            return;
        }
        if (selection.mode() == CastMode.FORMATION) {
            spawnFormation(level, player, selection, false);
        }
    }

    /**
     * 考试 / 试炼目标实体的攻击入口（W15，D-8）。
     *
     * <p>复用玩家施法同一套 {@link MagicBoltEntity} + 魔法伤害，只是发起者换成试炼魔法体；
     * 弹速慢一些，玩家可以躲。注意 {@code StaffCaster} 的其余入口都绑定
     * {@link ServerPlayer}，所以这里单独开一个 {@link LivingEntity} 版本。</p>
     */
    public static void castTrialAttack(ServerLevel level, LivingEntity caster,
                                       LivingEntity target, ExamAttack attack) {
        if (caster == null || target == null || !target.isAlive()) {
            return;
        }
        Vec3 from = caster.getEyePosition().subtract(0.0, 0.2, 0.0);
        Vec3 direction = target.getEyePosition().subtract(from);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        direction = direction.normalize();
        MagicBoltEntity bolt = new MagicBoltEntity(level, caster);
        bolt.setPos(from);
        bolt.setColor(MagicPalette.NATURAL);
        bolt.setKind(attack == ExamAttack.POINT ? MagicBoltEntity.KIND_POINT : MagicBoltEntity.KIND_LINE);
        bolt.setDamage(attack.damage());
        bolt.setBypassInvulnerability(true);
        bolt.shoot(direction.x, direction.y, direction.z, 1.2F, 0.4F);
        level.addFreshEntity(bolt);
    }

    private static void spawnDamageBolt(ServerLevel level, ServerPlayer player, int color, int kind,
                                        float damage, boolean pierce) {        Vec3 look = player.getLookAngle().normalize();
        MagicBoltEntity bolt = new MagicBoltEntity(level, player);
        bolt.setPos(player.getEyePosition().subtract(0.0, 0.1, 0.0));
        bolt.setColor(color);
        bolt.setKind(kind);
        bolt.setDamage(damage);
        bolt.setPierce(pierce);
        bolt.shoot(look.x, look.y, look.z, 4.5F, 0.0F);
        level.addFreshEntity(bolt);
    }

    private static void spawnHealBolt(ServerLevel level, ServerPlayer player, int color, int kind,
                                      float heal, int levelValue) {
        Vec3 look = player.getLookAngle().normalize();
        MagicBoltEntity bolt = new MagicBoltEntity(level, player);
        bolt.setPos(player.getEyePosition().subtract(0.0, 0.1, 0.0));
        bolt.setColor(color);
        bolt.setKind(kind);
        bolt.setHeal(heal, MagicNumbers.regenTicks(levelValue), MagicNumbers.regenAmplifier(levelValue));
        bolt.shoot(look.x, look.y, look.z, 4.5F, 0.0F);
        level.addFreshEntity(bolt);
        SoundFx.at(player, SoundEvents.BEACON_ACTIVATE, 0.5F, 1.6F);
    }

    private static void tickSustained(ServerLevel level, ServerPlayer player, SustainedState state) {
        Vec3 look = player.getLookAngle().normalize();
        if (state.direction.distanceToSqr(look) <= 1.0E-4D) {
            state.ticksSinceChange = -1;
        } else {
            if (state.ticksSinceChange < 0) {
                state.ticksSinceChange = 0;
            }
            state.ticksSinceChange++;
            if (state.ticksSinceChange >= SUSTAINED_AIM_UPDATE_TICKS) {
                state.direction = look;
                state.ticksSinceChange = -1;
            }
        }

        Selection selection = state.selection;
        Vec3 pos = player.getEyePosition().subtract(0.0, 0.1, 0.0)
                .add(state.direction.scale(0.6D));
        if (selection.type() == MagicType.HEAL) {
            MagicBoltEntity bolt = new MagicBoltEntity(level, player);
            bolt.setPos(pos);
            bolt.setColor(MagicPalette.colorFor(selection.type()));
            bolt.setKind(MagicBoltEntity.KIND_LINE);
            bolt.setHeal(MagicNumbers.sustainedHeal(selection.level()),
                    MagicNumbers.regenTicks(selection.level()), MagicNumbers.regenAmplifier(selection.level()));
            bolt.shoot(state.direction.x, state.direction.y, state.direction.z, 4.5F, 0.0F);
            level.addFreshEntity(bolt);
        } else {
            MagicBoltEntity bolt = new MagicBoltEntity(level, player);
            bolt.setPos(pos);
            bolt.setColor(MagicPalette.colorFor(selection.type()));
            bolt.setKind(MagicBoltEntity.KIND_LINE);
            bolt.setDamage(MagicNumbers.sustainedDamage(selection.level()));
            bolt.setPierce(true);
            bolt.setBypassInvulnerability(true);
            bolt.shoot(state.direction.x, state.direction.y, state.direction.z, 4.5F, 0.0F);
            level.addFreshEntity(bolt);
        }

        state.ticks++;
        if (state.ticks % SECOND_TICKS == 0 && !tryPay(player, selection.perSecondCost(), true)) {
            stopUsing(player);
        }
    }

    private static void tickDefense(ServerLevel level, ServerPlayer player, MagicFormationEntity defense) {
        if (!defense.isAlive()) {
            DEFENSES.remove(player.getUUID());
            EquipoiseFormationEntity.deactivateDefense(player.getUUID());
            return;
        }
        Selection selection = Selection.read(player.getMainHandItem());
        if (selection == null || !selection.isDefense()) {
            return;
        }
        if (player.tickCount % SECOND_TICKS == 0
                && !tryPay(player, selection.perSecondCost(), true)) {
            stopUsing(player);
        }
    }

    private static void stopUsing(ServerPlayer player) {
        player.stopUsingItem();
        SUSTAINED.remove(player.getUUID());
        CHARGES.remove(player.getUUID());
        MagicFormationEntity defense = DEFENSES.remove(player.getUUID());
        if (defense != null && defense.isAlive()) {
            EquipoiseFormationEntity.deactivateDefense(player.getUUID());
            defense.discard();
        }
    }

    private static boolean tryPay(ServerPlayer player, int amount, boolean everySecond) {
        if (amount <= 0) {
            return true;
        }
        ManaService.Result result = ManaService.tryConsume(player, amount, everySecond);
        if (result == ManaService.Result.INSUFFICIENT) {
            player.sendSystemMessage(
                    Component.translatable("message.mugewrayce-mod.mana_insufficient"), true);
            return false;
        }
        return true;
    }

    private static MagicFormationEntity spawnFormation(ServerLevel level, ServerPlayer player,
                                                       Selection selection, boolean defense) {
        if (defense) {
            MagicFormationEntity formation = new MagicFormationEntity(level, player,
                    EquipoiseFormationEntity.TYPE_MAGIC_DEFENSE, selection.level());
            formation.setPos(player.getEyePosition().add(player.getLookAngle()
                    .scale(EquipoiseFormationEntity.DEFENSE_DISTANCE)));
            formation.setYRot(player.getYRot());
            formation.setXRot(player.getXRot());
            formation.setSize(3.0F * (float) player.getAttributeValue(Attributes.SCALE));
            level.addFreshEntity(formation);
            DEFENSES.put(player.getUUID(), formation);
            EquipoiseFormationEntity.activateDefense(player.getUUID(), formation);
            return formation;
        }
        int type = switch (selection.sub()) {
            case FORMATION_GROUP_ATTACK -> EquipoiseFormationEntity.TYPE_MAGIC_GROUP_ATTACK;
            case FORMATION_SELF_HEAL -> EquipoiseFormationEntity.TYPE_MAGIC_SELF_HEAL;
            case FORMATION_GROUP_HEAL -> EquipoiseFormationEntity.TYPE_MAGIC_GROUP_HEAL;
            default -> -1;
        };
        if (type < 0) {
            return null;
        }
        MagicFormationEntity formation = new MagicFormationEntity(level, player, type, selection.level());
        if (type == EquipoiseFormationEntity.TYPE_MAGIC_SELF_HEAL) {
            formation.setPos(player.getX(), player.getY() + 0.02, player.getZ());
            formation.setSize(2.0F);
        } else {
            Vec3 pos = pickGroupPosition(level, player);
            formation.setPos(pos.x, pos.y + 0.02, pos.z);
            formation.setSize(6.0F);
        }
        formation.startPersist();
        level.addFreshEntity(formation);
        return formation;
    }

    private static Vec3 pickGroupPosition(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(REACH));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() != HitResult.Type.MISS ? hit.getLocation() : end;
    }

    /** 一次施法的选择（从法杖组件读取并服务端校验）。 */
    public record Selection(MagicType type, CastMode mode, CastSubMode sub, MagicSkill skill, int level) {

        public static Selection read(ItemStack stack) {
            StaffData staff = stack.getOrDefault(ModComponents.STAFF_DATA, StaffData.DEFAULT);
            StaffSelectionData selection = stack.getOrDefault(
                    ModComponents.STAFF_SELECTION_DATA, StaffSelectionData.DEFAULT);
            if (selection.isEmpty() || staff.entries().isEmpty()) {
                return null;
            }
            if (!MagicDataRules.isValidShape(selection.type(), selection.mode(), selection.sub())) {
                return null;
            }
            MagicType type = MagicType.fromId(selection.type());
            CastMode mode = CastMode.fromId(selection.mode());
            if (type == null || mode == null) {
                return null;
            }
            CertEntry entry = findEntry(staff, selection);
            if (entry == null) {
                return null;
            }
            CastSubMode sub = mode == CastMode.SKILL || mode == CastMode.POINT
                    ? null : CastSubMode.fromId(selection.sub());
            MagicSkill skill = mode == CastMode.SKILL ? MagicSkill.fromId(selection.sub()) : null;
            if (mode == CastMode.SKILL && (skill == null || !type.supports(skill))) {
                return null;
            }
            return new Selection(type, mode, sub, skill, entry.level());
        }

        private static CertEntry findEntry(StaffData staff, StaffSelectionData selection) {
            for (CertEntry entry : staff.entries()) {
                if (entry.type().equals(selection.type())
                        && entry.mode().equals(selection.mode())
                        && entry.sub().equals(selection.sub())) {
                    return entry;
                }
            }
            return null;
        }

        public boolean isSustained() {
            return mode == CastMode.LINE && sub == CastSubMode.LINE_SUSTAINED;
        }

        public boolean isDefense() {
            return mode == CastMode.FORMATION && sub == CastSubMode.FORMATION_OUTWARD_DEFENSE;
        }

        /** W18：点式与非防御魔法阵走“按住蓄力、松手结算”。 */
        public boolean isCharging() {
            return mode == CastMode.POINT || (mode == CastMode.FORMATION && !isDefense());
        }

        public boolean isHold() {
            return isSustained() || isDefense();
        }

        public boolean isSkill() {
            return mode == CastMode.SKILL;
        }

        public int startCost() {
            if (mode == CastMode.POINT) {
                return MagicNumbers.MANA_POINT;
            }
            if (mode == CastMode.LINE) {
                return sub == CastSubMode.LINE_SINGLE
                        ? MagicNumbers.MANA_LINE_SINGLE : MagicNumbers.MANA_LINE_SUSTAINED_START;
            }
            if (mode == CastMode.FORMATION) {
                if (sub == CastSubMode.FORMATION_GROUP_ATTACK) {
                    return MagicNumbers.MANA_FORMATION_GROUP_ATTACK;
                }
                if (sub == CastSubMode.FORMATION_SELF_HEAL) {
                    return MagicNumbers.MANA_FORMATION_SELF_HEAL;
                }
                if (sub == CastSubMode.FORMATION_GROUP_HEAL) {
                    return MagicNumbers.MANA_FORMATION_GROUP_HEAL;
                }
                if (sub == CastSubMode.FORMATION_OUTWARD_DEFENSE) {
                    return MagicNumbers.MANA_FORMATION_DEFENSE_START;
                }
            }
            return 0;
        }

        public int perSecondCost() {
            if (isSustained()) {
                return MagicNumbers.MANA_LINE_SUSTAINED_PER_SECOND;
            }
            if (isDefense()) {
                return MagicNumbers.MANA_FORMATION_DEFENSE_PER_SECOND;
            }
            return 0;
        }
    }

    /** W18：点式 / 魔法阵的蓄力状态。 */
    private static final class ChargeState {
        private final Selection selection;
        private boolean notified;

        private ChargeState(Selection selection) {
            this.selection = selection;
        }
    }

    private static final class SustainedState {
        private final Selection selection;
        private Vec3 direction;
        private int ticksSinceChange = -1;
        private int ticks;

        private SustainedState(Selection selection, Vec3 direction) {
            this.selection = selection;
            this.direction = direction;
        }
    }
}
