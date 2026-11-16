package com.mugewrayce.sizeadjuster.mixin;

import com.mugewrayce.sizeadjuster.magic.cast.TimePauseZone;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 时间暂停 W20：冻结区块不跑自然生成（{@code /tick freeze} 的同款覆盖面）。
 *
 * <p>{@code ServerChunkCache#tickSpawningChunk} 是 private；HEAD cancel 只跳过这一区块的
 * 刷怪 / {@code inhabitedTime} 推进，不影响其他区块。</p>
 */
@Mixin(ServerChunkCache.class)
public abstract class TimePauseSpawnMixin {

    @Inject(method = "tickSpawningChunk", at = @At("HEAD"), cancellable = true)
    private void mugewrayce$skipFrozenSpawn(LevelChunk chunk, long time,
                                            List<MobCategory> categories,
                                            NaturalSpawner.SpawnState spawnState, CallbackInfo ci) {
        if (chunk == null) {
            return;
        }
        ServerChunkCache self = (ServerChunkCache) (Object) this;
        if (self.getLevel() instanceof ServerLevel level
                && TimePauseZone.isFrozen(level, chunk.getPos())) {
            ci.cancel();
        }
    }
}
