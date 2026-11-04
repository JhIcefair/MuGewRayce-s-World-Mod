package com.mugewrayce.sizeadjuster.data;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

/**
 * 模组持久化路径（W05，接口约定 §5）。
 *
 * <p>统一放在世界存档的 {@code data} 目录下、模组专用文件夹里：</p>
 * <pre>
 * &lt;world&gt;/data/mugewrayce-mod/roles.json
 * &lt;world&gt;/mugewrayce_roles.json   ← 旧文件（迁移源）
 * </pre>
 *
 * <p>约定：{@code data} 目录一律通过 {@link MinecraftServer#getWorldPath(LevelResource)}
 * 获取（{@link LevelResource#DATA}），<b>不要手写 {@code world/data} 拼接</b>；
 * 只有离线自检才会直接传 {@code data} 目录进来。</p>
 */
public final class ModDataPaths {

    /** 模组数据目录名（= 模组 ID）。 */
    public static final String DATA_DIR_NAME = SizeAdjusterMod.MOD_ID;

    /** 角色文件名（新路径）。 */
    public static final String ROLES_FILE_NAME = "roles.json";

    /** 学习证编号计数器文件名（W06，接口约定 §5）。 */
    public static final String CERT_COUNTER_FILE_NAME = "cert_counter.json";

    /** 旧角色文件名（世界根，迁移源）。 */
    public static final String LEGACY_ROLES_FILE_NAME = "mugewrayce_roles.json";

    private ModDataPaths() {
    }

    /** {@code <world>/data/mugewrayce-mod}。 */
    public static Path dataDir(MinecraftServer server) {
        return dataDir(server.getWorldPath(LevelResource.DATA));
    }

    /** {@code <data>/mugewrayce-mod}；参数是世界的 {@code data} 目录。 */
    public static Path dataDir(Path dataRoot) {
        return dataRoot.resolve(DATA_DIR_NAME);
    }

    /** {@code <world>/data/mugewrayce-mod/roles.json}。 */
    public static Path rolesFile(MinecraftServer server) {
        return rolesFile(server.getWorldPath(LevelResource.DATA));
    }

    /** {@code <data>/mugewrayce-mod/roles.json}。 */
    public static Path rolesFile(Path dataRoot) {
        return dataDir(dataRoot).resolve(ROLES_FILE_NAME);
    }

    /** {@code <world>/mugewrayce_roles.json}（旧文件，迁移源）。 */
    public static Path legacyRolesFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(LEGACY_ROLES_FILE_NAME);
    }

    /** {@code <world>/mugewrayce_roles.json}；由 {@code <world>/data} 反推世界根。 */
    public static Path legacyRolesFile(Path dataRoot) {
        Path worldRoot = dataRoot.getParent();
        return worldRoot == null
                ? Path.of(LEGACY_ROLES_FILE_NAME)
                : worldRoot.resolve(LEGACY_ROLES_FILE_NAME);
    }

    /** {@code <world>/data/mugewrayce-mod/cert_counter.json}（W06 学习证编号计数器）。 */
    public static Path certCounterFile(MinecraftServer server) {
        return certCounterFile(server.getWorldPath(LevelResource.DATA));
    }

    /** {@code <data>/mugewrayce-mod/cert_counter.json}。 */
    public static Path certCounterFile(Path dataRoot) {
        return dataDir(dataRoot).resolve(CERT_COUNTER_FILE_NAME);
    }

    /** 与目标文件同目录的原子写临时文件（{@code xxx.json.tmp}）。 */
    public static Path tempFile(Path target) {
        return target.resolveSibling(target.getFileName() + ".tmp");
    }
}
