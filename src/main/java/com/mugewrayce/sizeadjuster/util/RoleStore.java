package com.mugewrayce.sizeadjuster.util;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.data.AtomicJson;
import com.mugewrayce.sizeadjuster.data.ModDataPaths;
import com.mugewrayce.sizeadjuster.data.RoleMigration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家角色持久化：写入 {@code <world>/data/mugewrayce-mod/roles.json}（按 UUID → 角色编号）。
 *
 * <p>W05 起：载入前先由 {@link RoleMigration} 把旧世界根 {@code mugewrayce_roles.json}
 * 迁移到新路径；正常迁移后旧文件不再存在（接口约定 §5）。</p>
 *
 * <p>服务器启动（进入/重进世界，SERVER_STARTED）时载入；
 * 角色变更时立即写盘（setRole → writeNow）；服务器停止（SERVER_STOPPING）时再写一次。
 * 写入走 {@link AtomicJson} 的原子替换；载入对损坏文件宽容处理——
 * 记录日志、清空内存缓存、允许进入世界，下一次写入再重建文件。</p>
 */
public final class RoleStore {

    /** 内存缓存（本存档会话）；所有读写加锁保证线程安全。 */
    private static final Map<UUID, Integer> CACHE = new HashMap<>();
    private static Path filePath;

    private RoleStore() {
    }

    /** 服务器启动（进入/重进世界）时：先迁移旧文件，再载入角色。 */
    public static void load(MinecraftServer server) {
        Path dataRoot = server.getWorldPath(LevelResource.DATA);
        Map<String, Integer> raw = RoleMigration.migrate(dataRoot);
        Map<UUID, Integer> parsed = sanitize(raw);
        synchronized (CACHE) {
            CACHE.clear();
            CACHE.putAll(parsed);
            // 即使迁移/读取失败也记住路径：下一次写入会重建新文件
            filePath = ModDataPaths.rolesFile(dataRoot);
        }
        SizeAdjusterMod.LOGGER.info("角色存档已载入 {} 条（{}）", parsed.size(), filePath);
    }

    /** 服务器停止（退出世界/关闭游戏）时再写一次。 */
    public static void save(MinecraftServer server) {
        synchronized (CACHE) {
            filePath = ModDataPaths.rolesFile(server.getWorldPath(LevelResource.DATA));
        }
        writeNow();
    }

    /** 立即写盘（角色变更后调用，异常退出也已落盘）。 */
    public static void saveNow() {
        writeNow();
    }

    private static void writeNow() {
        synchronized (CACHE) {
            if (filePath == null) {
                return;
            }
            Map<String, Integer> raw = new HashMap<>();
            for (Map.Entry<UUID, Integer> e : CACHE.entrySet()) {
                raw.put(e.getKey().toString(), e.getValue());
            }
            try {
                AtomicJson.write(filePath, raw);
            } catch (IOException e) {
                // 写盘失败：内存数据保留，下次再试
                SizeAdjusterMod.LOGGER.warn("角色存档写入失败（内存已保留）：{}", e.toString());
            }
        }
    }

    public static int getRole(UUID uuid) {
        synchronized (CACHE) {
            return CACHE.getOrDefault(uuid, PlayerRoles.ROLE_DEFAULT);
        }
    }

    public static void setRole(UUID uuid, int role) {
        synchronized (CACHE) {
            CACHE.put(uuid, role);
        }
        writeNow();
    }

    /**
     * 校验并转换原始角色数据：非法 UUID 键忽略、非法角色编号回退 {@link PlayerRoles#ROLE_DEFAULT}。
     *
     * <p>由 {@link #load(MinecraftServer)} 调用；也供离线自检直接复用（不依赖服务端）。</p>
     */
    public static Map<UUID, Integer> sanitize(Map<String, Integer> raw) {
        Map<UUID, Integer> parsed = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return parsed;
        }
        for (Map.Entry<String, Integer> e : raw.entrySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(e.getKey());
            } catch (IllegalArgumentException | NullPointerException ex) {
                SizeAdjusterMod.LOGGER.debug("角色存档中的非法 UUID 键已忽略：{}", e.getKey());
                continue;
            }
            Integer value = e.getValue();
            int role = value == null ? PlayerRoles.ROLE_DEFAULT : value;
            if (!PlayerRoles.isValid(role)) {
                SizeAdjusterMod.LOGGER.warn("角色存档中 {} 的角色编号 {} 非法，已回退为默认", uuid, role);
                role = PlayerRoles.ROLE_DEFAULT;
            }
            parsed.put(uuid, role);
        }
        return parsed;
    }
}
