package com.mugewrayce.sizeadjuster.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 通用容器屏幕基类（W09）。
 *
 * <p>统一处理：</p>
 * <ul>
 *   <li>{@code imageWidth/imageHeight} 由子类按“内容区尺寸”传入（百杉包 176×222，
 *       后续方块 GUI 238×248），<b>绝不拿 256×256 当 GUI 尺寸</b>；</li>
 *   <li>背景只画左上角 {@code imageWidth × imageHeight} 的内容区，贴图本身仍是 256×256；</li>
 *   <li>标题与玩家背包标签沿用 {@link AbstractContainerScreen} 的默认位置；</li>
 *   <li>二级及以上界面由子类传入 parent screen，基类据此在左上角显示返回键；
 *       一级界面 parent 为 null，不显示返回键。</li>
 * </ul>
 */
public abstract class AbstractModScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    private final Identifier backgroundTexture;
    private final Screen parentScreen;

    protected AbstractModScreen(T menu, Inventory playerInventory, Component title,
                                int imageWidth, int imageHeight, Identifier backgroundTexture) {
        this(menu, playerInventory, title, imageWidth, imageHeight, backgroundTexture, null);
    }

    protected AbstractModScreen(T menu, Inventory playerInventory, Component title,
                                int imageWidth, int imageHeight, Identifier backgroundTexture,
                                Screen parentScreen) {
        super(menu, playerInventory, title, imageWidth, imageHeight);
        this.backgroundTexture = backgroundTexture;
        this.parentScreen = parentScreen;
    }

    public Identifier getBackgroundTexture() {
        return this.backgroundTexture;
    }

    public Screen getParentScreen() {
        return this.parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        // 只有二级及以上界面（传入 parent）才有返回键；一级界面不显示。
        if (this.parentScreen != null) {
            this.addRenderableWidget(new BackButton(5, 5, this::goBack));
        }
    }

    /** 返回上一级；没有 parent 时按普通关闭处理。 */
    protected void goBack() {
        if (this.minecraft == null) {
            return;
        }
        if (this.parentScreen != null) {
            this.minecraft.setScreen(this.parentScreen);
        } else {
            this.onClose();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 与 26.1 原版 ContainerScreen 一致：先画菜单遮罩，再画 GUI 内容区。
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (this.backgroundTexture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.backgroundTexture,
                    this.leftPos, this.topPos, 0.0F, 0.0F,
                    this.imageWidth, this.imageHeight, 256, 256);
        }
    }
}
