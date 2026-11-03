package com.mugewrayce.sizeadjuster.magic;

/**
 * 2.0.0 具体模式 / 子模式（草案 §6.2 类型表第二行，§15.5「具体项」）。
 *
 * <p>只列<b>有子模式</b>的三类：线式（单发 / 持续）、魔法阵（对群攻击 / 普通防御 / 对己治疗 / 对群治疗）；
 * 点式与技能没有子模式，直接用 {@link CastMode} 表达。</p>
 *
 * <p>每个常量都绑定 {@link CastMode} 父模式，界面按键"第二行"就是按父模式过滤出来的。</p>
 */
public enum CastSubMode {

    LINE_SINGLE("line_single", "单发线式", "Single-shot Line", "Single-shot Line Mode",
            CastMode.LINE, "sub_line1"),
    LINE_SUSTAINED("line_sustained", "持续线式", "Sustained Line", "Sustained Line Mode",
            CastMode.LINE, "sub_line2"),

    FORMATION_GROUP_ATTACK("formation_group_attack", "对群攻击", "Group Attack",
            "Group Attack Formation", CastMode.FORMATION, "sub_arr_atk"),
    FORMATION_OUTWARD_DEFENSE("formation_outward_defense", "普通防御", "Defense",
            "Outward Defense Formation", CastMode.FORMATION, "sub_arr_def"),
    FORMATION_SELF_HEAL("formation_self_heal", "对己治疗", "Self Heal",
            "Self-Healing Formation", CastMode.FORMATION, "sub_heal_self"),
    FORMATION_GROUP_HEAL("formation_group_heal", "对群治疗", "Group Heal",
            "Group Healing Formation", CastMode.FORMATION, "sub_heal_group");

    private final String id;
    private final String zhShortName;
    private final String enShortName;
    private final String enFullName;
    private final CastMode mode;
    private final String langField;

    CastSubMode(String id, String zhShortName, String enShortName, String enFullName,
                CastMode mode, String langField) {
        this.id = id;
        this.zhShortName = zhShortName;
        this.enShortName = enShortName;
        this.enFullName = enFullName;
        this.mode = mode;
        this.langField = langField;
    }

    /** 稳定 ID（小写下划线），用于存档字段，不要用 {@link #name()}。 */
    public String id() {
        return id;
    }

    public String zhShortName() {
        return zhShortName;
    }

    public String enShortName() {
        return enShortName;
    }

    /** 完整英文名：按方案①只在"说明 / 提示"文字处显示。 */
    public String enFullName() {
        return enFullName;
    }

    /** 父使用模式（线式 / 魔法阵）。 */
    public CastMode mode() {
        return mode;
    }

    /** 讲台 GUI 语言键。 */
    public String langKey() {
        return CastMode.LANG_PREFIX + langField;
    }

    /** 按稳定 ID 查找；找不到返回 {@code null}。 */
    public static CastSubMode fromId(String id) {
        for (CastSubMode sub : values()) {
            if (sub.id.equals(id)) {
                return sub;
            }
        }
        return null;
    }
}
