package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.magic.cast.TimePauseZone;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时间暂停：冻结区块内的<b>方块 / 流体计划刻</b>（W19）。
 *
 * <p>水 / 岩浆流动、红石更新、作物与熔炉的计划刻都走 {@code LevelTicks}；W14b 只拦了
 * 实体、方块实体与随机刻，这一层当时漏掉了。</p>
 *
 * <p>注入 {@code scheduleForThisTick}（收集到“本 tick 要跑”队列的那一步）而不是
 * {@code tick}：这里还拿得到原来的 {@code TickPriority} 与 {@code subTickOrder}，
 * 红石更新顺序不会因为暂停被打乱。</p>
 *
 * <p>做法是<b>延后 +1 tick 而不是丢弃</b>：重新 {@code schedule} 回原容器，暂停结束后
 * 水 / 红石继续，不会出现“水永远不流”。此刻该刻已从 {@code ticksPerPosition} 移除，
 * 重新调度不会撞重复集合。</p>
 *
 * <p>注意：昼夜是维度级全局时钟，本 mixin 只冻结区域 tick，不动世界时钟。</p>
 */
@Mixin(LevelTicks.class)
public abstract class TimePauseScheduledTickMixin {

    @Inject(method = "scheduleForThisTick", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void mugewrayce$delayFrozenScheduledTick(ScheduledTick<?> tick, CallbackInfo ci) {
        if (tick == null) {
            return;
        }
        ServerLevel level = TimePauseZone.levelIfTickFrozen((LevelTicks<?>) (Object) this, tick.pos());
        if (level == null) {
            return;
        }
        // 延后 1 tick 重新排队，保留原优先级 / 子刻顺序
        ((LevelTicks) (Object) this).schedule(new ScheduledTick(tick.type(), tick.pos(),
                level.getGameTime() + 1L, tick.priority(), tick.subTickOrder()));
        ci.cancel();
    }
}
