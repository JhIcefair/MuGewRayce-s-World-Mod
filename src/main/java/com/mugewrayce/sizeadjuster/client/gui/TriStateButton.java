package com.mugewrayce.sizeadjuster.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 三态按键（W09 通用控件）。
 *
 * <p>三态 = 常态 / 按下 / 不可选：</p>
 * <ul>
 *   <li>贴图由调用方传入，本类不内置任何 GUI 贴图；</li>
 *   <li>不可选：换“不可选”贴图 + {@code active=false} + 不响应点击 + 悬停给出原因；</li>
 *   <li>“按下”既支持点按式（鼠标按住时显示），也支持切换式
 *       （由 {@link OptionGroup} 调用 {@link #setToggled(boolean)}）。</li>
 * </ul>
 *
 * <p>注意：26.1 的原版 {@code Button} 画的是原版控件贴图，不能用于带自定义贴图的 GUI；
 * 这里直接继承 {@link AbstractWidget} 并重写 {@code extractWidgetRenderState}。</p>
 */
public class TriStateButton extends AbstractWidget {

    private final Identifier normalTexture;
    private final Identifier pressedTexture;
    private final Identifier disabledTexture;

    private boolean toggled;
    private boolean mouseDown;
    private boolean toggleMode;
    private Runnable onPress = () -> {
    };
    private Component disabledReason;
    private int textColor = 0xFFFFFFFF;

    public TriStateButton(int x, int y, int width, int height, Component message,
                          Identifier normalTexture, Identifier pressedTexture,
                          Identifier disabledTexture, Runnable onPress) {
        super(x, y, width, height, message);
        this.normalTexture = normalTexture;
        this.pressedTexture = pressedTexture;
        this.disabledTexture = disabledTexture;
        this.onPress = onPress;
        this.setTooltip(null);
    }

    // ------------------------------------------------------------------
    // 状态
    // ------------------------------------------------------------------

    /** 是否为“切换式”按键（点击后保持按下状态；通常由 {@link OptionGroup} 管理）。 */
    public boolean isToggleMode() {
        return this.toggleMode;
    }

    public void setToggleMode(boolean toggleMode) {
        this.toggleMode = toggleMode;
    }

    public boolean isToggled() {
        return this.toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public void setOnPress(Runnable onPress) {
        this.onPress = onPress;
    }

    /** 设为不可选：换不可选贴图、禁用点击、悬停显示原因。 */
    public void setUnavailable(Component reason) {
        this.active = false;
        this.disabledReason = reason;
    }

    /** 恢复可选。 */
    public void setAvailable() {
        this.active = true;
        this.disabledReason = null;
    }

    public Component getDisabledReason() {
        return this.disabledReason;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public Identifier getNormalTexture() {
        return this.normalTexture;
    }

    public Identifier getPressedTexture() {
        return this.pressedTexture;
    }

    public Identifier getDisabledTexture() {
        return this.disabledTexture;
    }

    // ------------------------------------------------------------------
    // 绘制 / 点击
    // ------------------------------------------------------------------

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                            float partialTick) {
        if (!this.visible) {
            return;
        }

        Identifier texture = this.normalTexture;
        if (!this.active && this.disabledTexture != null) {
            texture = this.disabledTexture;
        } else if ((this.mouseDown || this.toggled) && this.pressedTexture != null) {
            texture = this.pressedTexture;
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                this.getX(), this.getY(), 0.0F, 0.0F,
                this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());

        Component message = this.getMessage();
        Minecraft minecraft = Minecraft.getInstance();
        int textWidth = minecraft.font.width(message);
        graphics.text(minecraft.font, message,
                this.getX() + (this.getWidth() - textWidth) / 2,
                this.getY() + (this.getHeight() - 8) / 2,
                this.active ? this.textColor : 0xFFA0A0A0, true);

        if (!this.active && this.disabledReason != null && this.isHovered()) {
            graphics.setTooltipForNextFrame(minecraft.font, this.disabledReason, mouseX, mouseY);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!this.active) {
            return;
        }
        this.mouseDown = true;
        this.playDownSound(Minecraft.getInstance().getSoundManager());
        this.onPress.run();
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.mouseDown = false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
