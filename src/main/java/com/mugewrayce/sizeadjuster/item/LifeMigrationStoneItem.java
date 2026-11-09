package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.clientbridge.ScreenOpenerHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * 生命迁移石（W12）：右键在客户端打开受限角色转换界面。
 *
 * <p>只列默认 / 有生命的魔法体；确认由 C2S 交给服务端校验并消耗 1 个。
 * 不能转特殊角色（icefair 等）。</p>
 */
public class LifeMigrationStoneItem extends Item {

    public LifeMigrationStoneItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ScreenOpenerHolder.openLifeMigration();
        }
        return InteractionResult.SUCCESS;
    }
}
