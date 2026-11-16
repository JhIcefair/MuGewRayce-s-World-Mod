package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.container.CrystalTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 水晶拆合台 GUI（W20）：9 个容器格 + 玩家真实背包 36 格；背景内容区 238×248。
 *
 * <p>左上角竖排两个 12×12 按键（{@code (8,20)}、{@code (8,38)}），文字标注在按键右侧
 * （{@code (24,22)}、{@code (24,40)}）：上=拆分、下=合成。两者互斥、按下保持选中：
 * 点击只发 button id，选中状态由服务端 {@code ContainerData} 回读后
 * 在 {@link #containerTick()} 里写回 {@link TriStateButton#setToggled(boolean)}。</p>
 *
 * <p>红字提示画在环下方（{@code y=141}），不压格子。</p>
 */
public class CrystalTableScreen extends AbstractModScreen<CrystalTableMenu> {

    public static final Identifier BACKGROUND = id("textures/gui/container/crystal_table.png");
    public static final Identifier BUTTON_NORMAL =
            id("textures/gui/container/crystal_table_button_normal.png");
    public static final Identifier BUTTON_PRESSED =
            id("textures/gui/container/crystal_table_button_pressed.png");

    private static final int BUTTON_SIZE = 12;

    private TriStateButton splitButton;
    private TriStateButton craftButton;

    public CrystalTableScreen(CrystalTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 238, 248, BACKGROUND);
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelX = 39;
    }

    @Override
    protected void init() {
        super.init();

        this.splitButton = new TriStateButton(
                this.leftPos + 8, this.topPos + 20, BUTTON_SIZE, BUTTON_SIZE, Component.empty(),
                BUTTON_NORMAL, BUTTON_PRESSED, BUTTON_NORMAL,
                () -> this.sendButton(CrystalTableMenu.BUTTON_SPLIT));
        this.splitButton.setToggleMode(true);
        this.addRenderableWidget(this.splitButton);

        this.craftButton = new TriStateButton(
                this.leftPos + 8, this.topPos + 38, BUTTON_SIZE, BUTTON_SIZE, Component.empty(),
                BUTTON_NORMAL, BUTTON_PRESSED, BUTTON_NORMAL,
                () -> this.sendButton(CrystalTableMenu.BUTTON_CRAFT));
        this.craftButton.setToggleMode(true);
        this.addRenderableWidget(this.craftButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // 选中状态由服务端 ContainerData 回读，客户端只负责显示；两个按键互斥。
        if (this.splitButton != null) {
            this.splitButton.setToggled(this.menu.isSplit());
        }
        if (this.craftButton != null) {
            this.craftButton.setToggled(!this.menu.isSplit());
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX + 1, this.titleLabelY + 1,
                0xFF9FB0D0, false);
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY,
                0xFF2A3A6E, false);
        graphics.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(this.font,
                Component.translatable("gui.mugewrayce-mod.crystal_table.split"),
                this.leftPos + 24, this.topPos + 22, 0xFF2A3A6E, false);
        graphics.text(this.font,
                Component.translatable("gui.mugewrayce-mod.crystal_table.craft"),
                this.leftPos + 24, this.topPos + 40, 0xFF2A3A6E, false);

        Component hint = hintComponent(this.menu.getHintCode());
        if (hint != null) {
            int width = this.font.width(hint);
            graphics.text(this.font, hint,
                    this.leftPos + 119 - width / 2, this.topPos + 141, 0xFFFF5555, false);
        }
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private static Component hintComponent(int hintCode) {
        return switch (hintCode) {
            case CrystalTableMenu.HINT_NEED_BLOCK ->
                    Component.translatable("gui.mugewrayce-mod.crystal_table.hint.need_block");
            case CrystalTableMenu.HINT_NEED_CRYSTALS ->
                    Component.translatable("gui.mugewrayce-mod.crystal_table.hint.need_crystals");
            case CrystalTableMenu.HINT_NOT_SAME ->
                    Component.translatable("gui.mugewrayce-mod.crystal_table.hint.not_same");
            case CrystalTableMenu.HINT_OUTPUT_BLOCKED ->
                    Component.translatable("gui.mugewrayce-mod.crystal_table.hint.output_blocked");
            case CrystalTableMenu.HINT_INPUT_FULL ->
                    Component.translatable("gui.mugewrayce-mod.crystal_table.hint.input_full");
            default -> null;
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("mugewrayce-mod", path);
    }
}
