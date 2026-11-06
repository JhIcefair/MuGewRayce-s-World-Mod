package com.mugewrayce.sizeadjuster.client;

import com.mugewrayce.sizeadjuster.client.gui.AbstractModScreen;
import com.mugewrayce.sizeadjuster.container.BaishanBagMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 百杉包 GUI：54 格容器 + 玩家真实背包 36 格，背景用 ls\8.png
 * （模组内 {@code textures/gui/container/baishan_bag.png}，256×256，内容区 176×222）。
 */
public class BagScreen extends AbstractModScreen<BaishanBagMenu> {

    /**
     * 注意：26.1 的 GUI 贴图 Identifier 必须带 ".png" 扩展名
     * （TextureContents.load 直接按完整路径查资源，不会自动补扩展名）。
     */
    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("mugewrayce-mod", "textures/gui/container/baishan_bag.png");

    public BagScreen(BaishanBagMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222, TEXTURE);
    }
}
