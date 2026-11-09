package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.component.MagicBookData;
import com.mugewrayce.sizeadjuster.payload.MagicBookCastPayload;
import com.mugewrayce.sizeadjuster.payload.MagicBookSettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 魔法书二级界面 1：空间传送目标坐标（W12；无背景）。
 *
 * <p>主界面「目标坐标」进入；输绝对坐标，确认后发 C2S 写
 * {@link MagicBookData#targetX()} / {@code targetY} / {@code targetZ}。
 * 左上角返回键回主界面；效果结算留 W14。</p>
 */
public class MagicBookTeleportScreen extends Screen {

    private static final int MAX_COORDINATE = 30_000_000;

    private final MagicBookScreen parent;
    private MagicBookData data;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private Component error;

    public MagicBookTeleportScreen(MagicBookScreen parent, MagicBookData data) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("gui.mugewrayce-mod.magic_book.teleport.title"));
        this.parent = parent;
        this.data = data == null ? MagicBookData.DEFAULT : data;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new BackButton(5, 5, this::goBack));

        int cx = this.width / 2;
        int y = this.height / 2 - 30;
        int boxWidth = 60;
        int gap = 10;
        int startX = cx - (boxWidth * 3 + gap * 2) / 2;
        this.xBox = this.createBox(startX, y, boxWidth, "X", this.data.targetX());
        this.yBox = this.createBox(startX + boxWidth + gap, y, boxWidth, "Y", this.data.targetY());
        this.zBox = this.createBox(startX + (boxWidth + gap) * 2, y, boxWidth, "Z", this.data.targetZ());
        this.setInitialFocus(this.xBox);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.common.confirm"),
                        b -> this.confirm())
                .bounds(cx - 60, y + 44, 120, 20)
                .build());
    }

    private EditBox createBox(int x, int y, int width, String label, int value) {
        EditBox box = new EditBox(this.font, x, y, width, 20, Component.literal(label));
        box.setMaxLength(12);
        box.setValue(Integer.toString(value));
        box.setResponder(s -> this.error = null);
        this.addRenderableWidget(box);
        return box;
    }

    private void confirm() {
        try {
            int x = Integer.parseInt(this.xBox.getValue().trim());
            int y = Integer.parseInt(this.yBox.getValue().trim());
            int z = Integer.parseInt(this.zBox.getValue().trim());
            if (!validCoordinate(x) || !validCoordinate(y) || !validCoordinate(z)) {
                this.error = Component.translatable("gui.mugewrayce-mod.common.invalid_input");
                return;
            }
            MagicBookData next = new MagicBookData(this.data.skill(), this.data.range(),
                    x, y, z, this.data.excluded());
            ClientPlayNetworking.send(new MagicBookSettingsPayload(
                    next.skill(), next.range(), next.targetX(), next.targetY(), next.targetZ(),
                    next.excluded()));
            ClientPlayNetworking.send(MagicBookCastPayload.INSTANCE);
            if (this.parent != null) {
                this.parent.applyData(next);
            }
            this.goBack();
        } catch (NumberFormatException ignored) {
            this.error = Component.translatable("gui.mugewrayce-mod.common.invalid_input");
        }
    }

    private static boolean validCoordinate(int value) {
        return Math.abs((long) value) <= MAX_COORDINATE;
    }

    private void goBack() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
    @Override
    public void onClose() {
        this.goBack();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.confirm();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;

        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.magic_book.teleport.title"),
                cx, this.height / 2 - 86, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.magic_book.teleport.hint"),
                cx, this.height / 2 - 66, 0xFFAAAAAA);

        int boxWidth = 60;
        int gap = 10;
        int startX = cx - (boxWidth * 3 + gap * 2) / 2;
        int y = this.height / 2 - 44;
        graphics.centeredText(this.font, Component.literal("X"), startX + boxWidth / 2, y, 0xFF00AAFF);
        graphics.centeredText(this.font, Component.literal("Y"),
                startX + boxWidth + gap + boxWidth / 2, y, 0xFF00AAFF);
        graphics.centeredText(this.font, Component.literal("Z"),
                startX + (boxWidth + gap) * 2 + boxWidth / 2, y, 0xFF00AAFF);

        if (this.error != null) {
            graphics.centeredText(this.font, this.error, cx, this.height / 2 + 46, 0xFFFF5555);
        }
    }
}
