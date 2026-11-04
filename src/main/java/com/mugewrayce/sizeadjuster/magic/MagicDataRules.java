package com.mugewrayce.sizeadjuster.magic;

import com.mugewrayce.sizeadjuster.component.ApplicationData;
import com.mugewrayce.sizeadjuster.component.AttachmentData;
import com.mugewrayce.sizeadjuster.component.CertData;
import com.mugewrayce.sizeadjuster.component.CertEntry;
import com.mugewrayce.sizeadjuster.component.CrystalData;
import com.mugewrayce.sizeadjuster.component.ProficiencyData;
import com.mugewrayce.sizeadjuster.component.PromotionData;
import com.mugewrayce.sizeadjuster.component.StaffData;

import java.util.Optional;

/**
 * 组件业务判定（W06，接口约定 §2；全部纯函数）。
 *
 * <p>只做<b>服务端数据判定</b>，不做 GUI 红字 / 消耗动画 / 物品注册：
 * 证明 / 认证的"消耗"由 W15 执行，法杖"升级"由 W14/W15 执行，本类只给"能不能"。</p>
 *
 * <p>字段一律按稳定 ID 字符串匹配（{@link MagicType#id()} / {@link CastMode#id()} /
 * {@link CastSubMode#id()} / {@link MagicSkill#id()}），不依赖 enum 顺序。</p>
 */
public final class MagicDataRules {

    /** 初学者。 */
    public static final int LEVEL_BEGINNER = 0;
    /** I 级（熟练证明只能升到这一级）。 */
    public static final int LEVEL_I = 1;
    /** V 级。 */
    public static final int LEVEL_MAX = 5;

    /** 附加书等级范围 I~III。 */
    public static final int ATTACH_MIN = 1;
    public static final int ATTACH_MAX = 3;

    private MagicDataRules() {
    }

    // ------------------------------------------------------------------
    // CertEntry / 形状
    // ------------------------------------------------------------------

    /**
     * 类型 / 模式 / 子模式（或技能）三者的组合是否合法（不看等级）。
     *
     * <p>技能：{@code mode=skill}、{@code sub=MagicSkill.id()}；点式 {@code sub=""}；
     * 线式 / 魔法阵必须是对应父模式的子模式。</p>
     */
    public static boolean isValidShape(String typeId, String modeId, String subId) {
        MagicType type = MagicType.fromId(typeId);
        if (type == null || !type.isImplemented()) {
            return false;
        }
        CastMode mode = CastMode.fromId(modeId);
        if (mode == null || !type.supports(mode)) {
            return false;
        }
        if (mode == CastMode.SKILL) {
            MagicSkill skill = MagicSkill.fromId(subId);
            return skill != null && type.supports(skill);
        }
        if (subId == null || subId.isEmpty()) {
            // 只有点式允许空子模式；线式 / 魔法阵必须写具体项
            return mode == CastMode.POINT;
        }
        CastSubMode sub = CastSubMode.fromId(subId);
        return sub != null && sub.mode() == mode && type.supports(sub);
    }

    /** 学习证 / 法杖里的一条记录是否合法（含等级规则：技能必须 level=0）。 */
    public static boolean isValidEntry(CertEntry entry) {
        if (entry == null || !isValidShape(entry.type(), entry.mode(), entry.sub())) {
            return false;
        }
        if (entry.level() < LEVEL_BEGINNER || entry.level() > LEVEL_MAX) {
            return false;
        }
        return !isSkill(entry) || entry.level() == LEVEL_BEGINNER;
    }

    /** 该条记录是否技能（技能无等级、不参与证明 / 认证 / 考试）。 */
    public static boolean isSkill(CertEntry entry) {
        return entry != null && CastMode.SKILL.id().equals(entry.mode());
    }

    // ------------------------------------------------------------------
    // 学习证
    // ------------------------------------------------------------------

    /** 学习证是否有效：编号 ≥ 1、至少一条记录、每条记录合法。 */
    public static boolean isValidCert(CertData data) {
        if (data == null || data.no() < 1 || data.entries().isEmpty()) {
            return false;
        }
        for (CertEntry entry : data.entries()) {
            if (!isValidEntry(entry)) {
                return false;
            }
        }
        return true;
    }

    /** 在学习证里找 type/mode/sub 完全一致的记录（忽略等级）。 */
    public static Optional<CertEntry> findEntry(CertData cert, String type, String mode, String sub) {
        if (cert == null) {
            return Optional.empty();
        }
        for (CertEntry entry : cert.entries()) {
            if (equalsId(entry.type(), type)
                    && equalsId(entry.mode(), mode)
                    && equalsId(entry.sub(), sub)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // 熟练证明 / 晋级认证 / 申请书
    // ------------------------------------------------------------------

    /** 熟练证明是否有效（形状合法且不是技能）。 */
    public static boolean isValidProficiency(ProficiencyData data) {
        return data != null
                && isValidShape(data.type(), data.mode(), data.sub())
                && !CastMode.SKILL.id().equals(data.mode());
    }

    /** 晋级认证是否有效（形状合法、非技能、目标等级 1~5）。 */
    public static boolean isValidPromotion(PromotionData data) {
        return data != null
                && isValidShape(data.type(), data.mode(), data.sub())
                && !CastMode.SKILL.id().equals(data.mode())
                && data.targetLevel() >= LEVEL_I
                && data.targetLevel() <= LEVEL_MAX;
    }

    /** 申请书是否有效（形状合法、非技能、目标等级 1~5；1=考试、2~5=试炼）。 */
    public static boolean isValidApplication(ApplicationData data) {
        return data != null
                && isValidShape(data.type(), data.mode(), data.sub())
                && !CastMode.SKILL.id().equals(data.mode())
                && data.targetLevel() >= LEVEL_I
                && data.targetLevel() <= LEVEL_MAX;
    }

    /** 熟练证明能否用于该学习证：逐字段匹配且当前等级 < I。 */
    public static boolean proficiencyApplies(CertData cert, ProficiencyData data) {
        if (!isValidProficiency(data)) {
            return false;
        }
        return findEntry(cert, data.type(), data.mode(), data.sub())
                .filter(entry -> !isSkill(entry))
                .filter(entry -> entry.level() < LEVEL_I)
                .isPresent();
    }

    /** 晋级认证能否用于该学习证：逐字段匹配且目标等级 > 当前等级。 */
    public static boolean promotionApplies(CertData cert, PromotionData data) {
        if (!isValidPromotion(data)) {
            return false;
        }
        return findEntry(cert, data.type(), data.mode(), data.sub())
                .filter(entry -> !isSkill(entry))
                .filter(entry -> data.targetLevel() > entry.level())
                .isPresent();
    }

    // ------------------------------------------------------------------
    // 法杖
    // ------------------------------------------------------------------

    /** 法杖数据是否合法：编号非负、每条记录合法、且只允许自然魔法。 */
    public static boolean isValidStaff(StaffData data) {
        if (data == null || data.boundCertNo() < 0) {
            return false;
        }
        for (CertEntry entry : data.entries()) {
            if (!isValidEntry(entry)) {
                return false;
            }
            MagicType type = MagicType.fromId(entry.type());
            if (type == null || type.system() != MagicSystem.NATURAL) {
                return false;
            }
        }
        return true;
    }

    /**
     * 法杖能否用该学习证升级：法杖已绑定（{@code boundCertNo > 0}）、学习证有效、且编号一致。
     * 未绑定的法杖（{@code 0}）走首次附加，不适用本方法。
     */
    public static boolean canUpgradeStaff(StaffData staff, CertData cert) {
        return isValidStaff(staff)
                && isValidCert(cert)
                && staff.boundCertNo() > 0
                && staff.boundCertNo() == cert.no();
    }

    // ------------------------------------------------------------------
    // 附加书 / 水晶
    // ------------------------------------------------------------------

    /** 附加书等级是否有效（1~3）。 */
    public static boolean isValidAttachment(AttachmentData data) {
        return data != null && data.level() >= ATTACH_MIN && data.level() <= ATTACH_MAX;
    }

    /** 覆盖规则：新等级 1~3，且比当前等级高（同级 / 低级拒绝）。 */
    public static boolean canOverwriteAttachment(int currentLevel, int newLevel) {
        if (newLevel < ATTACH_MIN || newLevel > ATTACH_MAX) {
            return false;
        }
        return newLevel > currentLevel;
    }

    /** 水晶数据是否有效：魔力值 0~30000；只开自动供能（auto && !supply）是无效态。 */
    public static boolean isValidCrystal(CrystalData data) {
        if (data == null || data.mana() < 0 || data.mana() > CrystalData.CAPACITY) {
            return false;
        }
        return !(data.autoSupplyEnabled() && !data.supplyEnabled());
    }

    // ------------------------------------------------------------------

    private static boolean equalsId(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
