package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.component.MagicBookData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.magic.MagicSkill;
import com.mugewrayce.sizeadjuster.payload.MagicBookSettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔法书设置 GUI（W12；无背景）。
 *
 * <p>i 键在主手魔法书时打开；选择时间暂停 / 空间传送（再点一次取消），
 * 选择范围（区块边长），并通过「目标坐标」「排除目标」进入两个二级界面。
 * W12 只写 {@link MagicBookData}；实际效果/耗魔/范围结算留 W14。</p>
 */
public class MagicBookScreen extends Screen {

    private MagicBookData data;
    private boolean loaded;

    private int skillY;
    private int rangeY;
    private int actionY;
    private final List<Highlight> highlights = new ArrayList<>();

    public MagicBookScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("gui.mugewrayce-mod.magic_book.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        if (!this.loaded) {
            this.loaded = true;
            this.loadFromHeld();
        }
        this.rebuild();
    }

    private void loadFromHeld() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            this.data = MagicBookData.DEFAULT;
            return;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        this.data = stack.getOrDefault(ModComponents.MAGIC_BOOK_DATA, MagicBookData.DEFAULT);
    }

    private void rebuild() {
        this.clearWidgets();
        this.highlights.clear();

        this.skillY = this.height / 2 - 92;
        this.rangeY = this.skillY + 44;
        this.actionY = this.rangeY + 46;

        MagicSkill selected = this.selectedSkill();
        int cx = this.width / 2;

        MagicSkill[] skills = {MagicSkill.TIME_PAUSE, MagicSkill.SPACE_TELEPORT};
        for (int i = 0; i < skills.length; i++) {
            MagicSkill skill = skills[i];
            boolean isSelected = selected == skill;
            Button button = this.addRenderableWidget(Button.builder(
                            Component.translatable(skill.langKey()),
                            b -> this.toggleSkill(skill))
                    .bounds(cx - 125 + i * 130, this.skillY, 120, 20)
                    .build());
            this.highlights.add(new Highlight(button, isSelected));
        }

        if (selected != null) {
            int max = MagicBookData.maxRange(selected.id());
            int startX = cx - (max * 36 - 4) / 2;
            for (int range = 1; range <= max; range++) {
                final int value = range;
                Button button = this.addRenderableWidget(Button.builder(
                                Component.literal(value + "×" + value),
                                b -> this.selectRange(value))
                        .bounds(startX + (range - 1) * 36, this.rangeY, 32, 20)
                        .build());
                this.highlights.add(new Highlight(button, this.data.range() == value));
            }
        }

        Button target = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.magic_book.target"),
                        b -> this.openTeleport())
                .bounds(cx - 155, this.actionY, 150, 20)
                .build());
        target.active = selected == MagicSkill.SPACE_TELEPORT;

        Button excluded = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.magic_book.excluded"),
                        b -> this.openExclusion())
                .bounds(cx + 5, this.actionY, 150, 20)
                .build());
        excluded.active = selected == MagicSkill.TIME_PAUSE;
    }

    private MagicSkill selectedSkill() {
        return this.data == null ? null : MagicSkill.fromId(this.data.skill());
    }

    private void toggleSkill(MagicSkill skill) {
        if (this.data == null) {
            this.data = MagicBookData.DEFAULT;
        }
        String next = skill.id().equals(this.data.skill()) ? "" : skill.id();
        int range = 0;
        if (!next.isEmpty()) {
            int max = MagicBookData.maxRange(next);
            range = this.data.range() <= 0 ? 1 : Math.min(max, this.data.range());
        }
        this.data = new MagicBookData(next, range,
                this.data.targetX(), this.data.targetY(), this.data.targetZ(), this.data.excluded());
        this.send();
        this.rebuild();
    }

    private void selectRange(int range) {
        MagicSkill selected = this.selectedSkill();
        if (selected == null || this.data == null) {
            return;
        }
        if (range < 1 || range > MagicBookData.maxRange(selected.id())) {
            return;
        }
        this.data = new MagicBookData(this.data.skill(), range,
                this.data.targetX(), this.data.targetY(), this.data.targetZ(), this.data.excluded());
        this.send();
        this.rebuild();
    }

    private void openTeleport() {
        if (this.minecraft == null || this.selectedSkill() != MagicSkill.SPACE_TELEPORT) {
            return;
        }
        this.minecraft.setScreen(new MagicBookTeleportScreen(this, this.data));
    }

    private void openExclusion() {
        if (this.minecraft == null || this.selectedSkill() != MagicSkill.TIME_PAUSE) {
            return;
        }
        this.minecraft.setScreen(new MagicBookExclusionScreen(this, this.data));
    }

    /** 二级界面保存后回写本地模型（服务端已由二级界面自己发包）。 */
    public void applyData(MagicBookData newData) {
        if (newData == null) {
            return;
        }
        this.data = newData;
        this.rebuild();
    }

    private void send() {
        if (this.data == null) {
            return;
        }
        ClientPlayNetworking.send(new MagicBookSettingsPayload(
                this.data.skill(), this.data.range(),
                this.data.targetX(), this.data.targetY(), this.data.targetZ(),
                this.data.excluded()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;

        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.magic_book.title"),
                cx, this.skillY - 42, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.magic_book.skill"),
                cx, this.skillY - 16, 0xFF00AAFF);

        MagicSkill selected = this.selectedSkill();
        if (selected == null) {
            graphics.centeredText(this.font,
                    Component.translatable("gui.mugewrayce-mod.magic_book.no_skill_hint"),
                    cx, this.actionY + 30, 0xFFFFAA00);
        } else {
            graphics.centeredText(this.font,
                    Component.translatable("gui.mugewrayce-mod.magic_book.range"),
                    cx, this.rangeY - 16, 0xFF00AAFF);
            if (this.data.range() > 0) {
                graphics.centeredText(this.font,
                        Component.translatable("gui.mugewrayce-mod.magic_book.range_format",
                                this.data.range(), this.data.range()),
                        cx, this.actionY + 30, 0xFFAAAAAA);
            }
        }

        for (Highlight highlight : this.highlights) {
            if (!highlight.selected() || !highlight.button().visible) {
                continue;
            }
            int x = highlight.button().getX();
            int y = highlight.button().getY();
            int w = highlight.button().getWidth();
            int h = highlight.button().getHeight();
            graphics.fill(x - 2, y - 2, x + w + 2, y, 0xFF00AAFF);
            graphics.fill(x - 2, y + h, x + w + 2, y + h + 2, 0xFF00AAFF);
            graphics.fill(x - 2, y - 2, x, y + h + 2, 0xFF00AAFF);
            graphics.fill(x + w, y - 2, x + w + 2, y + h + 2, 0xFF00AAFF);
        }
    }

    private record Highlight(Button button, boolean selected) {
    }
}
