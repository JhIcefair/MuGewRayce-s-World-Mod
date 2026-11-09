package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.magic.cast.TimePauseZone;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 时间暂停：区域内方块实体跳过 tick（W14b）。
 *
 * <p>26.1 的 {@code Level#tickBlockEntities} 通过 {@code TickingBlockEntity#tick()}
 *（含自定义 ticker 的熔炉/漏斗）统一调用，Redirect 该调用即可逐个跳过，
 * 不取消整个方法。</p>
 */
@Mixin(Level.class)
public abstract class TimePauseLevelMixin {

    @Redirect(method = "tickBlockEntities",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void mugewrayce$skipFrozenBlockEntity(TickingBlockEntity ticker) {
        Level self = (Level) (Object) this;
        if (TimePauseZone.isFrozen(self, ticker.getPos())) {
            return;
        }
        ticker.tick();
    }
}
