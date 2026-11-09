package com.mugewrayce.sizeadjuster.magic.mana;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * “区块不足”锁定（W13，草案 §7.4 / 交接单 §2.6 + Q3）。
 *
 * <p>一旦某次施法出现“区块魔力不足”，在该区块<b>恢复到 1000 之前</b>、或玩家<b>关闭
 * 供能之前</b>，这个区块完全不参与消耗，只扣水晶。锁定键 = <b>玩家 UUID + 维度 + 区块</b>
 * （主世界与下界同坐标不能串锁）。</p>
 *
 * <p>解除是惰性的：每次施法先看该区块是否已 {@code >= 1000}、或玩家的供能水晶是否
 * 已关掉/换掉，是则清锁再走正常流程（不额外开 ticker）。</p>
 */
public final class ManaLock {

    /** 区块恢复到该值即解除锁定。 */
    public static final int RELEASE_THRESHOLD = 1000;

    private static final Map<UUID, Map<ResourceKey<Level>, Set<Long>>> LOCKS = new HashMap<>();

    private ManaLock() {
    }

    public static boolean isLocked(ServerPlayer player, ResourceKey<Level> dimension, ChunkPos pos) {
        Map<ResourceKey<Level>, Set<Long>> byDimension = LOCKS.get(player.getUUID());
        if (byDimension == null) {
            return false;
        }
        Set<Long> chunks = byDimension.get(dimension);
        return chunks != null && chunks.contains(pos.pack());
    }

    public static void lock(ServerPlayer player, ResourceKey<Level> dimension, ChunkPos pos) {
        LOCKS.computeIfAbsent(player.getUUID(), key -> new HashMap<>())
                .computeIfAbsent(dimension, key -> new HashSet<>())
                .add(pos.pack());
    }

    public static void clear(ServerPlayer player, ResourceKey<Level> dimension, ChunkPos pos) {
        Map<ResourceKey<Level>, Set<Long>> byDimension = LOCKS.get(player.getUUID());
        if (byDimension == null) {
            return;
        }
        Set<Long> chunks = byDimension.get(dimension);
        if (chunks == null) {
            return;
        }
        chunks.remove(pos.pack());
        if (chunks.isEmpty()) {
            byDimension.remove(dimension);
        }
        if (byDimension.isEmpty()) {
            LOCKS.remove(player.getUUID());
        }
    }

    /** 玩家断开 / 服务器停止时清空该玩家的锁。 */
    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            LOCKS.remove(playerId);
        }
    }

    /** 服务器停止时清空全部锁。 */
    public static void clearAll() {
        LOCKS.clear();
    }
}
