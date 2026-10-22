package com.mugewrayce.sizeadjuster.client;

import com.mugewrayce.sizeadjuster.item.StaffCastModes;
import com.mugewrayce.sizeadjuster.payload.StaffModePayload;
import com.mugewrayce.sizeadjuster.switchitem.SwitchItemPayload;
import com.mugewrayce.sizeadjuster.switchitem.SwitchItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 切换 GUI（F3+F4 游戏模式切换风格，无背景贴图）：
 * - 物品行（中间）：鼠标移动到物品上高亮；点击物品立即切换并关闭。
 * - 功能行（物品行下方）：选择净化法杖的施法模式（魔法球/各魔法阵），
 *   点击即应用并高亮当前模式（不关闭 GUI）。
 * - 点击空白处或按 ESC 取消。
 * 注意：26.1 文本绘制要求颜色 alpha 非 0，统一使用 0xFF 开头颜色。
 */
public class SwitchScreen extends Screen {

    private static final int SLOT_SIZE = 16;
    private static final int SLOT_PITCH = 24;

    /** 法杖功能选项（翻译键；顺序与 StaffCastModes 的模式编号一致）。 */
    private static final String[] MODE_KEYS = {
            "gui.mugewrayce-mod.switch.mode_orb",
            "gui.mugewrayce-mod.switch.mode_single_heal",
            "gui.mugewrayce-mod.switch.mode_group_heal",
            "gui.mugewrayce-mod.switch.mode_group_damage",
            "gui.mugewrayce-mod.switch.mode_defense"
    };
    private static final int MODE_BOX_W = 76;
    private static final int MODE_BOX_H = 16;
    private static final int MODE_PITCH = 80;

    /** 客户端当前施法模式（高亮用，点击后本地记录并发送服务端）。 */
    public static int clientCastMode = StaffCastModes.MODE_ORB;

    public SwitchScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("switch"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean inside) {
        int x = (int) event.x();
        int y = (int) event.y();

        // 功能行（在物品行下方）
        int mode = modeAt(x, y);
        if (mode >= 0) {
            clientCastMode = mode;
            ClientPlayNetworking.send(new StaffModePayload(mode));
            this.onClose(); // 选好功能后直接关闭 GUI
            return true;
        }

        // 物品行
        int index = slotAt(x, y);
        if (index >= 0) {
            switchTo(index);
        } else {
            // 点击空白处：取消
            this.onClose();
        }
        return true;
    }

    private void switchTo(int index) {
        if (this.minecraft != null && this.minecraft.player != null) {
            ItemStack held = this.minecraft.player.getMainHandItem();
            Item target = SwitchItems.getItem(index);
            if (SwitchItems.isSwitchable(held) && held.getItem() != target) {
                ClientPlayNetworking.send(
                        new SwitchItemPayload(BuiltInRegistries.ITEM.getKey(target)));
            }
        }
        this.onClose();
    }

    /** 返回鼠标所在物品槽位下标，未命中返回 -1。 */
    private int slotAt(int mouseX, int mouseY) {
        int rowY = rowY();
        int slotTop = rowY - SLOT_SIZE / 2;
        int slotBottom = rowY + SLOT_SIZE / 2;
        if (mouseY < slotTop || mouseY > slotBottom) {
            return -1;
        }
        for (int i = 0; i < SwitchItems.ORDER.size(); i++) {
            int x = slotX(i);
            if (mouseX >= x && mouseX <= x + SLOT_SIZE) {
                return i;
            }
        }
        return -1;
    }

    /** 返回鼠标所在的法杖功能下标，未命中返回 -1。 */
    private int modeAt(int mouseX, int mouseY) {
        int modeY = modeRowY();
        if (mouseY < modeY || mouseY > modeY + MODE_BOX_H) {
            return -1;
        }
        for (int i = 0; i < MODE_KEYS.length; i++) {
            int x = modeX(i);
            if (mouseX >= x && mouseX <= x + MODE_BOX_W) {
                return i;
            }
        }
        return -1;
    }

    private int slotX(int index) {
        int totalWidth = (SwitchItems.ORDER.size() - 1) * SLOT_PITCH;
        return this.width / 2 - totalWidth / 2 + index * SLOT_PITCH;
    }

    private int rowY() {
        return this.height / 2;
    }

    private int modeX(int index) {
        int totalWidth = (MODE_KEYS.length - 1) * MODE_PITCH + MODE_BOX_W;
        return this.width / 2 - totalWidth / 2 + index * MODE_PITCH;
    }

    private int modeRowY() {
        return this.height / 2 + 40;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // 物品行
        int rowY = rowY();
        int hovered = slotAt(mouseX, mouseY);
        for (int i = 0; i < SwitchItems.ORDER.size(); i++) {
            int x = slotX(i);
            if (i == hovered) {
                graphics.fill(x, rowY - SLOT_SIZE / 2, x + SLOT_SIZE, rowY + SLOT_SIZE / 2, 0x60FFFFFF);
            }
            graphics.item(new ItemStack(SwitchItems.getItem(i)), x, rowY - SLOT_SIZE / 2);
        }
        if (hovered >= 0) {
            graphics.centeredText(this.font,
                    new ItemStack(SwitchItems.getItem(hovered)).getHoverName(),
                    this.width / 2, rowY - 42, 0xFFFFFFFF);
        }

        // 法杖功能行（物品行下方）
        int modeY = modeRowY();
        int hoveredMode = modeAt(mouseX, mouseY);
        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.switch.staff_modes"),
                this.width / 2, modeY - 14, 0xFFFFFFFF);
        for (int i = 0; i < MODE_KEYS.length; i++) {
            int x = modeX(i);
            boolean hover = i == hoveredMode;
            boolean selected = i == clientCastMode;
            graphics.fill(x - 1, modeY - 1, x + MODE_BOX_W + 1, modeY + MODE_BOX_H + 1,
                    selected ? 0xFF00AAFF : 0xFF888888);
            graphics.fill(x, modeY, x + MODE_BOX_W, modeY + MODE_BOX_H,
                    selected ? 0xFF1E5A8A : hover ? 0xFF606060 : 0xFF3A3A3A);
            graphics.centeredText(this.font, Component.translatable(MODE_KEYS[i]),
                    x + MODE_BOX_W / 2, modeY + 3, 0xFFFFFFFF);
        }
    }
}
