package com.mugewrayce.sizeadjuster.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 柃之册功能选择 GUI（无背景贴图）：
 * 右键柃之册打开，选择一个功能进入对应的新 GUI（功能列表，自上而下）：
 * ① 角色切换（首位）→ 角色切换 GUI（默认/魔法体/特殊-icefair…）；
 * ② 玩家尺寸属性 → 玩家尺寸属性 GUI。
 *
 * 注意：26.1 中 GuiGraphicsExtractor 的文本绘制要求颜色 alpha 非 0
 * （ARGB.alpha(color)==0 时直接跳过绘制），自定义文本颜色必须用 0xFF 开头。
 */
public class BookOfLingScreen extends Screen {

    public BookOfLingScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("book_of_ling"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 160;
        int buttonHeight = 20;
        int x = this.width / 2 - buttonWidth / 2;
        int y1 = this.height / 2 - buttonHeight / 2 - 14; // 角色切换：首位
        int y2 = this.height / 2 + buttonHeight / 2 + 14; // 玩家尺寸属性
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.book_of_ling.role_switch"),
                        b -> openRoleScreen())
                .bounds(x, y1, buttonWidth, buttonHeight)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.book_of_ling.size"),
                        b -> openSizeScreen())
                .bounds(x, y2, buttonWidth, buttonHeight)
                .build());
    }

    private void openRoleScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new RoleSwitchScreen());
        }
    }

    private void openSizeScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new SizeAdjustScreen());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.book_of_ling.title"),
                this.width / 2, this.height / 2 - 66, 0xFFFFFFFF);
    }
}
