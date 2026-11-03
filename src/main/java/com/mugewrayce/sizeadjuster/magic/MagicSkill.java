package com.mugewrayce.sizeadjuster.magic;

/**
 * 2.0.0 技能清单（草案 §6.4）。
 *
 * <p>自然魔法 8 个技能 + 起源魔法 2 个技能。技能<b>没有等级</b>、不参与考试与试炼（草案 §6.5 / §6.6）；
 * 每个技能归哪个类型，见 {@link MagicType#skills()}。</p>
 *
 * <p>耗魔数值（点火 3/格、融化 4/格、冻结 3/格、制冰 4/格、召雷 6、生水 2/格、闪光 10、
 * 时间暂停 5/区块 + 每秒 1、空间传送 7/区块 + 3/100 格）属于 W17 魔力值结算，<b>不在本包</b>落地。</p>
 *
 * <p>语言键只登记讲台 GUI 上会出现的 8 个自然魔法技能；起源魔法的两个技能在魔法书 GUI（W16）里显示，
 * 其语言键随 W16 登记，故这里 {@code langKey()} 为 {@code null}。</p>
 */
public enum MagicSkill {

    IGNITE("ignite", "点火", "Ignite", "skill_ignite"),
    MELT_ICE("melt_ice", "融化", "Melt Ice", "skill_melt"),
    FREEZE("freeze", "冻结", "Freeze", "skill_freeze"),
    MAKE_ICE("make_ice", "制冰", "Make Ice", "skill_make_ice"),
    SUMMON_LIGHTNING("summon_lightning", "召雷", "Summon Lightning", "skill_thunder"),
    CREATE_WATER("create_water", "生水", "Create Water", "skill_water"),
    FRONT_FLASH("front_flash", "前方闪光", "Front Flash", "skill_flash_front"),
    TARGETED_FLASH("targeted_flash", "指向闪光", "Targeted Flash", "skill_flash_target"),

    // 起源魔法（与魔法书绑定，只有技能、不涉及升级）；英文名待 W16 / 对照表正式登记
    TIME_PAUSE("time_pause", "时间暂停", "Time Pause", null),
    SPACE_TELEPORT("space_teleport", "空间传送", "Space Teleport", null);

    private final String id;
    private final String zhName;
    private final String enName;
    private final String langField;

    MagicSkill(String id, String zhName, String enName, String langField) {
        this.id = id;
        this.zhName = zhName;
        this.enName = enName;
        this.langField = langField;
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

    /** 是否已有冻结的语言键（起源技能要等 W16）。 */
    public boolean hasLangKey() {
        return langField != null;
    }

    /**
     * 讲台 GUI 语言键；起源技能在本版没有登记键，返回 {@code null}。
     */
    public String langKey() {
        return langField == null ? null : CastMode.LANG_PREFIX + langField;
    }

    /** 按稳定 ID 查找；找不到返回 {@code null}。 */
    public static MagicSkill fromId(String id) {
        for (MagicSkill skill : values()) {
            if (skill.id.equals(id)) {
                return skill;
            }
        }
        return null;
    }
}
