package com.mugewrayce.sizeadjuster.magic.exam;

import com.mugewrayce.sizeadjuster.magic.CastMode;
import com.mugewrayce.sizeadjuster.magic.CastSubMode;

/**
 * 考试 / 试炼目标实体允许使用的攻击方式（W15，交接卡 §2.4 / D-11）。
 *
 * <p>为什么不用 {@link CastMode}：内容表里的“允许攻击”是
 * {@code POINT / LINE_SINGLE / LINE_SUSTAINED / FORMATION_GROUP_ATTACK} 四个具体项，
 * 后三个是 {@link CastSubMode}，点式又没有子模式，两者无法放进同一个
 * {@code EnumSet<CastMode>}。因此在 {@code magic/exam} 内单独建枚举，
 * <b>不改冻结的 {@code CastMode}</b>（D-11）。</p>
 *
 * <p>攻击力是“目标实体打玩家/猪”的伤害，草案 §9.8 只规定攻击方式、没规定数值，
 * 这里取保守值（见 {@link #damage()}），报告中记为偏差。</p>
 */
public enum ExamAttack {

    POINT("point", CastMode.POINT, null, 2.0F),
    LINE_SINGLE("line_single", CastMode.LINE, CastSubMode.LINE_SINGLE, 2.0F),
    LINE_SUSTAINED("line_sustained", CastMode.LINE, CastSubMode.LINE_SUSTAINED, 1.0F),
    FORMATION_GROUP_ATTACK("formation_group_attack", CastMode.FORMATION,
            CastSubMode.FORMATION_GROUP_ATTACK, 1.0F);

    /** 两次攻击之间的间隔（tick）。存活类试炼要撑 1~3 分钟，所以放慢到 2 秒一击。 */
    public static final int ATTACK_INTERVAL_TICKS = 40;

    private final String id;
    private final CastMode mode;
    private final CastSubMode sub;
    private final float damage;

    ExamAttack(String id, CastMode mode, CastSubMode sub, float damage) {
        this.id = id;
        this.mode = mode;
        this.sub = sub;
        this.damage = damage;
    }

    /** 稳定 ID（与 {@link CastMode#id()} / {@link CastSubMode#id()} 同一套写法）。 */
    public String id() {
        return id;
    }

    /** 对应的使用模式（点式 / 线式 / 魔法阵）。 */
    public CastMode mode() {
        return mode;
    }

    /** 对应的具体项；点式没有具体项，返回 {@code null}。 */
    public CastSubMode sub() {
        return sub;
    }

    /** 单次攻击伤害（保守值，草案未规定）。 */
    public float damage() {
        return damage;
    }

    /** 按稳定 ID 查找；找不到返回 {@code null}。 */
    public static ExamAttack fromId(String id) {
        for (ExamAttack attack : values()) {
            if (attack.id.equals(id)) {
                return attack;
            }
        }
        return null;
    }

    /** 由（模式 + 具体项）反查；点式的 {@code sub} 传 {@code null}。 */
    public static ExamAttack of(CastMode mode, CastSubMode sub) {
        for (ExamAttack attack : values()) {
            if (attack.mode == mode && attack.sub == sub) {
                return attack;
            }
        }
        return null;
    }
}
