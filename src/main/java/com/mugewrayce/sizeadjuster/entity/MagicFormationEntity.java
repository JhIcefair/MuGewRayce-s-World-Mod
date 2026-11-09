package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.magic.MagicDamage;
import com.mugewrayce.sizeadjuster.magic.MagicNumbers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * W14 魔法体系的三类魔法阵（服务端子类，复用 {@link EquipoiseFormationEntity} 的实体类型与渲染）。
 *
 * <ul>
 *   <li>{@code TYPE_MAGIC_GROUP_ATTACK}：每秒对范围内实体 5 × 等级倍率魔法伤害；</li>
 *   <li>{@code TYPE_MAGIC_SELF_HEAL}：每刻治疗施法者 1 + 等级每刻加点；</li>
 *   <li>{@code TYPE_MAGIC_GROUP_HEAL}：每刻治疗范围内实体；</li>
 *   <li>{@code TYPE_MAGIC_DEFENSE}：竖立视线前 0.5 格，抵挡伤害（B-2：不挡来自魔法阵的伤害）。</li>
 * </ul>
 *
 * <p>服务端由本类接管效果；客户端由 {@link EquipoiseFormationEntity} 基类按同步的
 * 类型/尺寸渲染（{@code magic_formation.png}）。</p>
 */
public class MagicFormationEntity extends EquipoiseFormationEntity {

    private final int magicLevel;
    private int attackTickCounter;
    private boolean selfRegenApplied;
    private final Set<UUID> groupRegenApplied = new HashSet<>();

    public MagicFormationEntity(Level level, Player owner, int formationType, int magicLevel) {
        super(level, owner, formationType);
        this.magicLevel = MagicNumbers.clampLevel(magicLevel);
    }

    public int getMagicLevel() {
        return this.magicLevel;
    }

    @Override
    protected void tickMagic(Player owner) {
        int type = this.getFormationType();
        if (type == TYPE_MAGIC_GROUP_ATTACK) {
            if (++this.attackTickCounter % 20 == 0) {
                applyAttack(owner);
            }
            return;
        }
        if (type == TYPE_MAGIC_SELF_HEAL) {
            owner.heal(MagicNumbers.formationHeal(this.magicLevel));
            applyRegen(owner, true);
            return;
        }
        if (type == TYPE_MAGIC_GROUP_HEAL) {
            for (LivingEntity entity : this.entitiesInRange()) {
                entity.heal(MagicNumbers.formationHeal(this.magicLevel));
                applyRegen(entity, false);
            }
            return;
        }
        if (type == TYPE_MAGIC_DEFENSE) {
            this.setPos(owner.getEyePosition().add(owner.getLookAngle().scale(DEFENSE_DISTANCE)));
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.setSize(3.0F * (float) owner.getAttributeValue(Attributes.SCALE));
        }
    }

    private void applyAttack(Player owner) {
        float damage = MagicNumbers.formationDamage(this.magicLevel);
        for (LivingEntity entity : this.entitiesInRange()) {
            if (entity == owner) {
                continue;
            }
            entity.hurt(MagicDamage.of((ServerLevel) this.level(), this, owner), damage);
        }
    }

    /** IV/V 的额外生命恢复：每个目标只挂一次，避免每刻刷新。 */
    private void applyRegen(LivingEntity target, boolean self) {
        int ticks = MagicNumbers.regenTicks(this.magicLevel);
        if (ticks <= 0) {
            return;
        }
        if (self) {
            if (this.selfRegenApplied) {
                return;
            }
            this.selfRegenApplied = true;
        } else if (!this.groupRegenApplied.add(target.getUUID())) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks,
                MagicNumbers.regenAmplifier(this.magicLevel)));
    }
}
