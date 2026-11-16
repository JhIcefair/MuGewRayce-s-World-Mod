package com.mugewrayce.sizeadjuster.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mugewrayce.sizeadjuster.magic.cast.TimePauseZone;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 时间暂停：区域内方块实体跳过 tick（W14b；W20 改为 MixinExtras 的 {@code @WrapOperation}）。
 *
 * <p>26.1 的 {@code Level#tickBlockEntities} 通过 {@code TickingBlockEntity#tick()}
 *（含自定义 ticker 的熔炉/漏斗）统一调用，包一层即可逐个跳过，不取消整个方法。</p>
 *
 * <p>W20 把原来的 {@code @Redirect} 换成 {@code @WrapOperation}：只包住这一处调用、
 * 不独占重定向点，与其他 mod 同时改这里时冲突更小（MixinExtras 由 Loader 内置）。</p>
 */
@Mixin(Level.class)
public abstract class TimePauseLevelMixin {

    @WrapOperation(method = "tickBlockEntities",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void mugewrayce$skipFrozenBlockEntity(TickingBlockEntity ticker, Operation<Void> original) {
        Level self = (Level) (Object) this;
        if (TimePauseZone.isFrozen(self, ticker.getPos())) {
            return;
        }
        original.call(ticker);
    }
}
