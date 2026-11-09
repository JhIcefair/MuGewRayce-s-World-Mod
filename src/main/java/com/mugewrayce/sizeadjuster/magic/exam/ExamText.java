package com.mugewrayce.sizeadjuster.magic.exam;

import com.mugewrayce.sizeadjuster.magic.CastMode;
import com.mugewrayce.sizeadjuster.magic.CastSubMode;
import com.mugewrayce.sizeadjuster.magic.MagicType;
import net.minecraft.network.chat.Component;

/**
 * 考试 / 试炼的文字拼装（W15，服务端与客户端共用）。
 *
 * <p>第一行 = 类型短名 | 具体项（没有具体项用模式短名） | 初学魔法考试 / 升N级魔法试炼。
 * 全部走已有语言键 + {@code exam_hud.*}，不新增后缀键（D-10）；与草案示例
 * 「普通自然魔法|单发线式模式|…」的措辞差异只记报告，不改键。</p>
 *
 * <p>文字一律用“可翻译 Component”，由各客户端本地化（§6 坑 15）。</p>
 */
public final class ExamText {

    public static final String LANG_PREFIX = "message.mugewrayce-mod.";
    public static final String HUD_PREFIX = "gui.mugewrayce-mod.exam_hud.";

    private ExamText() {
    }

    /** HUD 第一行 / 世界空间文字 / 聊天栏名称。 */
    public static Component firstLine(String typeId, String modeId, String subId, int targetLevel) {
        Component typeName = Component.translatable(typeLangKey(typeId));
        Component detail = Component.translatable(detailLangKey(modeId, subId));
        Component tail = targetLevel <= 1
                ? Component.translatable(HUD_PREFIX + "beginner")
                : Component.translatable(HUD_PREFIX + "trial", targetLevel);
        return Component.empty().append(typeName).append("|").append(detail).append("|").append(tail);
    }

    /** 通过条件（聊天栏第三条）。 */
    public static Component goal(ExamSpec spec) {
        return switch (spec.kind()) {
            case KILL -> Component.translatable(LANG_PREFIX + "exam_goal.kill", spec.kills(), spec.health());
            case PROTECT_PIG -> Component.translatable(LANG_PREFIX + "exam_goal.protect", spec.seconds());
            case OUTLAST, SURVIVE -> Component.translatable(LANG_PREFIX + "exam_goal.survive", spec.seconds());
            case HEAL_TARGET -> Component.translatable(LANG_PREFIX + "exam_goal.heal");
            case SELF_HEAL -> Component.translatable(LANG_PREFIX + "exam_goal.self_heal");
            case NO_EXAM -> Component.translatable("gui.mugewrayce-mod.magic_study_lectern.hint.no_exam_content");
        };
    }

    /** mm:ss（倒计时）。 */
    public static String clock(int remainingSeconds) {
        int safe = Math.max(0, remainingSeconds);
        int minutes = safe / 60;
        int seconds = safe % 60;
        return String.format(java.util.Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    private static String typeLangKey(String typeId) {
        MagicType type = MagicType.fromId(typeId);
        return type == null ? MagicType.NATURAL.langKey() : type.langKey();
    }

    private static String detailLangKey(String modeId, String subId) {
        if (subId != null && !subId.isEmpty()) {
            CastSubMode sub = CastSubMode.fromId(subId);
            if (sub != null) {
                return sub.langKey();
            }
        }
        CastMode mode = CastMode.fromId(modeId);
        return mode == null ? CastMode.POINT.langKey() : mode.langKey();
    }
}
