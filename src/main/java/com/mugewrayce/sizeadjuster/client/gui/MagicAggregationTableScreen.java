package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.container.MagicAggregationTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 魔法聚合台 GUI（W10）：37 格 = 1 产物 + 36 真背包；背景内容区 238×248。
 *
 * <p>「普通」为切换式选项键：点击只发 button id 0，不在客户端本地 toggle；
 * 实际选中状态由服务端 ContainerData 回读。 「合成」为点按式：按下换贴图，松开执行。</p>
 */
public class MagicAggregationTableScreen extends AbstractModScreen<MagicAggregationTableMenu> {

    public static final Identifier BACKGROUND = id("textures/gui/container/magic_aggregation_table.png");
    public static final Identifier OPTION_NORMAL =
            id("textures/gui/container/magic_aggregation_table_option_normal.png");
    public static final Identifier OPTION_PRESSED =
            id("textures/gui/container/magic_aggregation_table_option_pressed.png");
    public static final Identifier CRAFT_NORMAL =
            id("textures/gui/container/magic_aggregation_table_craft_normal.png");
    public static final Identifier CRAFT_PRESSED =
            id("textures/gui/container/magic_aggregation_table_craft_pressed.png");

    private TriStateButton optionButton;
    private TriStateButton craftButton;

    public MagicAggregationTableScreen(MagicAggregationTableMenu menu, Inventory playerInventory,
                                       Component title) {
        super(menu, playerInventory, title, 238, 248, BACKGROUND);
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelX = 39;
    }

    @Override
    protected void init() {
        super.init();

        this.optionButton = new TriStateButton(
                this.leftPos + 8, this.topPos + 32, 16, 16, Component.empty(),
                OPTION_NORMAL, OPTION_PRESSED, OPTION_NORMAL,
                () -> this.sendButton(MagicAggregationTableMenu.BUTTON_TOGGLE));
        this.optionButton.setToggleMode(true);
        this.addRenderableWidget(this.optionButton);

        this.craftButton = new TriStateButton(
                this.leftPos + 170, this.topPos + 137, 54, 12,
                Component.translatable("gui.mugewrayce-mod.magic_aggregation_table.craft"),
                CRAFT_NORMAL, CRAFT_PRESSED, CRAFT_NORMAL, () -> {
        });
        this.craftButton.setToggleMode(false);
        this.craftButton.setOnRelease(() -> this.sendButton(MagicAggregationTableMenu.BUTTON_CRAFT));
        this.addRenderableWidget(this.craftButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.optionButton != null) {
            // 选中状态由服务端 ContainerData 回读，客户端只负责显示。
            this.optionButton.setToggled(this.menu.isSelected());
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 标题色 #2A3A6E + 1px 浅色阴影；背包标签 x=39，y 用基类默认。
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
                Component.translatable("gui.mugewrayce-mod.magic_aggregation_table.option.normal"),
                this.leftPos + 28, this.topPos + 36, 0xFF2A3A6E, false);

        int seconds = this.menu.getRemainingSeconds();
        if (seconds > 0) {
            String text = String.valueOf(seconds);
            int w = this.font.width(text);
            graphics.text(this.font, text,
                    this.leftPos + 119 - w / 2, this.topPos + 62, 0xFF2549C8, false);
        }

        Component hint = hintComponent(this.menu.getHintCode());
        if (hint != null) {
            int w = this.font.width(hint);
            graphics.text(this.font, hint,
                    this.leftPos + 119 - w / 2, this.topPos + 95, 0xFFFF5555, false);
        }
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private static Component hintComponent(int hintCode) {
        return switch (hintCode) {
            case MagicAggregationTableMenu.HINT_SELECT_CRYSTAL ->
                    Component.translatable("gui.mugewrayce-mod.magic_aggregation_table.hint.select_crystal");
            case MagicAggregationTableMenu.HINT_CRAFTING ->
                    Component.translatable("gui.mugewrayce-mod.magic_aggregation_table.hint.crafting");
            case MagicAggregationTableMenu.HINT_NO_MANA ->
                    Component.translatable("gui.mugewrayce-mod.magic_aggregation_table.hint.no_mana");
            case MagicAggregationTableMenu.HINT_OUTPUT_BLOCKED ->
                    Component.translatable("gui.mugewrayce-mod.magic_aggregation_table.hint.output_blocked");
            default -> null;
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("mugewrayce-mod", path);
    }
}
