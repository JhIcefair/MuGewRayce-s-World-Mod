package com.mugewrayce.sizeadjuster.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家角色持久化：写入世界存档根目录下的 mugewrayce_roles.json
 * （按玩家 UUID → 角色编号）。
 *
 * 服务器启动（进入/重进世界，SERVER_STARTED）时载入；
 * 角色变更时立即写盘（setRole → writeNow）；服务器停止（SERVER_STOPPING）时再写一次。
 * 采用**原子写入**（先写临时文件再替换）防止写盘中途崩溃导致 JSON 损坏；
 * 载入对损坏文件宽容处理（记为无存档，不阻塞进入世界）。
 */
public final class RoleStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {
    }.getType();

    /** 内存缓存（本存档会话）；所有读写加锁保证线程安全。 */
    private static final Map<UUID, Integer> CACHE = new HashMap<>();
    private static Path filePath;

    private RoleStore() {
    }

    /** 服务器启动（进入/重进世界）时载入角色。 */
    public static void load(MinecraftServer server) {
        Path world = server.getWorldPath(LevelResource.ROOT);
        Path target = world.resolve("mugewrayce_roles.json");
        synchronized (CACHE) {
            CACHE.clear();
            filePath = target;
            if (Files.exists(target)) {
                try {
                    String json = Files.readString(target, StandardCharsets.UTF_8);
                    Map<String, Integer> raw = GSON.fromJson(json, MAP_TYPE);
                    if (raw != null) {
                        for (Map.Entry<String, Integer> e : raw.entrySet()) {
                            try {
                                CACHE.put(UUID.fromString(e.getKey()), e.getValue());
                            } catch (IllegalArgumentException ignored) {
                                // 非法 UUID 键：忽略
                            }
                        }
                    }
                } catch (IOException | JsonParseException e) {
                    // 读盘/JSON 损坏：宽容处理——记为无存档，不阻塞进入世界
                    SizeAdjusterMod.LOGGER.warn("角色存档 {} 读取失败，已忽略并重建：{}", target, e.toString());
                    CACHE.clear();
                }
            }
        }
    }

    /** 服务器停止（退出世界/关闭游戏）时再写一次。 */
    public static void save(MinecraftServer server) {
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
            // 原子写入：先写临时文件，成功后替换正式文件（避免写盘中途崩溃损坏）
            Path tmp = filePath.resolveSibling(filePath.getFileName() + ".tmp");
            try {
                Files.writeString(tmp, GSON.toJson(raw), StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
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
}
