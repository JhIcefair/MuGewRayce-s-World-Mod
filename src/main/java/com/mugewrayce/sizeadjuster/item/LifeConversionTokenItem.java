package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.clientbridge.ScreenOpenerHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 生命转换令牌：右键直接打开"角色切换"GUI（与柃之册第二级 GUI 相同，
 * 此处作为第一级 GUI 打开）。
 * 模型/手持样式同起源令牌（平面贴图）；贴图 ls\17.png。
 * GUI 打开走 {@link ScreenOpenerHolder} 客户端桥接；服务端无事可做，返回 SUCCESS 即可。
 */
public class LifeConversionTokenItem extends Item {

    public LifeConversionTokenItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ScreenOpenerHolder.openRoleSwitch();
        }
        return InteractionResult.SUCCESS;
    }
}
