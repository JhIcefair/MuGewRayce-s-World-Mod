package com.mugewrayce.sizeadjuster.util;

import net.minecraft.server.level.ServerPlayer;

/**
 * 服务端写完物品数据组件后的背包同步（W12）。
 *
 * <p>设置类 GUI 只应通过 C2S 包让服务端改组件；改完后需要让客户端收到新的
 * ItemStack 组件，否则重新打开界面会读到旧值。这里统一标记背包变更并广播
 * 玩家自己的 inventoryMenu。</p>
 */
public final class ComponentSync {

    private ComponentSync() {
    }

    public static void markChanged(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }
}
