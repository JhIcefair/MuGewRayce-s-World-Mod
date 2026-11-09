package com.mugewrayce.sizeadjuster.magic.exam;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.data.AtomicJson;
import com.mugewrayce.sizeadjuster.data.ModDataPaths;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * {@code exams.json} 读写（W15，接口约定 §5-3）。
 *
 * <p>结构 {@code {"sessions":[...]}}，字段名与 {@link ExamSession} 一一对应并转成
 * 下划线小写；BlockPos / ChunkPos 存成 {@code "x,y,z"} / {@code "x,z"} 字符串；
 * 原子写（{@link AtomicJson}）、损坏按“记日志 + 跳过 + 不阻塞”处理。</p>
 */
public final class ExamStorage {

    private ExamStorage() {
    }

    /** 读取全部会话；文件不存在 / 损坏都返回空表（损坏会记日志）。 */
    public static List<ExamSession> load(MinecraftServer server) {
        Path file = ModDataPaths.examsFile(server);
        JsonObject root;
        try {
            root = AtomicJson.read(file, JsonObject.class);
        } catch (IOException | JsonParseException e) {
            SizeAdjusterMod.LOGGER.warn("Failed to read exams.json ({}); starting with no sessions", e.toString());
            return List.of();
        }
        if (root == null || !root.has("sessions") || !root.get("sessions").isJsonArray()) {
            return List.of();
        }
        List<ExamSession> result = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("sessions")) {
            ExamSession session = parse(element);
            if (session != null) {
                result.add(session);
            }
        }
        return result;
    }

    /** 原子写入；写入失败只记日志，不抛给调用方（不能因为存档坏了卡死服务端）。 */
    public static void save(MinecraftServer server, Collection<ExamSession> sessions) {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        for (ExamSession session : sessions) {
            array.add(write(session));
        }
        root.add("sessions", array);
        try {
            AtomicJson.write(ModDataPaths.examsFile(server), root);
        } catch (IOException e) {
            SizeAdjusterMod.LOGGER.warn("Failed to write exams.json ({})", e.toString());
        }
    }

    private static JsonObject write(ExamSession session) {
        JsonObject object = new JsonObject();
        object.addProperty("id", session.id());
        object.addProperty("owner", session.owner().toString());
        object.addProperty("dimension", session.dimension().toString());
        object.addProperty("chest_pos", pos(session.chestPos()));
        object.addProperty("center_chunk", chunk(session.centerChunk()));
        object.addProperty("right_click_pos", pos(session.rightClickPos()));
        object.addProperty("type", session.type());
        object.addProperty("mode", session.mode());
        object.addProperty("sub", session.sub());
        object.addProperty("target_level", session.targetLevel());
        object.addProperty("remaining_ticks", session.remainingTicks());
        object.addProperty("state", session.state().name());
        JsonArray targets = new JsonArray();
        for (UUID target : session.targets()) {
            targets.add(target.toString());
        }
        object.add("targets", targets);
        object.addProperty("break_attempts", session.breakAttempts());
        object.addProperty("started_at", session.startedAt());
        return object;
    }

    private static ExamSession parse(JsonElement element) {
        try {
            JsonObject object = element.getAsJsonObject();
            List<UUID> targets = new ArrayList<>();
            if (object.has("targets") && object.get("targets").isJsonArray()) {
                for (JsonElement target : object.getAsJsonArray("targets")) {
                    targets.add(UUID.fromString(target.getAsString()));
                }
            }
            return new ExamSession(
                    object.get("id").getAsString(),
                    UUID.fromString(object.get("owner").getAsString()),
                    Identifier.parse(object.get("dimension").getAsString()),
                    pos(object.get("chest_pos").getAsString()),
                    chunk(object.get("center_chunk").getAsString()),
                    pos(object.get("right_click_pos").getAsString()),
                    object.get("type").getAsString(),
                    object.get("mode").getAsString(),
                    object.get("sub").getAsString(),
                    object.get("target_level").getAsInt(),
                    object.get("remaining_ticks").getAsInt(),
                    ExamSession.State.valueOf(object.get("state").getAsString()),
                    targets,
                    object.get("break_attempts").getAsInt(),
                    object.get("started_at").getAsLong());
        } catch (RuntimeException e) {
            SizeAdjusterMod.LOGGER.warn("Skipping malformed exam session entry ({})", e.toString());
            return null;
        }
    }

    private static String pos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static BlockPos pos(String value) {
        String[] parts = value.split(",");
        return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    private static String chunk(ChunkPos pos) {
        return pos.x() + "," + pos.z();
    }

    private static ChunkPos chunk(String value) {
        String[] parts = value.split(",");
        return new ChunkPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
