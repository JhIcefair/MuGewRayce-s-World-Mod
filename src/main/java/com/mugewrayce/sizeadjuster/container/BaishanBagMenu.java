package com.mugewrayce.sizeadjuster.container;

import com.mugewrayce.sizeadjuster.menu.AbstractModMenu;
import com.mugewrayce.sizeadjuster.registry.MenuRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

/**
 * 百杉包菜单：大箱子式 54 格容器 + 玩家真实背包 36 格（共 90 格）。
 *
 * <p>W09 起改接 {@link AbstractModMenu}：容器格 0..53、玩家物品栏 54..80、
 * 快捷栏 81..89；玩家 36 格由基类绑定真实 {@code PlayerInventory}。</p>
 *
 * <p>26.1 的 MenuType 构造器为 private，依赖 fabric-menu-api-v1 的 classtweaker
 * 将其公开（fabric-api 自带），因此可直接构造；注册统一走
 * {@link MenuRegistry#registerMenu(String, MenuType.MenuSupplier)}。</p>
 */
public class BaishanBagMenu extends AbstractModMenu {

    public static final int ROWS = 6;
    public static final int SIZE = ROWS * 9;

    public static final MenuType<BaishanBagMenu> TYPE =
            MenuRegistry.registerMenu("baishan_bag", BaishanBagMenu::new);

    /** 容器区首格坐标（与原版大箱子一致）。 */
    public static final int CONTAINER_X = 8;
    public static final int CONTAINER_Y = 18;
    /** 玩家物品栏首格坐标（与原版 6 行大箱子一致）。 */
    public static final int PLAYER_INVENTORY_X = 8;
    public static final int PLAYER_INVENTORY_Y = 140;

    /** 服务端：真实容器（读写物品组件）。 */
    public BaishanBagMenu(int id, Inventory playerInventory, Container container) {
        super(TYPE, id, playerInventory, container, SIZE);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                this.addModSlot(new Slot(container, col + row * 9,
                        CONTAINER_X + col * 18,
                        CONTAINER_Y + row * 18));
            }
        }

        // 玩家真实背包 36 格：物品栏 54..80、快捷栏 81..89。
        this.addPlayerInventory(PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
    }

    /** 客户端：MenuType 工厂用（虚拟容器，数据由同步填充）。 */
    public BaishanBagMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(SIZE));
    }
}
