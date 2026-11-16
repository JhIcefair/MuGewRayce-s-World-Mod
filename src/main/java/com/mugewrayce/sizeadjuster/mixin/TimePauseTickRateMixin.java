package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.magic.cast.TimePauseZone;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 时间暂停 W20：让区域冻结享受 {@code /tick freeze} 的实体覆盖面。
 *
 * <p>{@code ServerLevel#tick} 的实体遍历 lambda 在调用 {@code tickNonPassenger} 之前先问
 * {@link TickRateManager#isEntityFrozen(Entity)}；返回 true 就整条跳过。因此这里补一条判定，
 * 能一次覆盖所有“不调 {@code Entity#tick} 的实体”：TNT 倒计时、落沙、矿车、
 * 掉落物 {@code age++}（6000 tick 丢弃）、乘客树等。</p>
 *
 * <p>原生 {@code /tick freeze} 的结果优先（{@code RETURN} 注入且只在 false 时改写）；
 * 玩家与载着玩家的实体永不冻结。原有的 {@code Entity}/{@code LivingEntity} HEAD cancel
 * 两个 mixin 保留作兜底，无副作用。</p>
 */
@Mixin(TickRateManager.class)
public abstract class TimePauseTickRateMixin {

    @Inject(method = "isEntityFrozen", at = @At("RETURN"), cancellable = true)
    private void mugewrayce$zoneFreeze(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == null || cir.getReturnValueZ()) {
            return;
        }
        // 玩家不冻结（与 /tick freeze 一致；区域冻结的玩家回拉另有 TIME_PAUSE_FREEZE_PLAYERS 开关）
        if (entity instanceof Player) {
            return;
        }
        // 载着玩家的载具不做整体冻结，避免玩家被卡在实体里
        if (entity.countPlayerPassengers() > 0) {
            return;
        }
        if (TimePauseZone.isFrozen(entity.level(), entity)) {
            cir.setReturnValue(true);
        }
    }
}
