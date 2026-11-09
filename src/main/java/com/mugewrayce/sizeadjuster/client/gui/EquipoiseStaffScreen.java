package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.item.StaffCastModes;
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
 * 净化法杖设置 GUI（W12；无背景）。
 *
 * <p>i 键在主手净化法杖时打开；一行 7 种施法模式，点击只改高亮，
 * 按「确认」或 Enter / 小键盘 Enter 才发送 {@link StaffModePayload} 并关闭。
 * 旧的“物品行”已由方向键取代，本界面不再提供切换物品入口。</p>
 */
public class EquipoiseStaffScreen extends Screen {

    private static final String[] MODE_KEYS = {
            "gui.mugewrayce-mod.equipoise_staff.mode_orb",
            "gui.mugewrayce-mod.equipoise_staff.mode_line_single",
            "gui.mugewrayce-mod.equipoise_staff.mode_line_sustained",
            "gui.mugewrayce-mod.equipoise_staff.mode_single_heal",
            "gui.mugewrayce-mod.equipoise_staff.mode_group_heal",
            "gui.mugewrayce-mod.equipoise_staff.mode_group_damage",
            "gui.mugewrayce-mod.equipoise_staff.mode_defense"
    };

    /** 客户端记住的当前模式（服务端在 StaffCastModes 中另有权威值）。 */
    private static int clientMode = StaffCastModes.MODE_ORB;

    private final Button[] modeButtons = new Button[MODE_KEYS.length];
    private int selectedMode = StaffCastModes.MODE_ORB;
    private int buttonX;
    private int buttonY;
    private int buttonWidth;
    private int buttonHeight;

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

        int gap = 4;
        this.buttonHeight = 20;
        this.buttonWidth = Math.min(92, Math.max(40,
                (this.width - 40 - gap * (MODE_KEYS.length - 1)) / MODE_KEYS.length));
        int total = this.buttonWidth * MODE_KEYS.length + gap * (MODE_KEYS.length - 1);
        this.buttonX = (this.width - total) / 2;
        this.buttonY = this.height / 2 - 22;

        for (int i = 0; i < MODE_KEYS.length; i++) {
            final int mode = i;
            this.modeButtons[i] = this.addRenderableWidget(
                    Button.builder(Component.translatable(MODE_KEYS[i]), b -> this.selectedMode = mode)
                            .bounds(this.buttonX + i * (this.buttonWidth + gap), this.buttonY,
                                    this.buttonWidth, this.buttonHeight)
                            .build());
        }

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.common.confirm"),
                        b -> this.confirm())
                .bounds(this.width / 2 - 60, this.buttonY + 44, 120, 20)
                .build());
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
                this.width / 2, this.buttonY - 42, 0xFFFFFFFF);

        // 当前选择：画 2px 外框，不覆盖按钮文字
        int index = this.selectedMode;
        if (index >= 0 && index < this.modeButtons.length && this.modeButtons[index] != null) {
            int x = this.modeButtons[index].getX();
            int y = this.modeButtons[index].getY();
            int w = this.modeButtons[index].getWidth();
            int h = this.modeButtons[index].getHeight();
            graphics.fill(x - 2, y - 2, x + w + 2, y, 0xFF00AAFF);
            graphics.fill(x - 2, y + h, x + w + 2, y + h + 2, 0xFF00AAFF);
            graphics.fill(x - 2, y - 2, x, y + h + 2, 0xFF00AAFF);
            graphics.fill(x + w, y - 2, x + w + 2, y + h + 2, 0xFF00AAFF);
        }
    }
}
