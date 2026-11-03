package com.mugewrayce.sizeadjuster.magic;

/**
 * 2.0.0 四大使用模式（草案 §6.3）：点式 / 线式 / 魔法阵 / 技能。
 *
 * <p>命名权威见《物品名称对照表》2.0.0「魔法体系术语」：
 * 点式 Point、线式 Line、魔法阵 <b>Formation</b>、技能 Skill。
 * 注意"魔法阵"这一<b>使用模式</b>统一用 Formation，与净化系实体
 * （Equipoise Circle / Equipoise Formation）区分开。</p>
 *
 * <p>语言键取讲台 GUI 的选项键（接口约定 §4 + 对照表"魔法学习讲台显示文本"表），
 * 前缀固定 {@code gui.mugewrayce-mod.magic_study_lectern.}。</p>
 */
public enum CastMode {

    POINT("point", "点式", "Point", "Point Mode", "mode_point"),
    LINE("line", "线式", "Line", "Line Mode", "mode_line"),
    FORMATION("formation", "魔法阵", "Formation", "Formation", "mode_formation"),
    SKILL("skill", "技能", "Skill", "Skill", "mode_skill");

    /** 讲台 GUI 选项键前缀（接口约定 §4）。 */
    public static final String LANG_PREFIX = "gui.mugewrayce-mod.magic_study_lectern.";

    private final String id;
    private final String zhShortName;
    private final String enShortName;
    private final String enFullName;
    private final String langField;

    CastMode(String id, String zhShortName, String enShortName, String enFullName, String langField) {
        this.id = id;
        this.zhShortName = zhShortName;
        this.enShortName = enShortName;
        this.enFullName = enFullName;
        this.langField = langField;
    }

    /** 稳定 ID（小写下划线），用于存档字段，不要用 {@link #name()}。 */
    public String id() {
        return id;
    }

    /** 按钮上的中文短名（草案 §6.2「界面短名」）。 */
    public String zhShortName() {
        return zhShortName;
    }

    /** 按钮上的英文短名（英文界面显示这个）。 */
    public String enShortName() {
        return enShortName;
    }

    /** 完整英文名：按方案①只在"说明 / 提示"文字处显示，不上按钮。 */
    public String enFullName() {
        return enFullName;
    }

    /** 讲台 GUI 语言键。 */
    public String langKey() {
        return LANG_PREFIX + langField;
    }

    /** 按稳定 ID 查找；找不到返回 {@code null}。 */
    public static CastMode fromId(String id) {
        for (CastMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return null;
    }
}
