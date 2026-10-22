package com.mugewrayce.sizeadjuster.container;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * 百杉包的容器：从物品的 CONTAINER 组件加载，变更时写回（内容随物品持久化）。
 */
public class BagContainer extends SimpleContainer {

    private final ItemStack stack;
    private final RegistryAccess registryAccess;

    public BagContainer(ItemStack stack, RegistryAccess registryAccess) {
        super(BaishanBagMenu.SIZE);
        this.stack = stack;
        this.registryAccess = registryAccess;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(this.getItems());
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!stack.isEmpty()) {
            stack.set(DataComponents.CONTAINER,
                    ItemContainerContents.fromItems(this.getItems()));
        }
    }
}
