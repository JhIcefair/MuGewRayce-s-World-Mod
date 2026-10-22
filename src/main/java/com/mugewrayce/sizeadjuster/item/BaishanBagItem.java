package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.container.BaishanBagMenu;
import com.mugewrayce.sizeadjuster.container.BagContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

/**
 * 百杉包：右键打开 54 格大箱子式背包（内容随物品持久化）。
 * 手持模型套用原版木棍（见 items/baishan_bag.json 的 display_context 定义）。
 */
public class BaishanBagItem extends Item {

    public BaishanBagItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new BaishanBagMenu(id, inventory,
                            new BagContainer(stack, level.registryAccess())),
                    Component.translatable("container.mugewrayce.baishan_bag")));
        }
        return InteractionResult.SUCCESS;
    }
}
