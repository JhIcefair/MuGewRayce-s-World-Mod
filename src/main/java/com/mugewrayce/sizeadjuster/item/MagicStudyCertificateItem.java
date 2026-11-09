package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.clientbridge.ScreenOpenerHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * 魔法学习证（W12）：右键在客户端打开只读学习证界面。
 *
 * <p>界面直接读主手已同步的 {@code cert_data}；无组件 / 编号为 0 / 内容非法时
 * 显示“无效学习证”，不崩溃。创造模式直接拿到的证没有组件，即无效。</p>
 */
public class MagicStudyCertificateItem extends Item {

    public MagicStudyCertificateItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ScreenOpenerHolder.openStudyCert();
        }
        return InteractionResult.SUCCESS;
    }
}
