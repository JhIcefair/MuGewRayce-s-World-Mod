package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.clientbridge.ScreenOpenerHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * 魔力值探测器（W12）：右键在客户端打开无背景 5×5 探测界面。
 *
 * <p>实际探测由界面发 C2S，服务端在请求瞬间取样，3 秒后回包；
 * W12 的 {@code ManaProbe} 只是占位，真实区块魔力在 W13。</p>
 */
public class ManaDetectorItem extends Item {

    public ManaDetectorItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ScreenOpenerHolder.openManaDetector();
        }
        return InteractionResult.SUCCESS;
    }
}
