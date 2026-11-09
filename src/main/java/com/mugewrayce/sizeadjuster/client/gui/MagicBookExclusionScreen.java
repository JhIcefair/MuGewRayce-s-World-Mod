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
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔法书二级界面 2：时间暂停排除目标（W12；无背景）。
 *
 * <p>主界面「排除目标」进入；可输入实体 UUID 或实体类型 ID，分别加
 * {@code entity:} / {@code type:} 前缀写入 {@link MagicBookData#excluded()}。
 * 左上角返回键回主界面；W14 再解析列表并结算时间暂停。</p>
 */
public class MagicBookExclusionScreen extends Screen {

    private static final int MAX_EXCLUDED = 64;
    private static final int MAX_ENTRY_LENGTH = 128;
    private static final int MAX_VISIBLE = 8;

    private final MagicBookScreen parent;
    private MagicBookData data;

    private EditBox input;
    private String inputText = "";
    private Component error;
    private int listTop;

    public MagicBookExclusionScreen(MagicBookScreen parent, MagicBookData data) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("gui.mugewrayce-mod.magic_book.exclusion.title"));
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
        this.rebuild();
    }

    private void rebuild() {
        this.clearWidgets();
        this.addRenderableWidget(new BackButton(5, 5, this::goBack));

        int cx = this.width / 2;
        int inputY = this.height / 2 - 82;
        this.input = new EditBox(this.font, cx - 100, inputY, 200, 20,
                Component.translatable("gui.mugewrayce-mod.magic_book.exclusion.input_hint"));
        this.input.setMaxLength(MAX_ENTRY_LENGTH);
        this.input.setValue(this.inputText);
        this.input.setResponder(s -> {
            this.inputText = s;
            this.error = null;
        });
        this.addRenderableWidget(this.input);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.magic_book.exclusion.add_entity"),
                        b -> this.addEntry("entity:"))
                .bounds(cx - 100, inputY + 28, 95, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.magic_book.exclusion.add_type"),
                        b -> this.addEntry("type:"))
                .bounds(cx + 5, inputY + 28, 95, 20)
                .build());

        this.listTop = inputY + 58;
        List<String> excluded = this.data.excluded();
        int visible = Math.min(excluded.size(), MAX_VISIBLE);
        for (int i = 0; i < visible; i++) {
            final int index = i;
            this.addRenderableWidget(Button.builder(
                            Component.translatable("gui.mugewrayce-mod.common.remove"),
                            b -> this.removeEntry(index))
                    .bounds(cx + 104, this.listTop + i * 20, 48, 16)
                    .build());
        }

        // W14b：时间暂停没有别的释放入口，这里补「确认」（复用 common.confirm，不新增键）
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.common.confirm"),
                        b -> this.confirm())
                .bounds(cx - 60, this.height - 30, 120, 20)
                .build());
    }

    private void addEntry(String prefix) {
        String value = this.inputText == null ? "" : this.inputText.trim();
        if (value.isEmpty()) {
            this.error = Component.translatable("gui.mugewrayce-mod.common.invalid_input");
            return;
        }
        String entry = prefix + value;
        if (entry.length() > MAX_ENTRY_LENGTH) {
            this.error = Component.translatable("gui.mugewrayce-mod.common.invalid_input");
            return;
        }
        if (this.data.excluded().contains(entry)) {
            return;
        }
        if (this.data.excluded().size() >= MAX_EXCLUDED) {
            this.error = Component.translatable("gui.mugewrayce-mod.common.invalid_input");
            return;
        }
        List<String> next = new ArrayList<>(this.data.excluded());
        next.add(entry);
        this.data = new MagicBookData(this.data.skill(), this.data.range(),
                this.data.targetX(), this.data.targetY(), this.data.targetZ(), next);
        this.inputText = "";
        this.error = null;
        this.send();
        this.rebuild();
    }

    private void removeEntry(int index) {
        if (index < 0 || index >= this.data.excluded().size()) {
            return;
        }
        List<String> next = new ArrayList<>(this.data.excluded());
        next.remove(index);
        this.data = new MagicBookData(this.data.skill(), this.data.range(),
                this.data.targetX(), this.data.targetY(), this.data.targetZ(), next);
        this.send();
        this.rebuild();
    }

    private void send() {
        ClientPlayNetworking.send(new MagicBookSettingsPayload(
                this.data.skill(), this.data.range(),
                this.data.targetX(), this.data.targetY(), this.data.targetZ(),
                this.data.excluded()));
        if (this.parent != null) {
            this.parent.applyData(this.data);
        }
    }

    /** 先写参数、再发释放；服务端会重新校验。 */
    private void confirm() {
        ClientPlayNetworking.send(new MagicBookSettingsPayload(
                this.data.skill(), this.data.range(),
                this.data.targetX(), this.data.targetY(), this.data.targetZ(),
                this.data.excluded()));
        ClientPlayNetworking.send(MagicBookCastPayload.INSTANCE);
        this.goBack();
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;

        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.magic_book.exclusion.title"),
                cx, this.height / 2 - 112, 0xFFFFFFFF);

        List<String> excluded = this.data.excluded();
        if (excluded.isEmpty()) {
            graphics.centeredText(this.font,
                    Component.translatable("gui.mugewrayce-mod.magic_book.exclusion.empty"),
                    cx, this.listTop + 4, 0xFFAAAAAA);
        } else {
            int visible = Math.min(excluded.size(), MAX_VISIBLE);
            for (int i = 0; i < visible; i++) {
                graphics.text(this.font, Component.literal(excluded.get(i)),
                        cx - 100, this.listTop + i * 20 + 4, 0xFFFFFFFF);
            }
            if (excluded.size() > MAX_VISIBLE) {
                graphics.text(this.font, Component.literal("..."),
                        cx - 100, this.listTop + MAX_VISIBLE * 20 + 4, 0xFFAAAAAA);
            }
        }

        if (this.error != null) {
            graphics.centeredText(this.font, this.error, cx, this.height - 48, 0xFFFF5555);
        }
    }
}
