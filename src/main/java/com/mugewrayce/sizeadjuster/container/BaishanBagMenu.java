package com.mugewrayce.sizeadjuster.container;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * 百杉包菜单：大箱子（9x6，54 格）布局。
 * 注意：26.1 的 MenuType 构造器为 private，依赖 fabric-menu-api-v1 的
 * classtweaker 将其公开（fabric-api 自带），因此可直接 new MenuType<>()。
 */
public class BaishanBagMenu extends ChestMenu {

    public static final int ROWS = 6;
    public static final int SIZE = ROWS * 9;

    public static final MenuType<BaishanBagMenu> TYPE = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "baishan_bag"),
            new MenuType<>(BaishanBagMenu::new, FeatureFlags.VANILLA_SET));

    /** 服务端：真实容器（读写物品组件）。 */
    public BaishanBagMenu(int id, Inventory playerInventory, Container container) {
        super(TYPE, id, playerInventory, container, ROWS);
    }

    /** 客户端：MenuType 工厂用（虚拟容器，数据由同步包填充）。 */
    public BaishanBagMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(SIZE));
    }
}
