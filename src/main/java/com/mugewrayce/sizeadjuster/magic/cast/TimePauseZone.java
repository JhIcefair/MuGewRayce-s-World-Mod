package com.mugewrayce.sizeadjuster.magic.cast;

import com.mugewrayce.sizeadjuster.magic.mana.ManaService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 时间暂停的区域注册表（W14b，草案 §6.4 / 交接单 §2.4）。
 *
 * <p>四层暂停：区域内的非玩家实体跳过 tick/AI、方块实体跳过 tick、随机刻跳过、
 * <b>方块 / 流体计划刻延后</b>（W19）；使用者本人永远不冻结，移动超过 0.5 格立即结束；
 * 每 20 tick 扣 1 点魔力，扣不起立即结束并提示。</p>
 *
 * <p><b>昼夜（世界时钟）不冻结</b>：26.1 的 {@code WorldClock} 是维度级全局值
 * （{@code ServerClockManager} 全服一份），不能按区块冻结；因此本魔法只让“区域 tick 静止”，
 * {@code /time query gametime} 继续增长。若以后要连昼夜一起停，需要另开一个明确开关（本轮不做）。</p>
 *
 * <p>{@link #TIME_PAUSE_ENABLED} 是总开关；{@link #TIME_PAUSE_FREEZE_BLOCKS}
 * 关闭时退化为“只冻结实体”；{@link #TIME_PAUSE_FREEZE_PLAYERS} 默认关闭
 * （服务端强拉其他玩家会橡皮筋，草案与实现口径差异见 W14 交接单 §2.10 E）。</p>
 */
public final class TimePauseZone {

    public static final boolean TIME_PAUSE_ENABLED = true;
    public static final boolean TIME_PAUSE_FREEZE_BLOCKS = true;
    public static final boolean TIME_PAUSE_FREEZE_PLAYERS = false;

    /** 使用者允许的位移容差（格）；超过即失效。 */
    private static final double OWNER_MOVE_TOLERANCE = 0.5D;

    /** 维度 → 活动区域。 */
    private static final Map<ResourceKey<Level>, List<Zone>> ZONES = new HashMap<>();

    /** 被冻结玩家的回拉位置（仅 TIME_PAUSE_FREEZE_PLAYERS 打开时使用）。 */
    private static final Map<UUID, Vec3> FROZEN_PLAYER_POSITIONS = new HashMap<>();

    /** W19：{@code LevelTicks} → 所属维度（身份表缓存，避免每个计划刻都扫全部维度）。 */
    private static final Map<LevelTicks<?>, ServerLevel> TICK_OWNERS = new IdentityHashMap<>();

    /** W19：当前服务端（{@link #tick(MinecraftServer)} 里更新，供计划刻 mixin 反查维度）。 */
    private static MinecraftServer currentServer;

    private TimePauseZone() {
    }

    /** 启动区域；调用方已扣除 5×区块 的启动耗魔。 */
    public static boolean start(ServerLevel level, ServerPlayer owner, int range, List<String> excluded) {
        if (!TIME_PAUSE_ENABLED) {
            return false;
        }
        int side = Math.max(1, Math.min(7, range));
        int startX = owner.chunkPosition().x() - (side - 1) / 2;
        int startZ = owner.chunkPosition().z() - (side - 1) / 2;
        end(owner.getUUID());
        Zone zone = new Zone(owner.getUUID(),
                startX, startZ, startX + side - 1, startZ + side - 1,
                owner.position());
        applyExcluded(zone, excluded);
        ZONES.computeIfAbsent(level.dimension(), key -> new ArrayList<>()).add(zone);
        return true;
    }

    /** 服务器每 tick：检查使用者、位移、每秒魔力。 */
    public static void tick(MinecraftServer server) {
        currentServer = server;
        if (ZONES.isEmpty()) {
            if (!TICK_OWNERS.isEmpty()) {
                TICK_OWNERS.clear();
            }
            return;
        }
        for (Map.Entry<ResourceKey<Level>, List<Zone>> entry : ZONES.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }
            Iterator<Zone> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                Zone zone = iterator.next();
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(zone.owner);
                if (owner == null || owner.isRemoved() || !owner.isAlive()
                        || owner.level() != level
                        || owner.position().distanceToSqr(zone.anchor) > OWNER_MOVE_TOLERANCE * OWNER_MOVE_TOLERANCE) {
                    iterator.remove();
                    continue;
                }
                zone.ticks++;
                if (zone.ticks % 20 == 0) {
                    ManaService.Result result = ManaService.tryConsume(owner,
                            com.mugewrayce.sizeadjuster.magic.MagicNumbers.MANA_TIME_PAUSE_PER_SECOND, true);
                    if (result == ManaService.Result.INSUFFICIENT) {
                        owner.sendSystemMessage(Component.translatable(
                                "message.mugewrayce-mod.mana_insufficient"), true);
                        iterator.remove();
                        continue;
                    }
                }
                if (TIME_PAUSE_FREEZE_PLAYERS) {
                    freezePlayers(level, zone);
                }
            }
            if (entry.getValue().isEmpty()) {
                ZONES.remove(entry.getKey());
            }
        }
    }

    private static void freezePlayers(ServerLevel level, Zone zone) {
        for (ServerPlayer player : level.players()) {
            if (player.getUUID().equals(zone.owner) || isExcluded(zone, player)) {
                FROZEN_PLAYER_POSITIONS.remove(player.getUUID());
                continue;
            }
            if (!zone.contains(player.chunkPosition())) {
                FROZEN_PLAYER_POSITIONS.remove(player.getUUID());
                continue;
            }
            Vec3 frozen = FROZEN_PLAYER_POSITIONS.computeIfAbsent(player.getUUID(), id -> player.position());
            player.setDeltaMovement(Vec3.ZERO);
            if (player.position().distanceToSqr(frozen) > 1.0E-4D) {
                player.teleportTo(frozen.x, frozen.y, frozen.z);
            }
            player.hurtMarked = true;
        }
    }

    /** 实体是否被冻结（实体 tick mixin 用）。 */
    public static boolean isFrozen(Level level, Entity entity) {
        if (!TIME_PAUSE_ENABLED || entity == null) {
            return false;
        }
        List<Zone> zones = ZONES.get(level.dimension());
        if (zones == null || zones.isEmpty()) {
            return false;
        }
        for (Zone zone : zones) {
            if (entity.getUUID().equals(zone.owner) || isExcluded(zone, entity)) {
                continue;
            }
            if (!zone.contains(entity.chunkPosition())) {
                continue;
            }
            if (entity instanceof Player && !TIME_PAUSE_FREEZE_PLAYERS) {
                continue; // 默认不强拉其他玩家
            }
            return true;
        }
        return false;
    }

    /** 方块/区块是否在冻结区域（方块实体与随机刻 mixin 用）。 */
    public static boolean isFrozen(Level level, BlockPos pos) {
        if (!TIME_PAUSE_ENABLED || !TIME_PAUSE_FREEZE_BLOCKS) {
            return false;
        }
        List<Zone> zones = ZONES.get(level.dimension());
        if (zones == null || zones.isEmpty()) {
            return false;
        }
        ChunkPos chunk = ChunkPos.containing(pos);
        for (Zone zone : zones) {
            if (zone.contains(chunk)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFrozen(Level level, ChunkPos chunk) {
        if (!TIME_PAUSE_ENABLED || !TIME_PAUSE_FREEZE_BLOCKS) {
            return false;
        }
        List<Zone> zones = ZONES.get(level.dimension());
        if (zones == null || zones.isEmpty()) {
            return false;
        }
        for (Zone zone : zones) {
            if (zone.contains(chunk)) {
                return true;
            }
        }
        return false;
    }

    public static void clearPlayer(UUID playerId) {
        for (List<Zone> zones : ZONES.values()) {
            zones.removeIf(zone -> zone.owner.equals(playerId));
        }
        FROZEN_PLAYER_POSITIONS.remove(playerId);
    }

    /** 结束该使用者的全部区域（重新施放 / 松手时调用；W17 改为 public）。 */
    public static void end(UUID owner) {
        clearPlayer(owner);
    }

    /** 该使用者当前是否有活动区域（W17：长按右键的逻辑判断）。 */
    public static boolean isActive(UUID owner) {
        if (owner == null) {
            return false;
        }
        for (List<Zone> zones : ZONES.values()) {
            for (Zone zone : zones) {
                if (zone.owner.equals(owner)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void clearAll() {
        ZONES.clear();
        FROZEN_PLAYER_POSITIONS.clear();
        TICK_OWNERS.clear();
        currentServer = null;
    }

    // ------------------------------------------------------------------
    // W19：方块 / 流体计划刻（LevelTicks）
    // ------------------------------------------------------------------

    /**
     * 计划刻所属维度；缓存未命中时在当前服务端的各维度里比
     * {@code getBlockTicks()} / {@code getFluidTicks()}。
     */
    public static ServerLevel levelOf(LevelTicks<?> ticks) {
        if (ticks == null) {
            return null;
        }
        ServerLevel cached = TICK_OWNERS.get(ticks);
        if (cached != null) {
            return cached;
        }
        MinecraftServer server = currentServer;
        if (server == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getBlockTicks() == ticks || level.getFluidTicks() == ticks) {
                TICK_OWNERS.put(ticks, level);
                return level;
            }
        }
        return null;
    }

    /**
     * W19：这条计划刻是否需要延后；需要时返回它所在的维度，否则 {@code null}。
     *
     * <p>无区域时在首行就返回（热路径）；只有冻结区块内的计划刻才会重排。</p>
     */
    public static ServerLevel levelIfTickFrozen(LevelTicks<?> ticks, BlockPos pos) {
        if (!TIME_PAUSE_ENABLED || !TIME_PAUSE_FREEZE_BLOCKS
                || ZONES.isEmpty() || ticks == null || pos == null) {
            return null;
        }
        ServerLevel level = levelOf(ticks);
        if (level == null) {
            return null;
        }
        return isFrozen(level, pos) ? level : null;
    }

    private static boolean isExcluded(Zone zone, Entity entity) {
        if (zone.excludedEntities.contains(entity.getUUID())) {
            return true;
        }
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return zone.excludedTypes.contains(typeId);
    }

    /** 从 magic_book_data.excluded 解析 entity:<uuid> / type:<id>。 */
    private static void applyExcluded(Zone zone, List<String> excluded) {
        if (excluded == null) {
            return;
        }
        for (String raw : excluded) {
            if (raw == null) {
                continue;
            }
            if (raw.startsWith("entity:")) {
                try {
                    zone.excludedEntities.add(UUID.fromString(raw.substring(7)));
                } catch (IllegalArgumentException ignored) {
                    // 非法 UUID 忽略
                }
            } else if (raw.startsWith("type:") && raw.length() > 5) {
                zone.excludedTypes.add(raw.substring(5));
            }
        }
    }

    private static final class Zone {
        private final UUID owner;
        private final int minChunkX;
        private final int minChunkZ;
        private final int maxChunkX;
        private final int maxChunkZ;
        private final Vec3 anchor;
        private final Set<UUID> excludedEntities = new HashSet<>();
        private final Set<String> excludedTypes = new HashSet<>();
        private int ticks;

        private Zone(UUID owner, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ, Vec3 anchor) {
            this.owner = owner;
            this.minChunkX = minChunkX;
            this.minChunkZ = minChunkZ;
            this.maxChunkX = maxChunkX;
            this.maxChunkZ = maxChunkZ;
            this.anchor = anchor;
        }

        private boolean contains(ChunkPos chunk) {
            return chunk.x() >= minChunkX && chunk.x() <= maxChunkX
                    && chunk.z() >= minChunkZ && chunk.z() <= maxChunkZ;
        }
    }
}
