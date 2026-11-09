package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.container.MagicStudyLecternMenu;
import com.mugewrayce.sizeadjuster.magic.CastMode;
import com.mugewrayce.sizeadjuster.magic.CastSubMode;
import com.mugewrayce.sizeadjuster.magic.LecternRules;
import com.mugewrayce.sizeadjuster.magic.MagicSkill;
import com.mugewrayce.sizeadjuster.magic.MagicType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 魔法学习讲台 GUI（W11）：41 格 = 5 专用 + 36 真背包；背景内容区 238×248。
 *
 * <p>布局按草案 §10.5：标题 (8,8)、顶部三键 y=20、块① y=37/50、块② y=68/82/98、
 * 块③ y=116/126、红字 (38,144)、物品栏 y=166/184/202、快捷栏 y=224。</p>
 *
 * <p>客户端<b>不维护选中状态</b>：所有按钮的按下/不可选/可见性与红字都从
 * {@link MagicStudyLecternMenu} 的 {@code ContainerData} 回读（handoff §2.10）；
 * 点击只发 buttonId，服务端重算后回读。</p>
 */
public class MagicStudyLecternScreen extends AbstractModScreen<MagicStudyLecternMenu> {

    private static final String LANG = "gui.mugewrayce-mod.magic_study_lectern.";

    public static final Identifier BACKGROUND = id("textures/gui/container/magic_study_lectern.png");
    public static final Identifier BIG_NORMAL =
            id("textures/gui/container/magic_study_lectern_big_normal.png");
    public static final Identifier BIG_PRESSED =
            id("textures/gui/container/magic_study_lectern_big_pressed.png");
    public static final Identifier BIG_DISABLED =
            id("textures/gui/container/magic_study_lectern_big_disabled.png");
    public static final Identifier MID_NORMAL =
            id("textures/gui/container/magic_study_lectern_mid_normal.png");
    public static final Identifier MID_PRESSED =
            id("textures/gui/container/magic_study_lectern_mid_pressed.png");
    public static final Identifier MID_DISABLED =
            id("textures/gui/container/magic_study_lectern_mid_disabled.png");
    public static final Identifier SMALL_NORMAL =
            id("textures/gui/container/magic_study_lectern_small_normal.png");
    public static final Identifier SMALL_PRESSED =
            id("textures/gui/container/magic_study_lectern_small_pressed.png");
    public static final Identifier SMALL_DISABLED =
            id("textures/gui/container/magic_study_lectern_small_disabled.png");

    public static final Identifier ADVANCED_BACKGROUND =
            id("textures/gui/container/advanced_magic_study_lectern.png");
    public static final Identifier ADVANCED_BIG_NORMAL =
            id("textures/gui/container/advanced_magic_study_lectern_big_normal.png");
    public static final Identifier ADVANCED_BIG_PRESSED =
            id("textures/gui/container/advanced_magic_study_lectern_big_pressed.png");
    public static final Identifier ADVANCED_BIG_DISABLED =
            id("textures/gui/container/advanced_magic_study_lectern_big_disabled.png");
    public static final Identifier ADVANCED_MID_NORMAL =
            id("textures/gui/container/advanced_magic_study_lectern_mid_normal.png");
    public static final Identifier ADVANCED_MID_PRESSED =
            id("textures/gui/container/advanced_magic_study_lectern_mid_pressed.png");
    public static final Identifier ADVANCED_MID_DISABLED =
            id("textures/gui/container/advanced_magic_study_lectern_mid_disabled.png");
    public static final Identifier ADVANCED_SMALL_NORMAL =
            id("textures/gui/container/advanced_magic_study_lectern_small_normal.png");
    public static final Identifier ADVANCED_SMALL_PRESSED =
            id("textures/gui/container/advanced_magic_study_lectern_small_pressed.png");
    public static final Identifier ADVANCED_SMALL_DISABLED =
            id("textures/gui/container/advanced_magic_study_lectern_small_disabled.png");

    /** 讲台 / 高级讲台两套皮肤（背景 + 9 张按钮图 + 标题键）。 */
    public enum LecternSkin {
        LECTERN("container.mugewrayce-mod.magic_study_lectern",
                BACKGROUND, BIG_NORMAL, BIG_PRESSED, BIG_DISABLED,
                MID_NORMAL, MID_PRESSED, MID_DISABLED,
                SMALL_NORMAL, SMALL_PRESSED, SMALL_DISABLED),
        ADVANCED("container.mugewrayce-mod.advanced_magic_study_lectern",
                ADVANCED_BACKGROUND, ADVANCED_BIG_NORMAL, ADVANCED_BIG_PRESSED,
                ADVANCED_BIG_DISABLED, ADVANCED_MID_NORMAL, ADVANCED_MID_PRESSED,
                ADVANCED_MID_DISABLED, ADVANCED_SMALL_NORMAL, ADVANCED_SMALL_PRESSED,
                ADVANCED_SMALL_DISABLED);

        private final String titleKey;
        private final Identifier background;
        private final Identifier bigNormal;
        private final Identifier bigPressed;
        private final Identifier bigDisabled;
        private final Identifier midNormal;
        private final Identifier midPressed;
        private final Identifier midDisabled;
        private final Identifier smallNormal;
        private final Identifier smallPressed;
        private final Identifier smallDisabled;

        LecternSkin(String titleKey, Identifier background,
                    Identifier bigNormal, Identifier bigPressed, Identifier bigDisabled,
                    Identifier midNormal, Identifier midPressed, Identifier midDisabled,
                    Identifier smallNormal, Identifier smallPressed, Identifier smallDisabled) {
            this.titleKey = titleKey;
            this.background = background;
            this.bigNormal = bigNormal;
            this.bigPressed = bigPressed;
            this.bigDisabled = bigDisabled;
            this.midNormal = midNormal;
            this.midPressed = midPressed;
            this.midDisabled = midDisabled;
            this.smallNormal = smallNormal;
            this.smallPressed = smallPressed;
            this.smallDisabled = smallDisabled;
        }

        public String titleKey() {
            return this.titleKey;
        }

        public Identifier background() {
            return this.background;
        }
    }

    private static final int TITLE_COLOR = 0xFF8A0C73;
    private static final int TITLE_SHADOW = 0xFFD9A8CE;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int HINT_COLOR = 0xFFC1002F;
    private static final int HINT_WIDTH = 192;

    private final TriStateButton[] typeButtons = new TriStateButton[LecternRules.TYPE_PAGE_SIZE];
    private final TriStateButton[] majorButtons = new TriStateButton[4];
    private final TriStateButton[] subButtons = new TriStateButton[LecternRules.SUB_PAGE_SIZE];
    private final TriStateButton[] levelButtons = new TriStateButton[5];

    private TriStateButton applyButton;
    private TriStateButton updateButton;
    private TriStateButton examButton;
    private TriStateButton confirmButton;
    private TriStateButton typePrevButton;
    private TriStateButton typeNextButton;
    private TriStateButton subPrevButton;
    private TriStateButton subNextButton;

    private int[] lastData;
    private final LecternSkin skin;

    public MagicStudyLecternScreen(MagicStudyLecternMenu menu, Inventory playerInventory,
                                   Component title) {
        this(menu, playerInventory, title, LecternSkin.LECTERN);
    }

    public MagicStudyLecternScreen(MagicStudyLecternMenu menu, Inventory playerInventory,
                                   Component title, LecternSkin skin) {
        super(menu, playerInventory, title, 238, 248, skin.background());
        this.skin = skin;
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelX = 39;
    }

    // ------------------------------------------------------------------
    // 创建控件
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();

        this.applyButton = this.triButton(38, 20, 54, 12,
                Component.translatable(LANG + "apply"), true,
                () -> this.sendButton(MagicStudyLecternMenu.BUTTON_APPLY));
        this.updateButton = this.triButton(98, 20, 54, 12,
                Component.translatable(LANG + "update"), true,
                () -> this.sendButton(MagicStudyLecternMenu.BUTTON_UPDATE));
        this.examButton = this.triButton(158, 20, 54, 12,
                Component.translatable(LANG + "exam"), true,
                () -> this.sendButton(MagicStudyLecternMenu.BUTTON_EXAM));

        this.confirmButton = this.triButton(206, 142, 24, 12,
                Component.translatable(LANG + "confirm"), false,
                () -> this.sendButton(MagicStudyLecternMenu.BUTTON_CONFIRM));

        for (int i = 0; i < this.typeButtons.length; i++) {
            int indexOnPage = i;
            this.typeButtons[i] = this.triButton(38 + 26 * i, 50, 24, 12,
                    Component.empty(), true,
                    () -> this.sendButton(MagicStudyLecternMenu.BUTTON_TYPE_BASE + indexOnPage));
        }
        this.typePrevButton = this.triButton(186, 35, 12, 12,
                Component.literal("<"), false,
                () -> this.sendButton(MagicStudyLecternMenu.BUTTON_TYPE_PREV));
        this.typeNextButton = this.triButton(200, 35, 12, 12,
                Component.literal(">"), false,
                () -> this.sendButton(MagicStudyLecternMenu.BUTTON_TYPE_NEXT));

        for (int i = 0; i < this.majorButtons.length; i++) {
            int index = i;
            this.majorButtons[i] = this.triButton(38 + 26 * i, 82, 24, 12,
                    Component.empty(), true,
                    () -> this.sendButton(MagicStudyLecternMenu.BUTTON_MAJOR_BASE + index));
        }

        for (int i = 0; i < this.subButtons.length; i++) {
            int position = i;
            this.subButtons[i] = this.triButton(38 + 56 * i, 98, 54, 12,
                    Component.empty(), true,
                    () -> this.sendButton(MagicStudyLecternMenu.BUTTON_SUB_BASE
                            + this.subIndexAt(position)));
        }
        this.subPrevButton = this.triButton(186, 82, 12, 12,
                Component.literal("<"), false,
                () -> this.sendButton(MagicStudyLecternMenu.BUTTON_SUB_PREV));
        this.subNextButton = this.triButton(200, 82, 12, 12,
                Component.literal(">"), false,
                () -> this.sendButton(MagicStudyLecternMenu.BUTTON_SUB_NEXT));

        for (int i = 0; i < this.levelButtons.length; i++) {
            int level = i + 1;
            this.levelButtons[i] = this.triButton(38 + 16 * i, 126, 12, 12,
                    Component.literal(roman(level)), true,
                    () -> this.sendButton(MagicStudyLecternMenu.BUTTON_LEVEL_BASE + level - 1));
        }

        this.lastData = null;
        this.refreshControls();
    }

    private TriStateButton triButton(int guiX, int guiY, int width, int height, Component message,
                                     boolean toggle, Runnable onClick) {
        TriStateButton button = new TriStateButton(
                this.leftPos + guiX, this.topPos + guiY, width, height, message,
                textureFor(width, false), textureFor(width, true), textureForDisabled(width),
                () -> {
                });
        button.setToggleMode(toggle);
        if (toggle) {
            button.setOnPress(onClick);
        } else {
            // 点按式：按下只换贴图，松开才执行（交接单 §1.4）。
            button.setOnRelease(onClick);
        }
        this.addRenderableWidget(button);
        return button;
    }

    private Identifier textureFor(int width, boolean pressed) {
        if (width >= 54) {
            return pressed ? this.skin.bigPressed : this.skin.bigNormal;
        }
        if (width >= 24) {
            return pressed ? this.skin.midPressed : this.skin.midNormal;
        }
        return pressed ? this.skin.smallPressed : this.skin.smallNormal;
    }

    private Identifier textureForDisabled(int width) {
        if (width >= 54) {
            return this.skin.bigDisabled;
        }
        if (width >= 24) {
            return this.skin.midDisabled;
        }
        return this.skin.smallDisabled;
    }

    // ------------------------------------------------------------------
    // 每 tick 从 ContainerData 回读
    // ------------------------------------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        int[] snapshot = new int[MagicStudyLecternMenu.DATA_SIZE];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = this.menu.getDataValue(i);
        }
        if (this.lastData == null || !java.util.Arrays.equals(snapshot, this.lastData)) {
            this.lastData = snapshot;
            this.refreshControls();
        }
    }

    private void refreshControls() {
        int mode = this.menu.getMode();
        MagicType type = this.menu.getSelectedType();
        CastMode major = this.menu.getSelectedMajor();
        CastSubMode sub = this.menu.getSelectedSub();
        int level = this.menu.getLevel();
        int skillMask = this.menu.getSkillMask();
        boolean update = mode == MagicStudyLecternMenu.MODE_UPDATE;
        boolean exam = mode == MagicStudyLecternMenu.MODE_EXAM;

        // 顶部三键：始终显示；再按弹起 / 互斥由服务端单一 mode 字段保证。
        this.applyButton.setToggled(mode == MagicStudyLecternMenu.MODE_APPLY);
        this.updateButton.setToggled(update);
        this.examButton.setToggled(exam);
        if (this.menu.isUpdateAvailable()) {
            this.updateButton.setAvailable();
        } else {
            this.updateButton.setUnavailable(Component.translatable(LANG + "hint.cert_missing"));
        }
        if (this.menu.isCanConfirm()) {
            this.confirmButton.setAvailable();
        } else {
            this.confirmButton.setUnavailable(this.unavailableConfirmReason(mode, type, major, sub));
        }

        // 块① 魔法类型
        boolean block1 = mode != MagicStudyLecternMenu.MODE_NONE;
        int typePage = this.menu.getTypePage();
        List<MagicType> types = LecternRules.types();
        int typeIndex = this.menu.getSelectedTypeIndex();
        for (int i = 0; i < this.typeButtons.length; i++) {
            int global = typePage * LecternRules.TYPE_PAGE_SIZE + i;
            boolean visible = block1 && global < types.size();
            TriStateButton button = this.typeButtons[i];
            button.visible = visible;
            if (!visible) {
                continue;
            }
            MagicType candidate = types.get(global);
            button.setMessage(Component.translatable(candidate.langKey()));
            button.setToggled(typeIndex == global);
            MagicType conflict = update ? this.menu.getConflictFor(candidate) : null;
            if (conflict != null) {
                button.setUnavailable(Component.translatable(LANG + "hint.conflict",
                        Component.translatable(conflict.langKey())));
            } else {
                button.setAvailable();
            }
        }
        this.typePrevButton.visible = block1;
        this.typeNextButton.visible = block1;
        if (block1) {
            boolean single = LecternRules.typePageCount() <= 1;
            if (single) {
                this.typePrevButton.setUnavailable(Component.empty());
                this.typeNextButton.setUnavailable(Component.empty());
            } else {
                this.typePrevButton.setAvailable();
                this.typeNextButton.setAvailable();
            }
        }

        // 块② 魔法使用模式（两级）
        boolean block2 = exam ? type != null : (update && this.menu.isSelectedTypeLearned());
        List<CastMode> majors = exam ? LecternRules.examMajorsFor(type)
                : LecternRules.majorsFor(type);
        for (int i = 0; i < this.majorButtons.length; i++) {
            boolean visible = block2 && i < majors.size();
            TriStateButton button = this.majorButtons[i];
            button.visible = visible;
            if (!visible) {
                continue;
            }
            CastMode candidate = majors.get(i);
            button.setMessage(Component.translatable(candidate.langKey()));
            button.setToggled(major == candidate);
            button.setAvailable();
        }

        boolean skillRow = block2 && major == CastMode.SKILL;
        boolean subRow = block2 && major != null && major != CastMode.SKILL;
        List<CastSubMode> subs = subRow ? LecternRules.subsFor(type, major) : List.of();
        List<MagicSkill> skills = skillRow ? LecternRules.skillsFor(type) : List.of();
        int subPage = subRow ? this.menu.getSubPage(major) : 0;
        for (int i = 0; i < this.subButtons.length; i++) {
            TriStateButton button = this.subButtons[i];
            int global = subPage * LecternRules.SUB_PAGE_SIZE + i;
            boolean visible = skillRow ? global < skills.size()
                    : subRow && global < subs.size();
            button.visible = visible;
            if (!visible) {
                continue;
            }
            if (skillRow) {
                MagicSkill skill = skills.get(global);
                button.setMessage(Component.translatable(skill.langKey()));
                button.setToggled((skillMask & (1 << skill.ordinal())) != 0);
            } else {
                CastSubMode candidate = subs.get(global);
                button.setMessage(Component.translatable(candidate.langKey()));
                button.setToggled(sub == candidate);
            }
            button.setAvailable();
        }
        this.subPrevButton.visible = subRow;
        this.subNextButton.visible = subRow;
        if (subRow) {
            boolean single = LecternRules.subPageCount(type, major) <= 1;
            if (single) {
                this.subPrevButton.setUnavailable(Component.empty());
                this.subNextButton.setUnavailable(Component.empty());
            } else {
                this.subPrevButton.setAvailable();
                this.subNextButton.setAvailable();
            }
        }

        // 块③ 等级：考试模式选好大类后出现；高级讲台在「更新」模式下也出现。
        boolean block3 = exam
                ? major != null && (sub != null || LecternRules.examSubsFor(type, major).isEmpty())
                : (this.menu.isAdvanced() && update);
        for (int i = 0; i < this.levelButtons.length; i++) {
            TriStateButton button = this.levelButtons[i];
            button.visible = block3;
            if (!block3) {
                continue;
            }
            button.setToggled(level == i + 1);
            button.setAvailable();
        }
    }

    private Component unavailableConfirmReason(int mode, MagicType type, CastMode major,
                                               CastSubMode sub) {
        if (mode == MagicStudyLecternMenu.MODE_NONE) {
            return Component.translatable(LANG + "hint.no_mode");
        }
        if (type == null) {
            return Component.translatable(LANG + "hint.no_type");
        }
        if (mode == MagicStudyLecternMenu.MODE_EXAM) {
            if (LecternRules.examMajorsFor(type).isEmpty()) {
                return Component.translatable(LANG + "hint.no_exam_content");
            }
            if (major == null) {
                return Component.translatable(LANG + "hint.no_category");
            }
            return Component.translatable(LANG + "hint.no_sub");
        }
        return Component.empty();
    }

    private int subIndexAt(int position) {
        CastMode major = this.menu.getSelectedMajor();
        if (major == null) {
            return position;
        }
        int page = this.menu.getSubPage(major);
        return page * LecternRules.SUB_PAGE_SIZE + position;
    }

    // ------------------------------------------------------------------
    // 绘制
    // ------------------------------------------------------------------

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component shownTitle = Component.translatable(this.skin.titleKey());
        graphics.text(this.font, shownTitle, this.titleLabelX + 1, this.titleLabelY + 1,
                TITLE_SHADOW, false);
        graphics.text(this.font, shownTitle, this.titleLabelX, this.titleLabelY,
                TITLE_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int mode = this.menu.getMode();
        if (mode != MagicStudyLecternMenu.MODE_NONE) {
            this.text(graphics, Component.translatable(LANG + "magic_type"), 38, 37);
        }
        MagicType type = this.menu.getSelectedType();
        CastMode major = this.menu.getSelectedMajor();
        CastSubMode sub = this.menu.getSelectedSub();
        boolean update = mode == MagicStudyLecternMenu.MODE_UPDATE;
        boolean exam = mode == MagicStudyLecternMenu.MODE_EXAM;
        boolean block2 = exam ? type != null : (update && this.menu.isSelectedTypeLearned());
        if (block2) {
            this.text(graphics, Component.translatable(LANG + "cast_mode"), 38, 68);
        }
        boolean block3 = exam
                ? major != null && (sub != null || LecternRules.examSubsFor(type, major).isEmpty())
                : (this.menu.isAdvanced() && update);
        if (block3) {
            this.text(graphics, Component.translatable(LANG + "level"), 38, 116);
        }

        Component hint = this.hintComponent();
        if (hint != null) {
            this.drawHint(graphics, hint);
        }
    }

    private void text(GuiGraphicsExtractor graphics, Component text, int guiX, int guiY) {
        graphics.text(this.font, text, this.leftPos + guiX, this.topPos + guiY, TITLE_COLOR, false);
    }

    private void drawHint(GuiGraphicsExtractor graphics, Component hint) {
        int x = this.leftPos + 8;
        int y = this.topPos + 144;
        int width = this.font.width(hint);
        if (width <= HINT_WIDTH) {
            graphics.text(this.font, hint, x, y, HINT_COLOR, false);
            return;
        }
        float scale = Math.max(0.8F, (float) HINT_WIDTH / width);
        if (width * scale <= HINT_WIDTH) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale, scale);
            graphics.text(this.font, hint, 0, 0, HINT_COLOR, false);
            graphics.pose().popMatrix();
            return;
        }
        // 0.8 倍仍超宽：按 240px 折成两行（缩放后每行 ≤192px）。
        List<FormattedCharSequence> lines = this.font.split(hint, (int) (HINT_WIDTH / scale));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        for (int i = 0; i < lines.size() && i < 2; i++) {
            graphics.text(this.font, lines.get(i), 0, i * 12, HINT_COLOR, false);
        }
        graphics.pose().popMatrix();
    }

    private Component hintComponent() {
        int hint = this.menu.getHintCode();
        if (hint == LecternRules.HINT_NONE) {
            return null;
        }
        if (hint == LecternRules.HINT_CONFLICT) {
            MagicType conflict = this.menu.getConflictingType();
            MagicType fallback = this.menu.getSelectedType();
            MagicType shown = conflict != null ? conflict : fallback;
            return Component.translatable(LANG + "hint.conflict", shown == null
                    ? Component.empty() : Component.translatable(shown.langKey()));
        }
        String key = switch (hint) {
            case LecternRules.HINT_NO_MODE -> "hint.no_mode";
            case LecternRules.HINT_NO_TYPE -> "hint.no_type";
            case LecternRules.HINT_NO_CATEGORY -> "hint.no_category";
            case LecternRules.HINT_NO_SUB -> "hint.no_sub";
            case LecternRules.HINT_OUTPUT_OCCUPIED -> "hint.output_occupied";
            case LecternRules.HINT_CERT_MISSING -> "hint.cert_missing";
            case LecternRules.HINT_CERT_INVALID -> "hint.cert_invalid";
            case LecternRules.HINT_ALREADY_LEARNED -> "hint.already_learned";
            case LecternRules.HINT_NEED_PROOF -> "hint.need_proof";
            case LecternRules.HINT_PROOF_MISMATCH -> "hint.proof_mismatch";
            case LecternRules.HINT_PROOF_NOT_HIGHER -> "hint.proof_not_higher";
            case LecternRules.HINT_LINE_REQUIRES_POINT -> "hint.line_requires_point";
            case LecternRules.HINT_FORMATION_REQUIRES_LINE -> "hint.formation_requires_line";
            case LecternRules.HINT_SKILL_LEARNED -> "hint.skill_learned";
            case LecternRules.HINT_NO_EXAM_CONTENT -> "hint.no_exam_content";
            case LecternRules.HINT_NUMBER_FAILED -> "hint.number_failed";
            case LecternRules.HINT_SKILL_NONE -> "hint.skill_none";
            case LecternRules.HINT_NOTHING -> "hint.nothing";
            case LecternRules.HINT_CERT_SLOT_ONLY -> "hint.cert_slot_only";
            case LecternRules.HINT_PROOF_SLOT_ONLY -> "hint.proof_slot_only";
            case LecternRules.HINT_OUTPUT_ONLY -> "hint.output_only";
            default -> null;
        };
        return key == null ? null : Component.translatable(LANG + key);
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "";
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("mugewrayce-mod", path);
    }
}
