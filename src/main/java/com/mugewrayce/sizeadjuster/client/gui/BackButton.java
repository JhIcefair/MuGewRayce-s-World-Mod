package com.mugewrayce.sizeadjuster.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * 左上角返回键（W09 通用控件）。
 *
 * <p>只用于二级及以上界面；一级界面不显示。点击后由
 * {@link AbstractModScreen} 返回上一级屏幕。</p>
 *
 * <p>本任务不提供返回键贴图，因此按“按键不设贴图”的通用规则用文字绘制；
 * 文案复用原版语言键 {@code gui.back}，不新增 GUI 专用语言键。</p>
 */
public class BackButton extends AbstractWidget {

    public static final int WIDTH = 40;
    public static final int HEIGHT = 20;

    private final Runnable onPress;

    public BackButton(int x, int y, Runnable onPress) {
        super(x, y, WIDTH, HEIGHT, Component.translatable("gui.back"));
        this.onPress = onPress;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                            float partialTick) {
        if (!this.visible) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Component message = this.getMessage();
        int textWidth = minecraft.font.width(message);
        graphics.text(minecraft.font, message,
                this.getX() + (this.getWidth() - textWidth) / 2,
                this.getY() + (this.getHeight() - 8) / 2,
                this.active ? 0xFFFFFFFF : 0xFFA0A0A0, true);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!this.active) {
            return;
        }
        this.playDownSound(Minecraft.getInstance().getSoundManager());
        this.onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
