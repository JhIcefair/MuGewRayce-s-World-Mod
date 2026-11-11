package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.item.StaffCastModes;
import com.mugewrayce.sizeadjuster.magic.MagicSkill;
import com.mugewrayce.sizeadjuster.payload.StaffModePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 净化法杖设置 GUI（W12；W19 改三列；无背景）。
 *
 * <p>i 键在主手净化法杖时打开。W19 起分三列纵向排列（技能从 7 个增加到 15 个，一行放不下）：</p>
 * <pre>
 *              净化法杖设置
 *   其他            常用              自然魔法
 *  单发线式         点式               点火
 *  单体治愈魔法阵   持续线式           融化
 *  群体治愈魔法阵   群体伤害魔法阵     冻结 / 制冰 / 召雷 / 生水 / 前方闪光 / 指向闪光
 *                  [确认]
 * </pre>
 *
 * <p>点击只改高亮，按「确认」或 Enter / 小键盘 Enter 才发送 {@link StaffModePayload} 并关闭。
 * 高亮用 2px 蓝框，按模式 id 查按钮。</p>
 */
public class EquipoiseStaffScreen extends Screen {

    /** 左列：其他净化魔法（除中间列 3 个之外的全部旧选项）。 */
    private static final int[] OTHER_MODES = {
            StaffCastModes.MODE_LINE_SINGLE,
            StaffCastModes.MODE_SINGLE_HEAL,
            StaffCastModes.MODE_GROUP_HEAL,
            StaffCastModes.MODE_DEFENSE
    };

    private static final String[] OTHER_KEYS = {
            "gui.mugewrayce-mod.equipoise_staff.mode_line_single",
            "gui.mugewrayce-mod.equipoise_staff.mode_single_heal",
            "gui.mugewrayce-mod.equipoise_staff.mode_group_heal",
            "gui.mugewrayce-mod.equipoise_staff.mode_defense"
    };

    /** 中列：常用（点式 / 持续线式 / 群体伤害魔法阵）。 */
    private static final int[] COMMON_MODES = {
            StaffCastModes.MODE_ORB,
            StaffCastModes.MODE_LINE_SUSTAINED,
            StaffCastModes.MODE_GROUP_DAMAGE
    };

    private static final String[] COMMON_KEYS = {
            "gui.mugewrayce-mod.equipoise_staff.mode_orb",
            "gui.mugewrayce-mod.equipoise_staff.mode_line_sustained",
            "gui.mugewrayce-mod.equipoise_staff.mode_group_damage"
    };

    /** 右列：8 个自然魔法技能（7~14，编号顺序与 {@link MagicSkill} 的自然魔法段一致）。 */
    private static final int[] SKILL_MODES = {
            StaffCastModes.MODE_IGNITE,
            StaffCastModes.MODE_MELT,
            StaffCastModes.MODE_FREEZE,
            StaffCastModes.MODE_MAKE_ICE,
            StaffCastModes.MODE_SUMMON_LIGHTNING,
            StaffCastModes.MODE_CREATE_WATER,
            StaffCastModes.MODE_FRONT_FLASH,
            StaffCastModes.MODE_TARGETED_FLASH
    };

    private static final int BUTTON_WIDTH = 96;
    private static final int BUTTON_HEIGHT = 16;
    private static final int ROW_GAP = 3;
    private static final int COLUMN_GAP = 12;

    /** 客户端记住的当前模式（服务端在 StaffCastModes 中另有权威值）。 */
    private static int clientMode = StaffCastModes.MODE_ORB;

    private final Button[] modeButtons = new Button[StaffCastModes.MODE_COUNT];
    private int selectedMode = StaffCastModes.MODE_ORB;

    private int columnLeftX;
    private int columnCommonX;
    private int columnSkillX;
    private int headerY;
    private int topY;

    public EquipoiseStaffScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("gui.mugewrayce-mod.equipoise_staff.title"));
    }

    /** 断开连接时重置客户端记住的模式。 */
    public static void resetClientState() {
        clientMode = StaffCastModes.MODE_ORB;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.selectedMode = StaffCastModes.isValid(clientMode) ? clientMode : StaffCastModes.MODE_ORB;

        int totalWidth = BUTTON_WIDTH * 3 + COLUMN_GAP * 2;
        this.columnLeftX = (this.width - totalWidth) / 2;
        this.columnCommonX = this.columnLeftX + BUTTON_WIDTH + COLUMN_GAP;
        this.columnSkillX = this.columnCommonX + BUTTON_WIDTH + COLUMN_GAP;
        this.topY = this.height / 2 - 64;
        this.headerY = this.topY - 14;

        for (int i = 0; i < OTHER_MODES.length; i++) {
            this.addModeButton(OTHER_MODES[i], Component.translatable(OTHER_KEYS[i]),
                    this.columnLeftX, this.topY + i * (BUTTON_HEIGHT + ROW_GAP));
        }
        for (int i = 0; i < COMMON_MODES.length; i++) {
            this.addModeButton(COMMON_MODES[i], Component.translatable(COMMON_KEYS[i]),
                    this.columnCommonX, this.topY + i * (BUTTON_HEIGHT + ROW_GAP));
        }
        for (int i = 0; i < SKILL_MODES.length; i++) {
            int mode = SKILL_MODES[i];
            MagicSkill skill = StaffCastModes.skillFor(mode);
            Component label = skill == null || skill.langKey() == null
                    ? Component.literal("?")
                    : Component.translatable(skill.langKey());
            this.addModeButton(mode, label, this.columnSkillX, this.topY + i * (BUTTON_HEIGHT + ROW_GAP));
        }

        // 确认键固定在中间列下方（3 个常用选项之后空 8px）
        int confirmY = this.topY + COMMON_MODES.length * (BUTTON_HEIGHT + ROW_GAP) + 8;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.common.confirm"),
                        b -> this.confirm())
                .bounds(this.columnCommonX, confirmY, BUTTON_WIDTH, 20)
                .build());
    }

    private void addModeButton(int mode, Component label, int x, int y) {
        Button button = this.addRenderableWidget(Button.builder(label, b -> this.selectedMode = mode)
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        this.modeButtons[mode] = button;
    }

    private void confirm() {
        clientMode = this.selectedMode;
        ClientPlayNetworking.send(new StaffModePayload(this.selectedMode));
        this.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.confirm();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.equipoise_staff.title"),
                this.width / 2, this.headerY - 26, 0xFFFFFFFF);

        // 三列标题（居中于各自列）
        this.columnHeader(graphics, "gui.mugewrayce-mod.equipoise_staff.column_other", this.columnLeftX);
        this.columnHeader(graphics, "gui.mugewrayce-mod.equipoise_staff.column_common", this.columnCommonX);
        this.columnHeader(graphics, "gui.mugewrayce-mod.equipoise_staff.column_skills", this.columnSkillX);

        // 当前选择：画 2px 外框，不覆盖按钮文字
        Button selected = this.selectedMode >= 0 && this.selectedMode < this.modeButtons.length
                ? this.modeButtons[this.selectedMode] : null;
        if (selected != null && selected.visible) {
            int x = selected.getX();
            int y = selected.getY();
            int w = selected.getWidth();
            int h = selected.getHeight();
            graphics.fill(x - 2, y - 2, x + w + 2, y, 0xFF00AAFF);
            graphics.fill(x - 2, y + h, x + w + 2, y + h + 2, 0xFF00AAFF);
            graphics.fill(x - 2, y - 2, x, y + h + 2, 0xFF00AAFF);
            graphics.fill(x + w, y - 2, x + w + 2, y + h + 2, 0xFF00AAFF);
        }
    }

    private void columnHeader(GuiGraphicsExtractor graphics, String key, int columnX) {
        graphics.centeredText(this.font, Component.translatable(key),
                columnX + BUTTON_WIDTH / 2, this.headerY, 0xFF00AAFF);
    }
}
