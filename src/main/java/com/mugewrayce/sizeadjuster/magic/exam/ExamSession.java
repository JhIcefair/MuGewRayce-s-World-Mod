package com.mugewrayce.sizeadjuster.magic.exam;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

import java.util.List;
import java.util.UUID;

/**
 * 一条考试 / 试炼会话（W15，接口约定 §5-3 的持久化单位）。
 *
 * <p>计时存 {@link #remainingTicks} 递减，<b>不存绝对时间</b>（跨服务器重启安全，D-6）。
 * 字段名写进 {@code exams.json} 时转成下划线小写（见 {@link ExamStorage}）。</p>
 */
public record ExamSession(
        String id,
        UUID owner,
        Identifier dimension,
        BlockPos chestPos,
        ChunkPos centerChunk,
        BlockPos rightClickPos,
        String type,
        String mode,
        String sub,
        int targetLevel,
        int remainingTicks,
        State state,
        List<UUID> targets,
        int breakAttempts,
        long startedAt) {

    /** 会话状态（接口约定 §5-3）。 */
    public enum State {
        RUNNING,
        PASSED,
        FAILED
    }

    public ExamSession {
        targets = targets == null ? List.of() : List.copyOf(targets);
    }

    /** 复制一份并替换运行期会变的四个量。 */
    public ExamSession with(int remainingTicks, State state, List<UUID> targets, int breakAttempts) {
        return new ExamSession(id, owner, dimension, chestPos, centerChunk, rightClickPos,
                type, mode, sub, targetLevel, remainingTicks, state, targets, breakAttempts, startedAt);
    }

    /** 已进行多少 tick（总时长 12000）。 */
    public int elapsedTicks(int totalTicks) {
        return Math.max(0, totalTicks - remainingTicks);
    }
}
