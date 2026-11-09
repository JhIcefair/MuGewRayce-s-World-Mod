package com.mugewrayce.sizeadjuster.magic.exam;

import com.mugewrayce.sizeadjuster.magic.CastMode;
import com.mugewrayce.sizeadjuster.magic.CastSubMode;
import com.mugewrayce.sizeadjuster.magic.MagicType;

import java.util.EnumSet;
import java.util.Set;

/**
 * 考试 / 试炼内容表（W15，交接卡 §2.5，逐条照抄草案 §9.8）。
 *
 * <p>纯函数：只由（类型 + 模式 + 具体项 + 目标等级）决定配置，不碰世界、不碰实体，
 * 便于 {@code W15ExamSpecSelfCheck} 逐条断言。</p>
 *
 * <p>等价类映射（Q-34）：{@code natural / fire / ice / lightning / water} 共用
 * {@link #NATURAL} 一套；{@code heal} 用 {@link #HEAL} 一套；
 * {@code flash} / {@link CastSubMode#FORMATION_OUTWARD_DEFENSE} / {@link CastMode#SKILL}
 * 返回 {@link #NO_EXAM}。</p>
 *
 * <p>可考条目总数 = 普通 4 模式×5 级 + 治疗 5 模式×5 级 = <b>45</b>。</p>
 *
 * <p>字段补充说明（D-11）：卡 §3.1 的原始 record 表达不了“治疗 I 先扣目标 10/40 血”
 * 与“对己 I 扣玩家 50% 血”，因此增加 {@link #damage} 与 {@link #selfPercent}；
 * {@code allowedAttacks} 用 {@link ExamAttack} 而不是 {@code CastMode}。</p>
 */
public record ExamSpec(
        Kind kind,
        int kills,
        int health,
        int seconds,
        boolean ai,
        boolean invulnerable,
        int pigCount,
        int pigHealth,
        int bodyCount,
        int damage,
        int selfPercent,
        Set<ExamAttack> allowedAttacks) {

    /** 判定分类：击杀 / 存活 / 治疗目标 / 治疗自己 / 保护猪 / 撑过攻击 / 无考试。 */
    public enum Kind {
        KILL,
        SURVIVE,
        HEAL_TARGET,
        SELF_HEAL,
        PROTECT_PIG,
        OUTLAST,
        NO_EXAM
    }

    /** 无考试（闪光的任何请求、普通防御阵、技能、等级越界）。 */
    public static final ExamSpec NO_EXAM =
            new ExamSpec(Kind.NO_EXAM, 0, 0, 0, false, false, 0, 0, 0, 0, 0, Set.of());

    public ExamSpec {
        allowedAttacks = allowedAttacks == null ? Set.of() : Set.copyOf(allowedAttacks);
    }

    /** 普通（natural/fire/ice/lightning/water 共用）：4 模式 × 5 级。 */
    private static final int[][] NATURAL_KILLS = {
            // 行 = 模式（点式 / 单发线式 / 持续线式 / 对群攻击阵），列 = I..V
            {1, 1, 2, 5, 10},
            {1, 1, 2, 5, 10},
            {1, 1, 1, 2, 5},
            {1, 1, 2, 5, 10}
    };

    private static final int[][] NATURAL_HEALTH = {
            {30, 50, 50, 30, 50},
            {30, 50, 50, 30, 50},
            {20, 30, 50, 50, 50},
            {20, 30, 30, 30, 50}
    };

    /**
     * 查表：类型 + 模式 + 具体项 + 目标等级。
     *
     * @param level 1~5；越界或三项不匹配都返回 {@link #NO_EXAM}
     */
    public static ExamSpec of(MagicType type, CastMode mode, CastSubMode sub, int level) {
        if (type == null || mode == null || level < 1 || level > 5) {
            return NO_EXAM;
        }
        if (mode == CastMode.SKILL || sub == CastSubMode.FORMATION_OUTWARD_DEFENSE) {
            return NO_EXAM;
        }
        if (type == MagicType.FLASH || type == MagicType.ORIGIN || type == MagicType.PURIFICATION) {
            return NO_EXAM;
        }
        if (type == MagicType.HEAL) {
            return heal(mode, sub, level);
        }
        return natural(mode, sub, level);
    }

    /** 普通四模式；类型本身不参与（等价类共用一套）。 */
    private static ExamSpec natural(CastMode mode, CastSubMode sub, int level) {
        int row = modeRow(mode, sub);
        if (row < 0) {
            return NO_EXAM;
        }
        int index = level - 1;
        int kills = NATURAL_KILLS[row][index];
        int health = NATURAL_HEALTH[row][index];
        boolean ai = level >= 2;
        // I 级没有 AI、也不会攻击，允许攻击集合留空（草案 §9.8 只在 II 级起写“仅会尝试…”）
        return kill(kills, health, ai, ai ? allowedFor(row) : Set.of());
    }

    /** 治疗五模式。 */
    private static ExamSpec heal(CastMode mode, CastSubMode sub, int level) {
        int index = level - 1;
        if (mode == CastMode.POINT) {
            return healByMode(0, index);
        }
        if (mode == CastMode.LINE && sub == CastSubMode.LINE_SINGLE) {
            return healByMode(1, index);
        }
        if (mode == CastMode.LINE && sub == CastSubMode.LINE_SUSTAINED) {
            return healByMode(2, index);
        }
        if (mode == CastMode.FORMATION && sub == CastSubMode.FORMATION_SELF_HEAL) {
            return selfHeal(index);
        }
        if (mode == CastMode.FORMATION && sub == CastSubMode.FORMATION_GROUP_HEAL) {
            return healByMode(3, index);
        }
        return NO_EXAM;
    }

    /** 模式行号：点式 0 / 单发线式 1 / 持续线式 2 / 对群攻击阵 3；其它返回 -1。 */
    private static int modeRow(CastMode mode, CastSubMode sub) {
        if (mode == CastMode.POINT) {
            return sub == null ? 0 : -1;
        }
        if (mode == CastMode.LINE && sub == CastSubMode.LINE_SINGLE) {
            return 1;
        }
        if (mode == CastMode.LINE && sub == CastSubMode.LINE_SUSTAINED) {
            return 2;
        }
        if (mode == CastMode.FORMATION && sub == CastSubMode.FORMATION_GROUP_ATTACK) {
            return 3;
        }
        return -1;
    }

    /** 普通：II 级起按行给“允许的攻击方式”（只增不减，草案 §9.8）。 */
    private static Set<ExamAttack> allowedFor(int row) {
        EnumSet<ExamAttack> set = EnumSet.of(ExamAttack.POINT);
        if (row >= 1) {
            set.add(ExamAttack.LINE_SINGLE);
        }
        if (row >= 2) {
            set.add(ExamAttack.LINE_SUSTAINED);
        }
        if (row >= 3) {
            set.add(ExamAttack.FORMATION_GROUP_ATTACK);
        }
        return set;
    }

    private static ExamSpec kill(int kills, int health, boolean ai, Set<ExamAttack> allowed) {
        return new ExamSpec(Kind.KILL, kills, health, 0, ai, false, 0, 0, 0, 0, 0, allowed);
    }

    /**
     * 治疗四模式（点式 / 单发 / 持续 / 对群），行号同普通但数值不同。
     * I = 治疗目标；II~V = 保护猪存活。
     */
    private static ExamSpec healByMode(int row, int index) {
        if (index == 0) {
            // 点式 / 单发：20 血扣 10；持续 / 对群：50 血扣 40（草案 §9.8）
            boolean small = row == 0 || row == 1;
            return new ExamSpec(Kind.HEAL_TARGET, 0, small ? 20 : 50, 0, false, false,
                    0, 0, 1, small ? 10 : 40, 0, Set.of());
        }
        int seconds = switch (index) {
            case 1 -> 60;
            case 2, 3 -> 120;
            default -> 180;
        };
        int pigCount;
        int bodyCount;
        int pigHealth = row == 3 ? 50 : 30;
        if (index == 3) {
            // IV：点式/单发/持续 = 1 猪 + 2 魔法体；对群 = 2 猪 + 2 魔法体
            pigCount = row == 3 ? 2 : 1;
            bodyCount = 2;
        } else if (index == 4) {
            // V：点式/单发/持续 = 2 猪 + 3 魔法体；对群 = 3 猪 + 5 魔法体
            pigCount = row == 3 ? 3 : 2;
            bodyCount = row == 3 ? 5 : 3;
        } else {
            pigCount = 1;
            bodyCount = 1;
        }
        return new ExamSpec(Kind.PROTECT_PIG, 0, 20, seconds, true, false,
                pigCount, pigHealth, bodyCount, 0, 0, allowedFor(row));
    }

    /** 对自己的治疗魔法阵：I = 扣 50% 后自行回满；II~V = 在攻击下存活。 */
    private static ExamSpec selfHeal(int index) {
        if (index == 0) {
            return new ExamSpec(Kind.SELF_HEAL, 0, 0, 0, false, false, 0, 0, 0, 0, 50, Set.of());
        }
        int seconds = switch (index) {
            case 1 -> 60;
            case 2, 3 -> 120;
            default -> 180;
        };
        int bodyCount = switch (index) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 5;
            default -> 10;
        };
        boolean invulnerable = index <= 2;
        int health = index <= 2 ? 20 : 100;
        return new ExamSpec(Kind.OUTLAST, 0, health, seconds, true, invulnerable,
                0, 0, bodyCount, 0, 0, allowedFor(0));
    }

    /** 是否“无考试”；用于申请书校验与提示。 */
    public boolean isNoExam() {
        return this.kind == Kind.NO_EXAM;
    }

    /** 需要生成的目标魔法体数量（KILL 用 {@link #kills}）。 */
    public int spawnBodyCount() {
        return this.kind == Kind.KILL ? this.kills : this.bodyCount;
    }
}
