package com.mugewrayce.sizeadjuster.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

/**
 * 百杉包 GUI：布局同大箱子（9x6），背景贴图用 ls\8.png（256x256，内容区约 176x222）。
 */
public class BagScreen extends ContainerScreen {

    /**
     * 注意：26.1 的 GUI 贴图 Identifier 必须带 ".png" 扩展名
     * （原版即 "textures/gui/container/generic_54.png"），
     * TextureContents.load 直接按完整路径查资源，不会自动补扩展名。
     */
    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("mugewrayce-mod", "textures/gui/container/baishan_bag.png");

    public BagScreen(ChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 像素式 blit（与原版容器屏幕一致）：绘制 8.png 的 0,0~imageWidth,imageHeight 区域
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                this.leftPos, this.topPos,
                0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }
}
