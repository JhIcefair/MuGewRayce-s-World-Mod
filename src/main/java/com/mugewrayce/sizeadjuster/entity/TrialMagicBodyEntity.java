package com.mugewrayce.sizeadjuster.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 试炼魔法体（W08a 只做配置骨架）。
 *
 * <p>与 {@link LifelessMagicBodyEntity} 共用皮肤与渲染器；具体“无 AI / 无敌 / 生命值 / 归属”配置
 * 由 W15 的考试/试炼流程设置。本任务只保留字段与 setter，不实现考试逻辑。</p>
 */
public class TrialMagicBodyEntity extends AbstractMagicBodyEntity {

    private UUID ownerUuid;
    private String mode = "";
    private float configuredMaxHealth = 20.0F;

    public TrialMagicBodyEntity(EntityType<? extends TrialMagicBodyEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setInvulnerable(true);
    }

    /** W15 接具体生命值/归属；W08 只给一个可用的基础值。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 0.0)
                .add(Attributes.SCALE, 1.0);
    }

    @Override
    protected void registerGoals() {
        // 考试/试炼魔法体默认无 AI；W15 按配置决定是否启用攻击/移动。
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode == null ? "" : mode;
    }

    public float getConfiguredMaxHealth() {
        return configuredMaxHealth;
    }

    public void setConfiguredMaxHealth(float configuredMaxHealth) {
        this.configuredMaxHealth = configuredMaxHealth;
    }
}
