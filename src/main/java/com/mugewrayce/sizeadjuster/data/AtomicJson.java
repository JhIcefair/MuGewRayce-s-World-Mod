package com.mugewrayce.sizeadjuster.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 原子 JSON 读写（W05，接口约定 §5）。
 *
 * <p>读：UTF-8；文件不存在返回 {@code null}；空文件也返回 {@code null}；
 * JSON 损坏会抛 {@code JsonParseException}（由调用方决定"宽容跳过"）。</p>
 *
 * <p>写：先写同目录临时文件 {@code xxx.json.tmp}，再用 {@code ATOMIC_MOVE} 替换；
 * 文件系统不支持原子移动时退化为 {@code REPLACE_EXISTING}；无论成功失败，
 * 结束时都尝试删除临时文件，保证不留 {@code .tmp}。</p>
 *
 * <p>目录不存在时自动 {@link Files#createDirectories}。</p>
 */
public final class AtomicJson {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AtomicJson() {
    }

    /**
     * 读取 JSON。
     *
     * @param file 目标文件
     * @param type Gson 类型（如 {@code Map<String,Integer>} 的 {@code Type}）
     * @return 解析结果；文件不存在或为空时 {@code null}
     * @throws IOException        读盘失败
     * @throws com.google.gson.JsonParseException JSON 损坏
     */
    public static <T> T read(Path file, Type type) throws IOException {
        if (file == null || !Files.exists(file)) {
            return null;
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        if (json.isBlank()) {
            return null;
        }
        return GSON.fromJson(json, type);
    }

    /**
     * 原子写入 JSON：临时文件 → 替换目标；失败时清理临时文件并抛出。
     *
     * @param file  目标文件（父目录会自动创建）
     * @param value 要序列化的对象（通常是 Map）
     * @throws IOException 写盘 / 替换失败
     */
    public static void write(Path file, Object value) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = ModDataPaths.tempFile(file);
        try {
            Files.writeString(tmp, GSON.toJson(value), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // 清理失败不影响主流程；下次写入会覆盖同名临时文件
            }
        }
    }
}
