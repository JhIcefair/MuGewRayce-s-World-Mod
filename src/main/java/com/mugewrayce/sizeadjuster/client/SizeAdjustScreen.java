package com.mugewrayce.sizeadjuster.client;

import com.mugewrayce.sizeadjuster.payload.SizeActionPayload;
import com.mugewrayce.sizeadjuster.util.SizeAttributes;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 玩家尺寸属性 GUI（无背景贴图）：
 * - 中间：身高(cm)输入框（键盘数字输入，限 4 位）+ 下方"确定应用"按钮；
 * - 左侧：显示当前全部属性值（先取本地已同步的玩家属性，服务端回发后刷新）；
 * - 右侧：特殊组合选项，最上面是"重置"，其下是各特殊组（目前一个："特殊尺寸：icefair"），
 *   点击即应用。
 * 打开时自动向服务端查询一次当前状态（保证与服务端一致）。
 *
 * 注意：26.1 中 GuiGraphicsExtractor 的文本绘制要求颜色 alpha 非 0
 * （ARGB.alpha(color)==0 时直接跳过绘制），自定义文本颜色必须用 0xFF 开头。
 */
public class SizeAdjustScreen extends Screen {

    private static final int MIN_HEIGHT = 10;
    private static final int MAX_HEIGHT = 1000;

    /** 身高输入（仅数字）。 */
    private String input = "";

    /** 当前属性状态文本（本地玩家属性 + 服务端回发刷新；文本组件按客户端语言渲染）。 */
    private List<Component> status = List.of();

    /** 输入框区域（init 时按窗口尺寸计算）。 */
    private int boxX, boxY;

    public SizeAdjustScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("player_size"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        this.boxX = cx - 60;
        this.boxY = this.height / 2 - 6;

        // 中间：确定应用（输入框下方）
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.size.apply"), b -> confirmInput())
                .bounds(cx - 60, this.height / 2 + 26, 120, 20)
                .build());

        // 右侧：重置（最上面）→ 特殊组（目前一个：icefair）
        int rx = this.width * 3 / 4 - 75;
        int ry = this.height / 2 - 70;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.size.reset"),
                        b -> applyAction(SizeActionPayload.ACTION_RESET, 0))
                .bounds(rx, ry, 150, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.size.special_icefair"),
                        b -> applyAction(SizeActionPayload.ACTION_APPLY_SPECIAL, 0))
                .bounds(rx, ry + 28, 150, 20)
                .build());

        // 本地玩家的属性值已同步到客户端，先直接显示当前状态（保证左侧一定有内容）
        if (this.minecraft != null && this.minecraft.player != null) {
            this.status = SizeAttributes.getStatusLines(this.minecraft.player);
        }
        // 向服务端查询一次，确保与服务端一致（回发会刷新 status）
        ClientPlayNetworking.send(new SizeActionPayload(SizeActionPayload.ACTION_QUERY_STATUS, 0));
    }

    /** 服务端状态回发回调（客户端接收器调用）。 */
    public void setStatus(List<Component> status) {
        this.status = status;
    }

    private void confirmInput() {
        try {
            int value = Integer.parseInt(this.input);
            if (value >= MIN_HEIGHT && value <= MAX_HEIGHT) {
                applyAction(SizeActionPayload.ACTION_APPLY_HEIGHT, value);
            }
        } catch (NumberFormatException ignored) {
            // 输入非法：忽略
        }
    }

    private void applyAction(int action, int height) {
        ClientPlayNetworking.send(new SizeActionPayload(action, height));
    }

    /** 数字字符输入（文本输入的标准通道）。 */
    @Override
    public boolean charTyped(CharacterEvent event) {
        String s = event.codepointAsString();
        if (s.length() == 1 && s.charAt(0) >= '0' && s.charAt(0) <= '9' && this.input.length() < 4) {
            this.input += s.charAt(0);
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_BACKSPACE && !this.input.isEmpty()) {
            this.input = this.input.substring(0, this.input.length() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.confirmInput();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;

        // 标题
        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.size.title"),
                cx, this.height / 2 - 92, 0xFFFFFFFF);

        // 中间：输入框（边框 + 背景 + 文本）
        graphics.fill(this.boxX - 1, this.boxY - 1, this.boxX + 121, this.boxY + 21, 0xFFAAAAAA);
        graphics.fill(this.boxX, this.boxY, this.boxX + 120, this.boxY + 20, 0xFF000000);
        boolean empty = this.input.isEmpty();
        Component inputText = empty
                ? Component.translatable("gui.mugewrayce-mod.size.input_hint")
                : Component.literal(this.input);
        graphics.centeredText(this.font, inputText, cx, this.boxY + 4,
                empty ? 0xFF888888 : 0xFFFFFFFF);

        // 左侧：当前全部属性值（同 /mugewrayce size status 输出内容）
        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.size.current_attributes"),
                this.width / 4, this.height / 2 - 92, 0xFFFFAA00);
        int lx = this.width / 4 - 110;
        int ly = this.height / 2 - 70;
        for (int i = 0; i < this.status.size(); i++) {
            graphics.text(this.font, this.status.get(i), lx, ly + i * 10, 0xFFFFFFFF);
        }
    }
}
