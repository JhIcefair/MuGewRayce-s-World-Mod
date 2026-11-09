package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.component.CrystalData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.payload.CrystalSettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 普通魔法水晶设置 GUI（W12；无背景）。
 *
 * <p>i 键在主手水晶时打开；默认只显示「供能」，开启供能后才显示「自动供能」。
 * 两个开关立即发 {@link CrystalSettingsPayload}，服务端拒绝 {@code auto && !supply}
 * 无效态。W12 只写开关，容量/消耗/消失留 W13。</p>
 */
public class MagicCrystalScreen extends Screen {

    private boolean supplyEnabled;
    private boolean autoSupplyEnabled;

    private Button supplyButton;
    private Button autoButton;

    public MagicCrystalScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("gui.mugewrayce-mod.magic_crystal.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.loadFromHeld();

        int cx = this.width / 2;
        int y = this.height / 2 - 20;
        this.supplyButton = this.addRenderableWidget(Button.builder(
                        Component.empty(), b -> this.toggleSupply())
                .bounds(cx - 100, y, 200, 20)
                .build());
        this.autoButton = this.addRenderableWidget(Button.builder(
                        Component.empty(), b -> this.toggleAuto())
                .bounds(cx - 100, y + 30, 200, 20)
                .build());
        this.refreshLabels();
    }

    private void loadFromHeld() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            this.supplyEnabled = false;
            this.autoSupplyEnabled = false;
            return;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        CrystalData data = stack.getOrDefault(ModComponents.CRYSTAL_DATA, CrystalData.DEFAULT);
        // 旧数据若已是无效态，界面按“只供能、不自动”显示；服务端仍会拒绝真正的无效写入。
        if (data.autoSupplyEnabled() && !data.supplyEnabled()) {
            this.supplyEnabled = false;
            this.autoSupplyEnabled = false;
        } else {
            this.supplyEnabled = data.supplyEnabled();
            this.autoSupplyEnabled = data.autoSupplyEnabled();
        }
    }

    private void toggleSupply() {
        this.supplyEnabled = !this.supplyEnabled;
        if (!this.supplyEnabled) {
            this.autoSupplyEnabled = false;
        }
        this.refreshLabels();
        this.send();
    }

    private void toggleAuto() {
        if (!this.supplyEnabled) {
            return;
        }
        this.autoSupplyEnabled = !this.autoSupplyEnabled;
        this.refreshLabels();
        this.send();
    }

    private void refreshLabels() {
        if (this.supplyButton != null) {
            this.supplyButton.setMessage(Component.translatable(
                    "gui.mugewrayce-mod.magic_crystal.supply_format",
                    Component.translatable(this.supplyEnabled
                            ? "gui.mugewrayce-mod.common.on"
                            : "gui.mugewrayce-mod.common.off")));
        }
        if (this.autoButton != null) {
            this.autoButton.visible = this.supplyEnabled;
            this.autoButton.active = this.supplyEnabled;
            this.autoButton.setMessage(Component.translatable(
                    "gui.mugewrayce-mod.magic_crystal.auto_supply_format",
                    Component.translatable(this.autoSupplyEnabled
                            ? "gui.mugewrayce-mod.common.on"
                            : "gui.mugewrayce-mod.common.off")));
        }
    }

    private void send() {
        ClientPlayNetworking.send(new CrystalSettingsPayload(this.supplyEnabled, this.autoSupplyEnabled));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.magic_crystal.title"),
                this.width / 2, this.height / 2 - 60, 0xFFFFFFFF);
    }
}
