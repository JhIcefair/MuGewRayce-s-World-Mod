package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.clientbridge.ScreenOpenerHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 柃之册：右键打开功能选择 GUI（目前包含"玩家尺寸属性"功能）。
 * GUI 打开走 {@link ScreenOpenerHolder} 客户端桥接；服务端无事可做，返回 SUCCESS 即可。
 */
public class BookOfLingItem extends Item {

    public BookOfLingItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ScreenOpenerHolder.openBookOfLing();
        }
        return InteractionResult.SUCCESS;
    }
}
