package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.magic.exam.ExamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * W18：考试奖励箱保护。
 *
 * <p>插在 {@code Level#setBlock} HEAD：只要新状态会移除/替换方块（空气或流体），
 * 且该位置是激活中的奖励箱，就取消这次修改。考试自己撤箱前会先解除登记，
 * 因此不会挡住正常清理。</p>
 */
@Mixin(Level.class)
public abstract class ExamChestProtectionMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            at = @At("HEAD"), cancellable = true)
    private void mugewrayce$protectExamChest(BlockPos pos, BlockState state, int flags,
                                             CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        if (self.isClientSide() || pos == null || state == null) {
            return;
        }
        boolean removesBlock = state.isAir() || !state.getFluidState().isEmpty();
        if (removesBlock && ExamManager.isProtectedChest(self, pos)) {
            cir.setReturnValue(false);
        }
    }
}
