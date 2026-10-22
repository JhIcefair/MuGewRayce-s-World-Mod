package com.mugewrayce.sizeadjuster.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/**
 * 净化法阵（Purification Circle）：净化水晶长按右键时出现在玩家脚下的魔法阵。
 * 贴图为 256x256（ls\12.png，原隐藏贴图 O12，新 id：textures/entity/purification_circle.png），
 * 渲染为地面水平方形（边长 2 格）。
 *
 * 生命周期由净化水晶驱动：长按期间每 tick 跟随玩家脚下位置；松开后由水晶 discard。
 * 本实体的 tick 仅做兜底清理：玩家死亡/断线/停止使用（未正常触发 release）时自行消失。
 */
public class PurificationCircleEntity extends Entity {

    private UUID ownerUuid;

    public PurificationCircleEntity(EntityType<? extends PurificationCircleEntity> type, Level level) {
        super(type, level);
    }

    public PurificationCircleEntity(Level level, Player owner) {
        this(ModEntities.PURIFICATION_CIRCLE, level);
        this.ownerUuid = owner.getUUID();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // 无需要同步的数据
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // 法阵为瞬态实体（noSave），不保存数据
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        // 法阵为瞬态实体（noSave），不保存数据
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.ownerUuid == null) {
                this.discard();
                return;
            }
            Player owner = this.level().getPlayerByUUID(this.ownerUuid);
            if (owner == null || owner.isRemoved() || !owner.isAlive() || !owner.isUsingItem()) {
                this.discard();
            }
        }
    }
}
