package com.mugewrayce.sizeadjuster.magic;

import com.mugewrayce.sizeadjuster.component.CertEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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
}
