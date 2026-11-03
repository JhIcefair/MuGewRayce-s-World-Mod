package com.mugewrayce.sizeadjuster.magic;

import java.util.Optional;

/**
 * 2.0.0 魔法等级（草案 §6.5）：初学者（第 1 档）、I ~ V 级。
 *
 * <p>对应关系（草案 §6.5）：I 级 = 初学魔法考试（产物是初学魔法考试申请书）；
 * II ~ V 级 = 魔法试炼（产物是魔法试炼申请书）。技能没有等级，
 * "对外的防御魔法阵"也没有等级，这两类不要套用本枚举。</p>
 *
 * <p>{@link #numericLevel()} 的口径与接口约定 §2 的组件字段一致：
 * {@code level} 0 = 初学者、1~5 = I~V。等级加成数值属 W19，不在本包。</p>
 */
public enum MagicLevel {

    BEGINNER(0, "beginner", "初学者", "Beginner", "level_beginner"),
    I(1, "i", "I", "I", null),
    II(2, "ii", "II", "II", null),
    III(3, "iii", "III", "III", null),
    IV(4, "iv", "IV", "IV", null),
    V(5, "v", "V", "V", null);

    private final int numericLevel;
    private final String id;
    private final String zhName;
    private final String enName;
    private final String langField;

    MagicLevel(int numericLevel, String id, String zhName, String enName, String langField) {
        this.numericLevel = numericLevel;
        this.id = id;
        this.zhName = zhName;
        this.enName = enName;
        this.langField = langField;
    }

    /** 0 = 初学者、1~5 = I~V（组件字段口径，接口约定 §2）。 */
    public int numericLevel() {
        return numericLevel;
    }

    /** 稳定 ID（小写下划线），用于存档字段，不要用 {@link #name()}。 */
    public String id() {
        return id;
    }

    public String zhName() {
        return zhName;
    }

    public String enName() {
        return enName;
    }

    /**
     * 讲台 GUI 语言键：只有"初学者"登记了键；I~V 直接显示罗马数字，
     * 不随语言变化（对照表「等级…I~V 直接用罗马数字」），故返回 {@code null}。
     */
    public String langKey() {
        return langField == null ? null : CastMode.LANG_PREFIX + langField;
    }

    public boolean isBeginner() {
        return this == BEGINNER;
    }

    public boolean isMax() {
        return this == V;
    }

    /** 升一级；V 已是最高级，返回 {@link Optional#empty()}。 */
    public Optional<MagicLevel> next() {
        MagicLevel[] levels = values();
        int next = ordinal() + 1;
        return next < levels.length ? Optional.of(levels[next]) : Optional.empty();
    }

    /** 按 0~5 的数值查等级；越界返回 {@code null}。 */
    public static MagicLevel fromNumericLevel(int numericLevel) {
        for (MagicLevel level : values()) {
            if (level.numericLevel == numericLevel) {
                return level;
            }
        }
        return null;
    }

    /** 按稳定 ID 查找；找不到返回 {@code null}。 */
    public static MagicLevel fromId(String id) {
        for (MagicLevel level : values()) {
            if (level.id.equals(id)) {
                return level;
            }
        }
        return null;
    }
}
