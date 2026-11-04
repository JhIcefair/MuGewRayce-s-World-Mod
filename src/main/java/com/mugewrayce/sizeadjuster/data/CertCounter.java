package com.mugewrayce.sizeadjuster.data;

import com.google.gson.reflect.TypeToken;
import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map;
import java.util.OptionalInt;

/**
 * 学习证编号计数器（W06，接口约定 §5）。
 *
 * <p>文件：{@code <world>/data/mugewrayce-mod/cert_counter.json}，内容 {@code {"last": N}}。
 * {@code 0} 表示还没发过号，下一个编号 = {@code last + 1}（显示时 {@code %04d} → {@code 0001}）。</p>
 *
 * <p>关键规则：<b>先把新值原子写盘成功，再返回编号</b>；写失败时内存不前进、返回空，
 * 绝不发出"未落盘的编号"（避免重启后重号）。文件损坏按 {@code last=0} 继续并记录日志
 * （只在损坏场景可能重号，报告已注明）。</p>
 *
 * <p>只在服务端使用；载入由 {@code SizeAdjusterMod} 的 {@code SERVER_STARTED} 触发。</p>
 */
public final class CertCounter {

    private static final Type LAST_MAP_TYPE = new TypeToken<Map<String, Integer>>() {
    }.getType();

    private static final Object LOCK = new Object();

    /** 已发出的最大编号（0 = 还没发过）。 */
    private static int last = 0;
    private static Path filePath;

    private CertCounter() {
    }

    /** 服务器启动时载入（进入/重进世界）。 */
    public static void load(MinecraftServer server) {
        loadFrom(server.getWorldPath(LevelResource.DATA));
    }

    /** 载入计数器；参数是世界的 {@code data} 目录（离线自检也走这里）。 */
    public static void loadFrom(Path dataRoot) {
        synchronized (LOCK) {
            filePath = ModDataPaths.certCounterFile(dataRoot);
            last = readLast(filePath);
            SizeAdjusterMod.LOGGER.info("学习证编号计数器已载入：last={}（{}）", last, filePath);
        }
    }

    /**
     * 申请一个新编号：先原子落盘，成功后才推进内存并返回。
     *
     * @return 新编号（从 1 开始）；未载入或写盘失败时 {@link OptionalInt#empty()}
     */
    public static OptionalInt nextNumber() {
        synchronized (LOCK) {
            if (filePath == null) {
                SizeAdjusterMod.LOGGER.warn("学习证编号计数器尚未载入，拒绝发号");
                return OptionalInt.empty();
            }
            int next = last + 1;
            if (next <= 0) {
                SizeAdjusterMod.LOGGER.error("学习证编号计数器溢出（last={}），拒绝发号", last);
                return OptionalInt.empty();
            }
            try {
                AtomicJson.write(filePath, Map.of("last", next));
            } catch (IOException e) {
                // 未落盘：内存保持旧值，下一次重试同一个编号
                SizeAdjusterMod.LOGGER.warn("学习证编号计数器写入失败，本次不发号：{}", e.toString());
                return OptionalInt.empty();
            }
            last = next;
            return OptionalInt.of(next);
        }
    }

    /** 当前已发出的最大编号（0 = 未发过）；供自检 / 调试查看。 */
    public static int lastIssued() {
        synchronized (LOCK) {
            return last;
        }
    }

    /** 读取 {@code last}；文件不存在 / 为空 → 0；损坏或非法 → 记录日志并按 0 继续。 */
    private static int readLast(Path file) {
        try {
            Map<String, Integer> data = AtomicJson.read(file, LAST_MAP_TYPE);
            if (data == null) {
                return 0;
            }
            Integer value = data.get("last");
            if (value == null || value < 0) {
                SizeAdjusterMod.LOGGER.warn("学习证编号计数器内容非法（last={}），按 0 继续", value);
                return 0;
            }
            return value;
        } catch (IOException | RuntimeException e) {
            SizeAdjusterMod.LOGGER.warn("学习证编号计数器 {} 损坏，按 0 继续（可能重复发号）：{}",
                    file, e.toString());
            return 0;
        }
    }
}
