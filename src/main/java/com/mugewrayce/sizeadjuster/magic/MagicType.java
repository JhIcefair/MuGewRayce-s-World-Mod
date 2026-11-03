package com.mugewrayce.sizeadjuster.magic;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 2.0.0 魔法类型（草案 §6.2 类型表、§15.5 术语）。
 *
 * <p>7 种自然魔法 + 起源魔法 + 净化魔法。<b>本类只放静态定义</b>：
 * 每个类型支持哪些使用模式 / 子模式 / 技能，按草案 §6.2 的表格逐行落定；
 * 学习顺序（§6.6）、考试与试炼（§9）、伤害与治疗数值（§8）不在本包实现。</p>
 *
 * <p>要点：</p>
 * <ul>
 *   <li>闪光魔法只有技能，没有点式 / 线式 / 魔法阵，因此也没有任何考试与试炼；</li>
 *   <li>普通自然魔法不含治疗阵；治疗阵属于治疗魔法（批注 B-18）；</li>
 *   <li>起源魔法目前只有两个技能，且不能附加到法杖上（批注 B-15）；</li>
 *   <li>净化魔法本版只占类型名，没有选项、不实装、不做魔力值计算（R-1）。</li>
 * </ul>
 *
 * <p>冲突（冰 ↔ 火）不写在本枚举里，见 {@link MagicConflicts}。</p>
 */
public enum MagicType {

    NATURAL("natural", "普通", "Natural", "Natural Magic", MagicSystem.NATURAL,
            modeSet(CastMode.POINT, CastMode.LINE, CastMode.FORMATION),
            subSet(CastSubMode.LINE_SINGLE, CastSubMode.LINE_SUSTAINED,
                    CastSubMode.FORMATION_GROUP_ATTACK, CastSubMode.FORMATION_OUTWARD_DEFENSE),
            skillSet()),

    FIRE("fire", "火", "Fire", "Fire Magic", MagicSystem.NATURAL,
            modeSet(CastMode.POINT, CastMode.LINE, CastMode.FORMATION, CastMode.SKILL),
            subSet(CastSubMode.LINE_SINGLE, CastSubMode.LINE_SUSTAINED,
                    CastSubMode.FORMATION_GROUP_ATTACK, CastSubMode.FORMATION_OUTWARD_DEFENSE),
            skillSet(MagicSkill.IGNITE, MagicSkill.MELT_ICE)),

    ICE("ice", "冰", "Ice", "Ice Magic", MagicSystem.NATURAL,
            modeSet(CastMode.POINT, CastMode.LINE, CastMode.FORMATION, CastMode.SKILL),
            subSet(CastSubMode.LINE_SINGLE, CastSubMode.LINE_SUSTAINED,
                    CastSubMode.FORMATION_GROUP_ATTACK, CastSubMode.FORMATION_OUTWARD_DEFENSE),
            skillSet(MagicSkill.FREEZE, MagicSkill.MAKE_ICE)),

    LIGHTNING("lightning", "雷电", "Lightning", "Lightning Magic", MagicSystem.NATURAL,
            modeSet(CastMode.POINT, CastMode.LINE, CastMode.FORMATION, CastMode.SKILL),
            subSet(CastSubMode.LINE_SINGLE, CastSubMode.LINE_SUSTAINED,
                    CastSubMode.FORMATION_GROUP_ATTACK, CastSubMode.FORMATION_OUTWARD_DEFENSE),
            skillSet(MagicSkill.SUMMON_LIGHTNING)),

    WATER("water", "水", "Water", "Water Magic", MagicSystem.NATURAL,
            modeSet(CastMode.POINT, CastMode.LINE, CastMode.FORMATION, CastMode.SKILL),
            subSet(CastSubMode.LINE_SINGLE, CastSubMode.LINE_SUSTAINED,
                    CastSubMode.FORMATION_GROUP_ATTACK, CastSubMode.FORMATION_OUTWARD_DEFENSE),
            skillSet(MagicSkill.CREATE_WATER)),

    FLASH("flash", "闪光", "Flash", "Flash Magic", MagicSystem.NATURAL,
            modeSet(CastMode.SKILL),
            subSet(),
            skillSet(MagicSkill.FRONT_FLASH, MagicSkill.TARGETED_FLASH)),

    HEAL("heal", "治疗", "Heal", "Healing Magic", MagicSystem.NATURAL,
            modeSet(CastMode.POINT, CastMode.LINE, CastMode.FORMATION),
            subSet(CastSubMode.LINE_SINGLE, CastSubMode.LINE_SUSTAINED,
                    CastSubMode.FORMATION_SELF_HEAL, CastSubMode.FORMATION_GROUP_HEAL,
                    CastSubMode.FORMATION_OUTWARD_DEFENSE),
            skillSet()),

    ORIGIN("origin", "起源", "Origin", "Origin Magic", MagicSystem.ORIGIN,
            modeSet(CastMode.SKILL),
            subSet(),
            skillSet(MagicSkill.TIME_PAUSE, MagicSkill.SPACE_TELEPORT)),

    /** 净化魔法：类型级占位，本版没有选项、不实现效果、不做魔力值计算。 */
    PURIFICATION("purification", "净化", "Equipoise", "Equipoise Magic", MagicSystem.EQUIPOISE,
            modeSet(), subSet(), skillSet());

    /** 类型名的语言键前缀（接口约定 §4：界面文字用 gui.* 前缀）。 */
    public static final String LANG_PREFIX = "gui.mugewrayce-mod.magic_type.";

    private final String id;
    private final String zhShortName;
    private final String enShortName;
    private final String enFullName;
    private final MagicSystem system;
    private final Set<CastMode> castModes;
    private final Set<CastSubMode> subModes;
    private final Set<MagicSkill> skills;
    private final boolean implemented;

    MagicType(String id, String zhShortName, String enShortName, String enFullName,
              MagicSystem system, Set<CastMode> castModes, Set<CastSubMode> subModes,
              Set<MagicSkill> skills) {
        this.id = id;
        this.zhShortName = zhShortName;
        this.enShortName = enShortName;
        this.enFullName = enFullName;
        this.system = system;
        this.castModes = castModes;
        this.subModes = subModes;
        this.skills = skills;
        // 本版"不实装"的判定：没有任何使用模式、也没有任何技能（目前只有净化魔法）
        this.implemented = !castModes.isEmpty() || !skills.isEmpty();
    }

    /** 稳定 ID（小写下划线），用于存档字段 / 语言键后缀，不要用 {@link #name()}。 */
    public String id() {
        return id;
    }

    /** 按钮上的中文短名（草案 §6.2「界面短名」）。 */
    public String zhShortName() {
        return zhShortName;
    }

    /** 按钮上的英文短名。 */
    public String enShortName() {
        return enShortName;
    }

    /** 完整英文名：按方案①只在"说明 / 提示"文字处显示。 */
    public String enFullName() {
        return enFullName;
    }

    /** 所属体系：自然 / 起源 / 净化。 */
    public MagicSystem system() {
        return system;
    }

    /** 该类型支持的使用模式（第一行按键）。 */
    public Set<CastMode> castModes() {
        return castModes;
    }

    /** 该类型支持的全部子模式（第二行按键）。 */
    public Set<CastSubMode> subModes() {
        return subModes;
    }

    /** 指定使用模式下的子模式（第二行按键按父模式过滤）。 */
    public Set<CastSubMode> subModes(CastMode mode) {
        if (mode == null || subModes.isEmpty()) {
            return Set.of();
        }
        EnumSet<CastSubMode> filtered = EnumSet.noneOf(CastSubMode.class);
        for (CastSubMode sub : subModes) {
            if (sub.mode() == mode) {
                filtered.add(sub);
            }
        }
        return Collections.unmodifiableSet(filtered);
    }

    /** 该类型支持的技能（目前只有火 / 冰 / 雷电 / 水 / 闪光 / 起源有）。 */
    public Set<MagicSkill> skills() {
        return skills;
    }

    public boolean supports(CastMode mode) {
        return mode != null && castModes.contains(mode);
    }

    public boolean supports(CastSubMode sub) {
        return sub != null && subModes.contains(sub);
    }

    public boolean supports(MagicSkill skill) {
        return skill != null && skills.contains(skill);
    }

    public boolean hasSkills() {
        return !skills.isEmpty();
    }

    /** 本版是否实装；净化魔法返回 {@code false}（草案 §6.4）。 */
    public boolean isImplemented() {
        return implemented;
    }

    /** 类型名的语言键（接口约定 §4）。 */
    public String langKey() {
        return LANG_PREFIX + id;
    }

    /** 按稳定 ID 查找；找不到返回 {@code null}。 */
    public static MagicType fromId(String id) {
        for (MagicType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    private static Set<CastMode> modeSet(CastMode... values) {
        return values.length == 0
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(values)));
    }

    private static Set<CastSubMode> subSet(CastSubMode... values) {
        return values.length == 0
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(values)));
    }

    private static Set<MagicSkill> skillSet(MagicSkill... values) {
        return values.length == 0
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(values)));
    }
}
