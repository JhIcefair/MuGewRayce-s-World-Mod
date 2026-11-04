package com.mugewrayce.sizeadjuster.data;

import com.google.gson.reflect.TypeToken;
import com.mugewrayce.sizeadjuster.SizeAdjusterMod;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色存档迁移：旧世界根 {@code mugewrayce_roles.json} → {@code data/mugewrayce-mod/roles.json}
 * （W05，接口约定 §5）。
 *
 * <p>迁移语义（与交接单一致）：</p>
 * <pre>
 * 旧存在、新不存在  → 旧搬到新；旧删除
 * 旧存在、新存在    → 新优先；旧中缺失 UUID 合并进新；旧删除
 * 旧损坏、新可用    → 保留新；记录旧损坏；不删除旧文件（留作人工恢复）
 * 旧损坏、新不存在  → 记录；不阻塞；保留旧文件
 * 新损坏、旧可读    → 用旧数据重建新文件；重建成功后再删旧
 * 新损坏、旧不存在  → 返回空；下次写入重建
 * </pre>
 *
 * <p><b>本类不抛异常</b>：任何 IO / JSON 问题都只记录日志并返回可用的（可能是空的）数据，
 * 保证"进不去世界"不会发生。</p>
 */
public final class RoleMigration {

    /** 角色文件格式：{@code Map<String uuid, Integer role>}（与旧文件相同）。 */
    public static final Type ROLE_MAP_TYPE = new TypeToken<Map<String, Integer>>() {
    }.getType();

    private RoleMigration() {
    }

    /**
     * 执行迁移并返回角色数据（原始 {@code uuid → role} 映射，未做 UUID/编号校验）。
     *
     * @param dataRoot 世界的 {@code data} 目录（服务端用 {@code server.getWorldPath(LevelResource.DATA)}）
     * @return 迁移后的角色数据；永不返回 {@code null}
     */
    public static Map<String, Integer> migrate(Path dataRoot) {
        Path legacy = ModDataPaths.legacyRolesFile(dataRoot);
        Path target = ModDataPaths.rolesFile(dataRoot);

        ReadResult newFile = read(target, "新");
        ReadResult oldFile = read(legacy, "旧");

        // 没有旧文件：直接用新文件（损坏 → 空）
        if (!oldFile.exists()) {
            return newFile.readable() ? newFile.data() : Map.of();
        }

        // 旧文件损坏：记录、不阻塞、不覆盖可读数据、不删旧文件
        if (!oldFile.readable()) {
            SizeAdjusterMod.LOGGER.warn("旧角色存档 {} 损坏，保留原文件不删除；继续使用新存档", legacy);
            return newFile.readable() ? newFile.data() : Map.of();
        }

        Map<String, Integer> oldData = oldFile.data();

        // 旧存在、新不存在：搬迁（原子移动；失败退回复制 + 删除）
        if (!newFile.exists()) {
            if (!moveLegacy(legacy, target, oldData)) {
                SizeAdjusterMod.LOGGER.warn("旧角色存档搬迁未完成，本次仍使用旧数据：{}", legacy);
            }
            warnIfLegacyLeft(legacy);
            return oldData;
        }

        // 新损坏、旧可读：用旧数据重建新文件，重建成功后删旧；失败则保留旧文件
        if (!newFile.readable()) {
            SizeAdjusterMod.LOGGER.warn("新角色存档 {} 损坏，尝试用旧存档重建", target);
            if (writeAtomic(target, oldData)) {
                deleteLegacy(legacy);
            }
            return oldData;
        }

        // 新旧都可读：新优先，合并旧中缺失的 UUID，原子写回，再删旧
        Map<String, Integer> merged = new LinkedHashMap<>(newFile.data());
        for (Map.Entry<String, Integer> entry : oldData.entrySet()) {
            merged.putIfAbsent(entry.getKey(), entry.getValue());
        }
        if (!merged.equals(newFile.data())) {
            writeAtomic(target, merged);
        }
        deleteLegacy(legacy);
        return merged;
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    private record ReadResult(boolean exists, boolean readable, Map<String, Integer> data) {
    }

    private static ReadResult read(Path file, String label) {
        if (!Files.exists(file)) {
            return new ReadResult(false, true, Map.of());
        }
        try {
            Map<String, Integer> data = AtomicJson.read(file, ROLE_MAP_TYPE);
            return new ReadResult(true, true, data == null ? Map.of() : data);
        } catch (IOException | RuntimeException e) {
            SizeAdjusterMod.LOGGER.warn("{}角色存档 {} 读取失败（按损坏处理）：{}", label, file, e.toString());
            return new ReadResult(true, false, Map.of());
        }
    }

    /** 旧文件 → 新文件：优先原子移动，失败则复制 + 删除。 */
    private static boolean moveLegacy(Path legacy, Path target, Map<String, Integer> data) {
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.move(legacy, target, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            SizeAdjusterMod.LOGGER.warn("旧角色存档原子搬迁失败，改用复制 + 删除：{}", e.toString());
        }
        return copyAndDelete(legacy, target, data);
    }

    /** 复制 + 删除：先确认新文件可读，再删旧文件。 */
    private static boolean copyAndDelete(Path legacy, Path target, Map<String, Integer> data) {
        if (!writeAtomic(target, data)) {
            return false;
        }
        try {
            if (!Files.exists(target) || Files.size(target) == 0) {
                SizeAdjusterMod.LOGGER.warn("复制后的新角色存档不可读，保留旧文件：{}", legacy);
                return false;
            }
            Files.deleteIfExists(legacy);
            return true;
        } catch (IOException e) {
            SizeAdjusterMod.LOGGER.warn("旧角色存档删除失败（不影响进世界）：{}", e.toString());
            return false;
        }
    }

    private static boolean writeAtomic(Path target, Map<String, Integer> data) {
        try {
            AtomicJson.write(target, data);
            return true;
        } catch (IOException e) {
            SizeAdjusterMod.LOGGER.warn("角色存档原子写入失败：{}", e.toString());
            return false;
        }
    }

    private static void deleteLegacy(Path legacy) {
        try {
            Files.deleteIfExists(legacy);
        } catch (IOException e) {
            SizeAdjusterMod.LOGGER.warn("旧角色存档删除失败（不影响进世界）：{}", e.toString());
            return;
        }
        warnIfLegacyLeft(legacy);
    }

    private static void warnIfLegacyLeft(Path legacy) {
        if (Files.exists(legacy)) {
            SizeAdjusterMod.LOGGER.warn("旧角色存档仍然存在（迁移未完全清理）：{}", legacy);
        }
    }
}
