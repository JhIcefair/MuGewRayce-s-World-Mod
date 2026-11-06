package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.container.MagicAttachmentTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 魔法附加台 GUI（W10）：41 格 = 5 专用格 + 36 真背包；背景内容区 238×248。
 *
 * <p>两个键都是点按式：按下换按下贴图，松开才发 button id；
 * 贴图不含文字，文字由代码画。</p>
 */
public class MagicAttachmentTableScreen extends AbstractModScreen<MagicAttachmentTableMenu> {

    public static final Identifier BACKGROUND = id("textures/gui/container/magic_attachment_table.png");
    public static final Identifier BUTTON_NORMAL =
            id("textures/gui/container/magic_attachment_table_button_normal.png");
    public static final Identifier BUTTON_PRESSED =
            id("textures/gui/container/magic_attachment_table_button_pressed.png");

    private TriStateButton craftStaffButton;
    private TriStateButton attachButton;

    public MagicAttachmentTableScreen(MagicAttachmentTableMenu menu, Inventory playerInventory,
                                      Component title) {
        super(menu, playerInventory, title, 238, 248, BACKGROUND);
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelX = 39;
    }

    @Override
    protected void init() {
        super.init();

        this.craftStaffButton = new TriStateButton(
                this.leftPos + 170, this.topPos + 129, 54, 12,
                Component.translatable("gui.mugewrayce-mod.magic_attachment_table.craft_staff"),
                BUTTON_NORMAL, BUTTON_PRESSED, BUTTON_NORMAL, () -> {
        });
        this.craftStaffButton.setToggleMode(false);
        this.craftStaffButton.setOnRelease(
                () -> this.sendButton(MagicAttachmentTableMenu.BUTTON_CRAFT_STAFF));
        this.addRenderableWidget(this.craftStaffButton);

        this.attachButton = new TriStateButton(
                this.leftPos + 170, this.topPos + 143, 54, 12,
                Component.translatable("gui.mugewrayce-mod.magic_attachment_table.attach"),
                BUTTON_NORMAL, BUTTON_PRESSED, BUTTON_NORMAL, () -> {
        });
        this.attachButton.setToggleMode(false);
        this.attachButton.setOnRelease(
                () -> this.sendButton(MagicAttachmentTableMenu.BUTTON_ATTACH));
        this.addRenderableWidget(this.attachButton);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 标题色 #6A0C8A + 1px 浅色阴影；背包标签 x=39，y 用基类默认。
        graphics.text(this.font, this.title, this.titleLabelX + 1, this.titleLabelY + 1,
                0xFFC9A0D8, false);
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY,
                0xFF6A0C8A, false);
        graphics.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Component hint = hintComponent(this.menu.getHintCode());
        if (hint != null) {
            graphics.centeredText(this.font, hint,
                    this.leftPos + 111, this.topPos + 95, 0xFFFF5555);
        }
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private static Component hintComponent(int hintCode) {
        return switch (hintCode) {
            case MagicAttachmentTableMenu.HINT_MATERIALS ->
                    Component.translatable("gui.mugewrayce-mod.magic_attachment_table.hint.materials");
            case MagicAttachmentTableMenu.HINT_CERT ->
                    Component.translatable("gui.mugewrayce-mod.magic_attachment_table.hint.cert");
            case MagicAttachmentTableMenu.HINT_CERT_MISMATCH ->
                    Component.translatable("gui.mugewrayce-mod.magic_attachment_table.hint.cert_mismatch");
            case MagicAttachmentTableMenu.HINT_STAFF_UNBOUND ->
                    Component.translatable("gui.mugewrayce-mod.magic_attachment_table.hint.staff_unbound");
            case MagicAttachmentTableMenu.HINT_NON_NATURAL ->
                    Component.translatable("gui.mugewrayce-mod.magic_attachment_table.hint.non_natural");
            case MagicAttachmentTableMenu.HINT_NO_MANA ->
                    Component.translatable("gui.mugewrayce-mod.magic_attachment_table.hint.no_mana");
            case MagicAttachmentTableMenu.HINT_OUTPUT_BLOCKED ->
                    Component.translatable("gui.mugewrayce-mod.magic_attachment_table.hint.output_blocked");
            case MagicAttachmentTableMenu.HINT_BOOK ->
                    Component.translatable("gui.mugewrayce-mod.magic_attachment_table.hint.book");
            case MagicAttachmentTableMenu.HINT_WEAPON ->
                    Component.translatable("gui.mugewrayce-mod.magic_attachment_table.hint.weapon");
            case MagicAttachmentTableMenu.HINT_ATTACHMENT_LEVEL ->
                    Component.translatable("gui.mugewrayce-mod.magic_attachment_table.hint.attachment_level");
            case MagicAttachmentTableMenu.HINT_NOTHING ->
                    Component.translatable("gui.mugewrayce-mod.magic_attachment_table.hint.nothing");
            default -> null;
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("mugewrayce-mod", path);
    }
}
