package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.payload.ManaDetectorRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Arrays;

/**
 * 魔力值探测器 GUI（W12；无背景）。
 *
 * <p>右键探测器打开并立即探测一次；5×5 个数值固定“上北下南左西右东”，
 * 不显示区块坐标。服务端在请求瞬间取样、3 秒后回包，期间显示“正在探测”。
 * 关闭界面 / 换维度 / 死亡会清空本次数值。W12 的取样是占位（显示 {@code --}），
 * 真实区块魔力在 W13。</p>
 */
public class ManaDetectorScreen extends Screen {

    private static final int GRID = 5;

    private final int[] values = new int[GRID * GRID];
    private boolean requested;
    private boolean probing;
    private boolean complete;

    private Button probeButton;
    private ResourceKey<Level> dimension;

    public ManaDetectorScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("gui.mugewrayce-mod.mana_detector.title"));
        Arrays.fill(this.values, -1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.dimension = this.minecraft != null && this.minecraft.level != null
                ? this.minecraft.level.dimension() : null;

        int cx = this.width / 2;
        int y = this.height / 2 + 84;
        this.probeButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.mana_detector.probe"),
                        b -> this.startProbe())
                .bounds(cx - 105, y, 100, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.common.close"),
                        b -> this.onClose())
                .bounds(cx + 5, y, 100, 20)
                .build());

        if (!this.requested) {
            this.requested = true;
            this.startProbe();
        } else {
            this.probeButton.active = !this.probing;
        }
    }

    private void startProbe() {
        if (this.probing) {
            return;
        }
        Arrays.fill(this.values, -1);
        this.probing = true;
        this.complete = false;
        if (this.probeButton != null) {
            this.probeButton.active = false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ChunkPos center = minecraft.player.chunkPosition();
        ClientPlayNetworking.send(new ManaDetectorRequestPayload(center.x(), center.z()));
    }

    /** 服务端回包（客户端接收器调用；只接受 25 个值）。 */
    public void acceptResult(int[] result, boolean complete) {
        if (result != null && result.length == this.values.length) {
            System.arraycopy(result, 0, this.values, 0, this.values.length);
        }
        this.probing = false;
        this.complete = complete;
        if (this.probeButton != null) {
            this.probeButton.active = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        // 关闭/换维度/死亡 → 清空数值（等于没探测成功）
        if (minecraft.player.isDeadOrDying()) {
            this.onClose();
            return;
        }
        if (this.dimension != null && minecraft.level != null
                && !this.dimension.equals(minecraft.level.dimension())) {
            this.onClose();
        }
    }

    @Override
    public void removed() {
        Arrays.fill(this.values, -1);
        this.probing = false;
        this.complete = false;
        super.removed();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;

        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.mana_detector.title"),
                cx, this.height / 2 - 104, 0xFFFFFFFF);
        if (this.probing) {
            graphics.centeredText(this.font,
                    Component.translatable("gui.mugewrayce-mod.mana_detector.probing"),
                    cx, this.height / 2 - 84, 0xFFFFAA00);
        } else if (this.complete) {
            graphics.centeredText(this.font,
                    Component.translatable("gui.mugewrayce-mod.mana_detector.complete"),
                    cx, this.height / 2 - 84, 0xFF55FF55);
        }

        int gridTop = this.height / 2 - 76;
        int buttonTop = this.height / 2 + 84;
        int minCell = this.font.width("99999") + 12;
        int cellByHeight = Math.max(1, (buttonTop - 12 - gridTop) / GRID);
        int cellByWidth = Math.max(1, (this.width - 40) / GRID);
        int cell = Math.max(minCell, Math.min(64, Math.min(cellByHeight, cellByWidth)));
        int availableW = Math.max(1, this.width - 16);
        int availableH = Math.max(1, buttonTop - 12 - gridTop);
        float scale = Math.min(1.0F, Math.min(
                (float) availableW / (cell * GRID),
                (float) availableH / (cell * GRID)));
        float gridW = cell * GRID * scale;
        float gridH = cell * GRID * scale;
        float startX = (this.width - gridW) / 2.0F;
        float startY = gridTop + (availableH - gridH) / 2.0F;

        graphics.pose().pushMatrix();
        graphics.pose().translate(startX, startY);
        graphics.pose().scale(scale, scale);
        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                int index = row * GRID + col;
                int value = this.values[index];
                String text = value < 0 ? "--" : Integer.toString(value);
                graphics.centeredText(this.font, Component.literal(text),
                        col * cell + cell / 2,
                        row * cell + cell / 2,
                        0xFFFFFFFF);
            }
        }
        graphics.pose().popMatrix();
    }
}
