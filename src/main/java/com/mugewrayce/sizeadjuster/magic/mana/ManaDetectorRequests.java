package com.mugewrayce.sizeadjuster.magic.mana;

import com.mugewrayce.sizeadjuster.magic.SoundFx;
import com.mugewrayce.sizeadjuster.payload.ManaDetectorResultPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 魔力值探测器服务端延时队列（W12）。
 *
 * <p>请求瞬间用 {@link ManaProbe#sample} 取样并保存快照；60 tick（3 秒）后发送
 * {@link ManaDetectorResultPayload}。玩家关闭界面/换维度/死亡时客户端会忽略结果；
 * 服务端在玩家断开时清理队列。同一玩家重复请求以最后一次为准。</p>
 */
public final class ManaDetectorRequests {

    /** 3 秒 = 60 tick。 */
    public static final int DELAY_TICKS = 60;

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private ManaDetectorRequests() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ManaDetectorRequests::onEndTick);
    }

    /** 在请求瞬间取样；覆盖该玩家尚未完成的旧请求。 */
    public static void schedule(ServerPlayer player, int centerChunkX, int centerChunkZ) {
        ServerLevel level = player.level();
        int[] values = ManaProbe.sample(level, centerChunkX, centerChunkZ);
        PENDING.put(player.getUUID(), new Pending(player, values, DELAY_TICKS));
        SoundFx.at(player, SoundEvents.NOTE_BLOCK_HAT.value(), 0.6F, 1.0F);
    }

    /** 玩家断开时清理其待回包请求。 */
    public static void clear(UUID playerId) {
        if (playerId != null) {
            PENDING.remove(playerId);
        }
    }

    /** 服务器停止时清空全部待回包请求。 */
    public static void clearAll() {
        PENDING.clear();
    }

    private static void onEndTick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Pending>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next().getValue();
            if (--pending.remaining > 0) {
                continue;
            }
            iterator.remove();
            ServerPlayer player = pending.player;
            if (player == null || player.isRemoved()) {
                continue;
            }
            ServerPlayNetworking.send(player, new ManaDetectorResultPayload(pending.values, true));
            SoundFx.at(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.2F);
        }
    }

    private static final class Pending {
        private final ServerPlayer player;
        private final int[] values;
        private int remaining;

        private Pending(ServerPlayer player, int[] values, int remaining) {
            this.player = player;
            this.values = values;
            this.remaining = remaining;
        }
    }
}
