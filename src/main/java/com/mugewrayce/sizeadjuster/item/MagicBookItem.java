package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.component.MagicBookData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.clientbridge.ScreenOpenerHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 魔法书（W12 设置界面 / W14b 右键释放入口）。
 *
 * <p>右键：未选技能 → 提示“未选择技能”；已选 → 打开对应二级界面，
 * 界面「确认」先发 {@code magic_book_settings} 再发 {@code magic_book_cast}。
 * 服务端不信任客户端，收到 cast 包后重读组件并校验。</p>
 */
public class MagicBookItem extends Item {

    public MagicBookItem(Item.Properties properties) {
        super(properties.component(ModComponents.MAGIC_BOOK_DATA, MagicBookData.DEFAULT));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ItemStack stack = player.getMainHandItem();
            MagicBookData data = stack.getOrDefault(ModComponents.MAGIC_BOOK_DATA, MagicBookData.DEFAULT);
            if (data.skill() == null || data.skill().isEmpty()) {
                player.sendSystemMessage(
                        Component.translatable("message.mugewrayce-mod.no_skill_selected"));
                return InteractionResult.SUCCESS;
            }
            if (MagicBookData.SKILL_TIME_PAUSE.equals(data.skill())) {
                ScreenOpenerHolder.openMagicBookExclusion();
            } else if (MagicBookData.SKILL_SPACE_TELEPORT.equals(data.skill())) {
                ScreenOpenerHolder.openMagicBookTeleport();
            }
        }
        return InteractionResult.SUCCESS;
    }
}
