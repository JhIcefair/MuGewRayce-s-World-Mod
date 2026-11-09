package com.mugewrayce.sizeadjuster.magic;

import com.mugewrayce.sizeadjuster.component.CertEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * 学习证 / 法杖条目的一行可读描述（W16）。
 *
 * <p>从 W12 的 {@code StudyCertScreen.describe()} 抽出，供界面与物品 tooltip 共用：
 * 类型 · 模式 [· 具体项] [等级]；技能不显示等级。</p>
 */
public final class CertLines {

    private CertLines() {
    }

    /** 一条学习记录：类型 · 模式 [· 具体项] [等级]；技能不显示等级。 */
    public static Component describe(CertEntry entry) {
        MutableComponent line = Component.empty();

        MagicType type = MagicType.fromId(entry.type());
        line.append(type == null
                ? Component.literal(entry.type())
                : Component.translatable(type.langKey()));

        CastMode mode = CastMode.fromId(entry.mode());
        line.append(" · ");
        line.append(mode == null
                ? Component.literal(entry.mode())
                : Component.translatable(mode.langKey()));

        if (mode == CastMode.SKILL) {
            MagicSkill skill = MagicSkill.fromId(entry.sub());
            line.append(" · ");
            if (skill == null || skill.langKey() == null) {
                line.append(Component.literal(entry.sub()));
            } else {
                line.append(Component.translatable(skill.langKey()));
            }
        } else if (mode != CastMode.POINT && entry.sub() != null && !entry.sub().isEmpty()) {
            CastSubMode sub = CastSubMode.fromId(entry.sub());
            line.append(" · ");
            line.append(sub == null
                    ? Component.literal(entry.sub())
                    : Component.translatable(sub.langKey()));
        }

        if (!MagicDataRules.isSkill(entry)) {
            MagicLevel level = MagicLevel.fromNumericLevel(entry.level());
            if (level != null) {
                line.append("  ");
                line.append(level.isBeginner()
                        ? Component.translatable(level.langKey())
                        : Component.literal(level.enName()));
            }
        }
        return line;
    }

    /** 按字段直接描述一条记录（W17：晋级认证 tooltip 用）。 */
    public static Component describe(String typeId, String modeId, String subId, int level) {
        return describe(new CertEntry(typeId, modeId, subId, level));
    }

    /**
     * 只列“类型”（W17 tooltip 精简）：按讲台类型顺序去重，用 “ / ” 连接。
     *
     * <p>学习证 / 法杖标签只显示编号 + 类型，不再逐条列模式。</p>
     */
    public static Component describeTypes(List<CertEntry> entries) {
        MutableComponent names = Component.empty();
        boolean first = true;
        for (MagicType type : LecternRules.types()) {
            if (!hasType(entries, type)) {
                continue;
            }
            if (!first) {
                names.append(" / ");
            }
            names.append(Component.translatable(type.langKey()));
            first = false;
        }
        return Component.translatable("tooltip.mugewrayce-mod.magic_types", names);
    }

    private static boolean hasType(List<CertEntry> entries, MagicType type) {
        if (entries == null) {
            return false;
        }
        for (CertEntry entry : entries) {
            if (type.id().equals(entry.type())) {
                return true;
            }
        }
        return false;
    }
}
