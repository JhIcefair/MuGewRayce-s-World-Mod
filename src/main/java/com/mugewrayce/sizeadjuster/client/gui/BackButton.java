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
 * <p>本任务不提供返回键贴图，因此按“按键不设贴图”的通用规则自绘：
 * 深色底 + 1px 灰边 + 白字（W17 起不再只有一行白字）；文案复用原版语言键
 * {@code gui.back}，前面加 “← ”，不新增 GUI 专用语言键。</p>
 */
public class BackButton extends AbstractWidget {

    /** W17：52×18，足够放下 “← 返回”。 */
    public static final int WIDTH = 52;
    public static final int HEIGHT = 18;

    private static final int BACKGROUND = 0xCC000000;
    private static final int BORDER = 0xFF808080;

    private final Runnable onPress;

    public BackButton(int x, int y, Runnable onPress) {
        super(x, y, WIDTH, HEIGHT,
                Component.literal("← ").append(Component.translatable("gui.back")));
        this.onPress = onPress;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                            float partialTick) {
        if (!this.visible) {
            return;
        }
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();
        graphics.fill(x, y, x + width, y + height, BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER);
        graphics.fill(x, y, x + 1, y + height, BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER);
        graphics.centeredText(Minecraft.getInstance().font, this.getMessage(),
                x + width / 2, y + (height - 8) / 2,
                this.active ? 0xFFFFFFFF : 0xFFA0A0A0);
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
