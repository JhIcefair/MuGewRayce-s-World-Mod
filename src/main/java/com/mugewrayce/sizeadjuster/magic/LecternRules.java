package com.mugewrayce.sizeadjuster.magic;

import com.mugewrayce.sizeadjuster.component.ApplicationData;
import com.mugewrayce.sizeadjuster.component.CertData;
import com.mugewrayce.sizeadjuster.component.CertEntry;
import com.mugewrayce.sizeadjuster.component.ProficiencyData;
import com.mugewrayce.sizeadjuster.component.PromotionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 魔法学习讲台的规则纯函数（W11，草案 §10.5 / §4.1 / §6.5~§6.7）。
 *
 * <p>本类<b>不碰 ItemStack / Level / Player</b>：菜单只负责读写槽位与发物品，
 * 所有“能不能学 / 该不该消耗证明 / 申请书里写什么”的判断都在这里，
 * 方便离线静态自检（{@code W11StaticCheck}）。</p>
 *
 * <p>三个容易做错的点（草案 §10.5-★，交接单 §1.10）：</p>
 * <ol>
 *   <li>考试不依赖学习证、所有类型可选，只过滤“技能”与“普通防御”；</li>
 *   <li>学新模式时，②格证明匹配的是<b>前置条目</b>（线式 → 点式；魔法阵 → 任意线式），
 *       不是要学的新模式；</li>
 *   <li>前置条件按“用掉证明之后的状态”模拟；成功后把证明作用在它自己绑定的条目上并消耗。</li>
 * </ol>
 */
public final class LecternRules {

    /** 讲台只列 7 种自然魔法，顺序固定（草案 §10.5 选项显示文本表）。 */
    public static final List<MagicType> TYPES = List.of(
            MagicType.NATURAL, MagicType.FIRE, MagicType.ICE, MagicType.LIGHTNING,
            MagicType.WATER, MagicType.FLASH, MagicType.HEAL);

    /** 块① 每页 7 个。 */
    public static final int TYPE_PAGE_SIZE = 7;
    /** 块② 第二级一行最多 3 个。 */
    public static final int SUB_PAGE_SIZE = 3;

    /** 初学者（0）；等级 I~V 为 1..5。 */
    public static final int LEVEL_BEGINNER = MagicDataRules.LEVEL_BEGINNER;
    public static final int LEVEL_I = MagicDataRules.LEVEL_I;
    public static final int LEVEL_MAX = MagicDataRules.LEVEL_MAX;

    // ------------------------------------------------------------------
    // hintCode（交接单 §2.4；23~25 不使用：③④ 无限格静默失败）
    // ------------------------------------------------------------------

    public static final int HINT_NONE = 0;
    public static final int HINT_NO_MODE = 1;
    public static final int HINT_NO_TYPE = 2;
    public static final int HINT_NO_CATEGORY = 3;
    public static final int HINT_NO_SUB = 4;
    public static final int HINT_OUTPUT_OCCUPIED = 5;
    public static final int HINT_CERT_MISSING = 6;
    public static final int HINT_CERT_INVALID = 7;
    public static final int HINT_CONFLICT = 8;
    public static final int HINT_ALREADY_LEARNED = 9;
    public static final int HINT_NEED_PROOF = 10;
    public static final int HINT_PROOF_MISMATCH = 11;
    public static final int HINT_PROOF_NOT_HIGHER = 12;
    public static final int HINT_LINE_REQUIRES_POINT = 13;
    public static final int HINT_FORMATION_REQUIRES_LINE = 14;
    public static final int HINT_SKILL_LEARNED = 15;
    public static final int HINT_NO_EXAM_CONTENT = 16;
    public static final int HINT_NUMBER_FAILED = 17;
    public static final int HINT_SKILL_NONE = 18;
    public static final int HINT_NOTHING = 19;
    public static final int HINT_CERT_SLOT_ONLY = 20;
    public static final int HINT_PROOF_SLOT_ONLY = 21;
    public static final int HINT_OUTPUT_ONLY = 22;

    private static final List<CastMode> MAJOR_ORDER = List.of(
            CastMode.POINT, CastMode.LINE, CastMode.FORMATION, CastMode.SKILL);

    private static final List<CastSubMode> FORMATION_ORDER = List.of(
            CastSubMode.FORMATION_GROUP_ATTACK,
            CastSubMode.FORMATION_OUTWARD_DEFENSE);

    /** 治疗魔法的魔法阵顺序按《选项显示文本表》：对己治疗、对群治疗、普通防御。 */
    private static final List<CastSubMode> HEAL_FORMATION_ORDER = List.of(
            CastSubMode.FORMATION_SELF_HEAL,
            CastSubMode.FORMATION_GROUP_HEAL,
            CastSubMode.FORMATION_OUTWARD_DEFENSE);

    private LecternRules() {
    }

    // ------------------------------------------------------------------
    // 类型 / 模式 / 等级
    // ------------------------------------------------------------------

    /** 讲台可以学习 / 考试的 7 种自然魔法。 */
    public static List<MagicType> types() {
        return TYPES;
    }

    /** 该类型是否属于讲台范围（自然魔法且已实装；起源 / 净化不进讲台）。 */
    public static boolean isLearnableType(MagicType type) {
        return type != null
                && type.system() == MagicSystem.NATURAL
                && type.isImplemented();
    }

    /** 按页取类型（块①每页 7 个）。 */
    public static List<MagicType> typePage(int page) {
        return page(TYPES, page, TYPE_PAGE_SIZE);
    }

    /** 块① 的页数（至少 1）。 */
    public static int typePageCount() {
        return pageCount(TYPES.size(), TYPE_PAGE_SIZE);
    }

    /** 该类型支持的大类（第一级），顺序固定 点式 / 线式 / 魔法阵 / 技能。 */
    public static List<CastMode> majorsFor(MagicType type) {
        if (type == null) {
            return List.of();
        }
        List<CastMode> result = new ArrayList<>();
        for (CastMode mode : MAJOR_ORDER) {
            if (type.supports(mode)) {
                result.add(mode);
            }
        }
        return List.copyOf(result);
    }

    /** 考试模式的大类：去掉“技能”（§6.6 / §10.5 过滤规则①）。 */
    public static List<CastMode> examMajorsFor(MagicType type) {
        List<CastMode> result = new ArrayList<>();
        for (CastMode mode : majorsFor(type)) {
            if (mode != CastMode.SKILL) {
                result.add(mode);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 该大类下的具体项（第二级）。点式没有第二级；线式 2 个；魔法阵按类型
     * （普通 / 火 / 冰 / 雷电 / 水 = 对群攻击 + 普通防御；治疗 = 对己 + 对群 + 普通防御）；
     * 技能 = 该类型的技能。
     */
    public static List<CastSubMode> subsFor(MagicType type, CastMode mode) {
        if (type == null || mode == null) {
            return List.of();
        }
        if (mode == CastMode.LINE) {
            List<CastSubMode> result = new ArrayList<>();
            for (CastSubMode sub : List.of(CastSubMode.LINE_SINGLE, CastSubMode.LINE_SUSTAINED)) {
                if (type.supports(sub)) {
                    result.add(sub);
                }
            }
            return List.copyOf(result);
        }
        if (mode == CastMode.FORMATION) {
            List<CastSubMode> order = type == MagicType.HEAL
                    ? HEAL_FORMATION_ORDER : FORMATION_ORDER;
            List<CastSubMode> result = new ArrayList<>();
            for (CastSubMode sub : order) {
                if (type.supports(sub)) {
                    result.add(sub);
                }
            }
            return List.copyOf(result);
        }
        return List.of();
    }

    /** 考试模式的具体项：去掉“普通防御”（§10.5 过滤规则②）。 */
    public static List<CastSubMode> examSubsFor(MagicType type, CastMode mode) {
        List<CastSubMode> result = new ArrayList<>();
        for (CastSubMode sub : subsFor(type, mode)) {
            if (sub != CastSubMode.FORMATION_OUTWARD_DEFENSE) {
                result.add(sub);
            }
        }
        return List.copyOf(result);
    }

    /** 该大类的具体项页数（第二级每页 3 个）。 */
    public static int subPageCount(MagicType type, CastMode mode) {
        return pageCount(subsFor(type, mode).size(), SUB_PAGE_SIZE);
    }

    /** 该大类的技能（块②“技能”这一行的第二级，多选）。 */
    public static List<MagicSkill> skillsFor(MagicType type) {
        if (type == null) {
            return List.of();
        }
        List<MagicSkill> result = new ArrayList<>();
        for (MagicSkill skill : MagicSkill.values()) {
            if (type.supports(skill)) {
                result.add(skill);
            }
        }
        return List.copyOf(result);
    }

    /** 某个技能是否已经学过。 */
    public static boolean isSkillLearned(CertData cert, MagicType type, MagicSkill skill) {
        return cert != null && type != null && skill != null
                && MagicDataRules.findEntry(cert, type.id(), CastMode.SKILL.id(), skill.id()).isPresent();
    }

    /** 等级 0（未选 / 初学者）到 5 的合法化。 */
    public static int clampLevel(int level) {
        return Math.max(LEVEL_BEGINNER, Math.min(LEVEL_MAX, level));
    }

    /**
     * 新条目的初始等级（W16 起恒为初学者）：讲台不再有“创造模式直接选等级”的入口；
     * 高等级只由高级讲台的更新/升级或证明/认证产生。
     */
    public static int entryLevel(MagicType type, CastMode mode, CastSubMode sub) {
        return LEVEL_BEGINNER;
    }

    // ------------------------------------------------------------------
    // 学习证
    // ------------------------------------------------------------------

    /** 申请学习证：非闪光 = 点式·初学者；闪光只有技能 = 写全部技能（§1.9 ①）。 */
    public static CertData newCert(int no, MagicType type, int level) {
        List<CertEntry> entries = new ArrayList<>();
        if (type == MagicType.FLASH) {
            for (MagicSkill skill : skillsFor(type)) {
                entries.add(new CertEntry(type.id(), CastMode.SKILL.id(), skill.id(), LEVEL_BEGINNER));
            }
        } else {
            entries.add(new CertEntry(type.id(), CastMode.POINT.id(), "", clampLevel(level)));
        }
        return new CertData(no, entries);
    }

    /** 学习证里已拥有的类型集合（按讲台类型顺序）。 */
    public static List<MagicType> ownedTypes(CertData cert) {
        List<MagicType> owned = new ArrayList<>();
        if (cert == null) {
            return owned;
        }
        for (MagicType type : TYPES) {
            if (hasType(cert, type)) {
                owned.add(type);
            }
        }
        return owned;
    }

    /** 学习证里是否已有该类型。 */
    public static boolean hasType(CertData cert, MagicType type) {
        if (cert == null || type == null) {
            return false;
        }
        for (CertEntry entry : cert.entries()) {
            if (type.id().equals(entry.type())) {
                return true;
            }
        }
        return false;
    }

    /** 追加一条记录（已存在同 类型/模式/子项 时不重复追加）。 */
    public static CertData addEntry(CertData cert, CertEntry entry) {
        if (cert == null || entry == null) {
            return cert;
        }
        if (MagicDataRules.findEntry(cert, entry.type(), entry.mode(), entry.sub()).isPresent()) {
            return cert;
        }
        List<CertEntry> entries = new ArrayList<>(cert.entries());
        entries.add(entry);
        return new CertData(cert.no(), entries);
    }

    /** 批量追加（技能一次全学）。 */
    public static CertData addEntries(CertData cert, List<CertEntry> newEntries) {
        CertData result = cert;
        if (newEntries == null) {
            return result;
        }
        for (CertEntry entry : newEntries) {
            result = addEntry(result, entry);
        }
        return result;
    }

    /** 把某条记录改成新的等级（按 类型/模式/子项 定位；找不到返回原证）。 */
    public static CertData replaceEntry(CertData cert, CertEntry target, int newLevel) {
        if (cert == null || target == null) {
            return cert;
        }
        List<CertEntry> entries = new ArrayList<>(cert.entries().size());
        for (CertEntry entry : cert.entries()) {
            if (entry.type().equals(target.type())
                    && entry.mode().equals(target.mode())
                    && entry.sub().equals(target.sub())) {
                entries.add(new CertEntry(entry.type(), entry.mode(), entry.sub(), clampLevel(newLevel)));
            } else {
                entries.add(entry);
            }
        }
        return new CertData(cert.no(), entries);
    }

    /** 学习证里该条记录（不存在返回空）。 */
    public static Optional<CertEntry> findEntry(CertData cert, MagicType type,
                                                CastMode mode, CastSubMode sub) {
        if (cert == null || type == null || mode == null) {
            return Optional.empty();
        }
        String subId = sub == null ? "" : sub.id();
        return MagicDataRules.findEntry(cert, type.id(), mode.id(), subId);
    }

    // ------------------------------------------------------------------
    // 证明 / 认证
    // ------------------------------------------------------------------

    /** ②格那张证明的引用（熟练证明或晋级认证；两者都没有 = 空）。 */
    public record ProofRef(ProficiencyData proficiency, PromotionData promotion) {

        public static final ProofRef EMPTY = new ProofRef(null, null);

        public boolean isEmpty() {
            return this.proficiency == null && this.promotion == null;
        }

        public String type() {
            return this.proficiency != null ? this.proficiency.type()
                    : this.promotion != null ? this.promotion.type() : "";
        }

        public String mode() {
            return this.proficiency != null ? this.proficiency.mode()
                    : this.promotion != null ? this.promotion.mode() : "";
        }

        public String sub() {
            return this.proficiency != null ? this.proficiency.sub()
                    : this.promotion != null ? this.promotion.sub() : "";
        }

        /** 用掉之后能升到的等级：熟练证明 → I；晋级认证 → 认证上的 targetLevel。 */
        public int targetLevel() {
            return this.proficiency != null ? LEVEL_I
                    : this.promotion != null ? this.promotion.targetLevel() : 0;
        }
    }

    /** 已学条目升级的结果。 */
    public record ApplyResult(boolean ok, int hintCode, boolean consumesProof, int newLevel) {

        public static ApplyResult ok(int newLevel) {
            return new ApplyResult(true, HINT_NONE, true, newLevel);
        }

        public static ApplyResult fail(int hintCode) {
            return new ApplyResult(false, hintCode, false, LEVEL_BEGINNER);
        }
    }

    /** 学新内容 / 学新模式的结果。 */
    public record LearnResult(boolean ok, int hintCode, boolean consumesProof,
                              CertEntry newEntry, CertEntry upgradedEntry) {

        public static LearnResult ok(CertEntry newEntry, CertEntry upgradedEntry, boolean consumesProof) {
            return new LearnResult(true, HINT_NONE, consumesProof, newEntry, upgradedEntry);
        }

        public static LearnResult fail(int hintCode) {
            return new LearnResult(false, hintCode, false, null, null);
        }
    }

    /**
     * 已学条目 + ②格证明 → 能否升级（草案 §3.22 / §3.24）。
     *
     * <p>证明与条目逐字段匹配（类型 + 模式 + 子项，不看编号）；熟练证明只到 I，
     * 晋级认证到它写明的等级；不高于当前等级（或熟练证明遇到已 ≥ I 的条目）→ 拒绝。</p>
     */
    public static ApplyResult applyProof(CertEntry entry, ProofRef proof) {
        if (entry == null) {
            return ApplyResult.fail(HINT_NEED_PROOF);
        }
        if (proof == null || proof.isEmpty()) {
            return ApplyResult.fail(HINT_NEED_PROOF);
        }
        if (!entry.type().equals(proof.type())
                || !entry.mode().equals(proof.mode())
                || !entry.sub().equals(proof.sub())) {
            return ApplyResult.fail(HINT_PROOF_MISMATCH);
        }
        if (MagicDataRules.isSkill(entry)) {
            return ApplyResult.fail(HINT_PROOF_MISMATCH);
        }
        if (proof.proficiency() != null) {
            if (entry.level() >= LEVEL_I) {
                return ApplyResult.fail(HINT_PROOF_NOT_HIGHER);
            }
            return ApplyResult.ok(LEVEL_I);
        }
        int target = proof.promotion().targetLevel();
        if (target < LEVEL_I || target > LEVEL_MAX) {
            return ApplyResult.fail(HINT_PROOF_MISMATCH);
        }
        if (target <= entry.level()) {
            return ApplyResult.fail(HINT_PROOF_NOT_HIGHER);
        }
        return ApplyResult.ok(target);
    }

    /** 按证明自己绑定的条目在学习证里找目标并升级（供纯函数自检使用）。 */
    public static ApplyResult applyProof(CertData cert, ProofRef proof) {
        if (cert == null || proof == null || proof.isEmpty()) {
            return ApplyResult.fail(HINT_NEED_PROOF);
        }
        Optional<CertEntry> target = MagicDataRules.findEntry(cert, proof.type(), proof.mode(), proof.sub());
        if (target.isEmpty()) {
            return ApplyResult.fail(HINT_PROOF_MISMATCH);
        }
        return applyProof(target.get(), proof);
    }

    /**
     * 高级讲台的无条件更新（W16）：技能条目不可升级，其余直接把等级设为选中值。
     *
     * <p>{@code selectedLevel <= 0} 表示“没有动等级”，按 W16 §1.4 保持条目当前等级，
     * 不解释成“降为初学者”。本方法不消耗②格证明。</p>
     */
    public static ApplyResult forceLevel(CertEntry entry, int selectedLevel) {
        if (entry == null) {
            return ApplyResult.fail(HINT_NEED_PROOF);
        }
        if (MagicDataRules.isSkill(entry)) {
            return ApplyResult.fail(HINT_SKILL_LEARNED);
        }
        int target = selectedLevel <= LEVEL_BEGINNER
                ? entry.level() : clampLevel(selectedLevel);
        return new ApplyResult(true, HINT_NONE, false, target);
    }

    /**
     * 高级讲台学新条目（W17）：不查前置、不读②格证明、不消耗。
     *
     * <p>新条目等级 = {@code selectedLevel > 初学者 ? selectedLevel : 初学者}；
     * 同一条目已存在 → {@link #HINT_ALREADY_LEARNED}（交给升级分支 {@link #forceLevel}）。</p>
     */
    public static LearnResult canLearnAdvanced(CertData cert, MagicType type, CastMode mode,
                                               CastSubMode sub, int selectedLevel) {
        if (cert == null || type == null || mode == null) {
            return LearnResult.fail(HINT_NO_TYPE);
        }
        String subId = sub == null ? "" : sub.id();
        if (MagicDataRules.findEntry(cert, type.id(), mode.id(), subId).isPresent()) {
            return LearnResult.fail(HINT_ALREADY_LEARNED);
        }
        int newLevel = selectedLevel > LEVEL_BEGINNER
                ? clampLevel(selectedLevel) : LEVEL_BEGINNER;
        CertEntry newEntry = new CertEntry(type.id(), mode.id(), subId, newLevel);
        return LearnResult.ok(newEntry, null, false);
    }

    // ------------------------------------------------------------------
    // 学新内容 / 学新模式（含证明模拟）
    // ------------------------------------------------------------------

    /**
     * 学一条**尚未学过**的内容（点式 / 线式 / 魔法阵；技能由菜单多选处理）。
     *
     * <p>前置条件（§6.6）：线式需要该类型「点式」≥ I；魔法阵需要该类型**任意**「线式」≥ I；
     * 点式在新类型时自动获得。前置不满足时，按②格证明匹配**前置条目**并模拟升级，
     * 模拟后满足就允许学，同时返回需要升级的条目（菜单据此消耗证明）。</p>
     */
    public static LearnResult canLearn(CertData cert, MagicType type, CastMode mode, CastSubMode sub,
                                       ProofRef proof) {
        if (cert == null || type == null || mode == null) {
            return LearnResult.fail(HINT_NO_TYPE);
        }
        String subId = sub == null ? "" : sub.id();
        if (MagicDataRules.findEntry(cert, type.id(), mode.id(), subId).isPresent()) {
            return LearnResult.fail(HINT_ALREADY_LEARNED);
        }
        int newLevel = entryLevel(type, mode, sub);
        CertEntry newEntry = new CertEntry(type.id(), mode.id(), subId, newLevel);

        if (mode == CastMode.POINT) {
            // 点式是新类型时自动给的，不应作为“新模式”单独学；真走到这里就允许写一条。
            return LearnResult.ok(newEntry, null, false);
        }
        if (mode == CastMode.SKILL) {
            return LearnResult.ok(newEntry, null, false);
        }
        if (mode == CastMode.LINE) {
            Optional<CertEntry> point = MagicDataRules.findEntry(
                    cert, type.id(), CastMode.POINT.id(), "");
            if (point.isPresent() && point.get().level() >= LEVEL_I) {
                return LearnResult.ok(newEntry, null, false);
            }
            if (point.isPresent()) {
                ApplyResult simulated = applyProof(point.get(), proof);
                if (simulated.ok()) {
                    return LearnResult.ok(newEntry, withLevel(point.get(), simulated.newLevel()), true);
                }
            }
            return LearnResult.fail(HINT_LINE_REQUIRES_POINT);
        }
        if (mode == CastMode.FORMATION) {
            List<CertEntry> lines = entriesOf(cert, type.id(), CastMode.LINE);
            for (CertEntry line : lines) {
                if (line.level() >= LEVEL_I) {
                    return LearnResult.ok(newEntry, null, false);
                }
            }
            for (CertEntry line : lines) {
                ApplyResult simulated = applyProof(line, proof);
                if (simulated.ok()) {
                    return LearnResult.ok(newEntry, withLevel(line, simulated.newLevel()), true);
                }
            }
            return LearnResult.fail(HINT_FORMATION_REQUIRES_LINE);
        }
        return LearnResult.fail(HINT_NOTHING);
    }

    /** 该类型下某个模式的所有记录。 */
    public static List<CertEntry> entriesOf(CertData cert, String typeId, CastMode mode) {
        List<CertEntry> result = new ArrayList<>();
        if (cert == null || typeId == null || mode == null) {
            return result;
        }
        for (CertEntry entry : cert.entries()) {
            if (typeId.equals(entry.type()) && mode.id().equals(entry.mode())) {
                result.add(entry);
            }
        }
        return result;
    }

    /** 生成一条等级被替换掉的记录副本。 */
    public static CertEntry withLevel(CertEntry entry, int level) {
        return new CertEntry(entry.type(), entry.mode(), entry.sub(), clampLevel(level));
    }

    // ------------------------------------------------------------------
    // 考试 / 试炼
    // ------------------------------------------------------------------

    /** 考试能否申请：类型有可考试的大类，且（大类有第二级时）具体项合法。 */
    public static boolean canExam(MagicType type, CastMode mode, CastSubMode sub) {
        if (type == null || mode == null || !examMajorsFor(type).contains(mode)) {
            return false;
        }
        List<CastSubMode> subs = examSubsFor(type, mode);
        return subs.isEmpty() || (sub != null && subs.contains(sub));
    }

    /** 考试 / 试炼申请书：等级空着按初学者处理（targetLevel=1）；I=考试、II~V=试炼。 */
    public static ApplicationData examApplication(MagicType type, CastMode mode,
                                                  CastSubMode sub, int level) {
        int target = level <= LEVEL_BEGINNER ? LEVEL_I : clampLevel(level);
        String subId = sub == null ? "" : sub.id();
        return new ApplicationData(type.id(), mode.id(), subId, target);
    }

    // ------------------------------------------------------------------
    // 内部小工具
    // ------------------------------------------------------------------

    private static <T> List<T> page(List<T> all, int page, int pageSize) {
        int pageCount = pageCount(all.size(), pageSize);
        int safe = Math.floorMod(page, pageCount);
        int from = safe * pageSize;
        int to = Math.min(all.size(), from + pageSize);
        return List.copyOf(all.subList(from, to));
    }

    private static int pageCount(int size, int pageSize) {
        if (size <= 0) {
            return 1;
        }
        return (size + pageSize - 1) / pageSize;
    }
}
