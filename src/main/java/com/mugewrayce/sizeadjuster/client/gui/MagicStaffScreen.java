package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.component.CertEntry;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.component.StaffData;
import com.mugewrayce.sizeadjuster.component.StaffSelectionData;
import com.mugewrayce.sizeadjuster.magic.CastMode;
import com.mugewrayce.sizeadjuster.magic.CastSubMode;
import com.mugewrayce.sizeadjuster.magic.MagicSkill;
import com.mugewrayce.sizeadjuster.magic.MagicType;
import com.mugewrayce.sizeadjuster.payload.MagicStaffSelectionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 普通魔法杖设置 GUI（W12；无背景）。
 *
 * <p>i 键在主手普通魔法杖时打开；三行逐级只列 {@link StaffData#entries()}
 * 已附加的内容：类型 → 使用模式 → 具体项（点式无第三行）。
 * 点击只改选择，按「确认」或 Enter / 小键盘 Enter 才发送并关闭；
 * 服务端会重新校验三元组确实在该法杖上。</p>
 */
public class MagicStaffScreen extends Screen {

    private static final int LABEL_X = 20;
    private static final int CONTENT_X = 120;
    private static final int ROW_GAP = 40;

    private StaffData staff;
    private boolean loaded;

    private String selectedType = "";
    private String selectedMode = "";
    private String selectedSub = "";

    private Button confirmButton;
    private int rowTypeY;
    private int rowModeY;
    private int rowSubY;
    private final List<Highlight> highlights = new ArrayList<>();

    public MagicStaffScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("gui.mugewrayce-mod.magic_staff.title"));
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
            this.staff = StaffData.DEFAULT;
            return;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        this.staff = stack.getOrDefault(ModComponents.STAFF_DATA, StaffData.DEFAULT);
        StaffSelectionData selection =
                stack.getOrDefault(ModComponents.STAFF_SELECTION_DATA, StaffSelectionData.DEFAULT);
        if (!selection.isEmpty()
                && this.contains(selection.type(), selection.mode(), selection.sub())) {
            this.selectedType = selection.type();
            this.selectedMode = selection.mode();
            this.selectedSub = selection.sub();
        }
    }

    private void rebuild() {
        this.clearWidgets();
        this.highlights.clear();

        this.rowTypeY = this.height / 2 - 78;
        this.rowModeY = this.rowTypeY + ROW_GAP;
        this.rowSubY = this.rowModeY + ROW_GAP;

        this.buildTypeRow();
        this.buildModeRow();
        this.buildSubRow();

        this.confirmButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.common.confirm"),
                        b -> this.confirm())
                .bounds(this.width / 2 - 60, this.rowSubY + 46, 120, 20)
                .build());
        this.updateConfirmState();
    }

    private void buildTypeRow() {
        List<MagicType> types = this.availableTypes();
        int y = this.rowTypeY;
        int width = this.rowButtonWidth(types.size());
        int x = CONTENT_X;
        for (MagicType type : types) {
            boolean selected = type.id().equals(this.selectedType);
            Button button = this.addRenderableWidget(Button.builder(
                            Component.translatable(type.langKey()),
                            b -> this.selectType(type.id()))
                    .bounds(x, y, width, 20)
                    .build());
            this.highlights.add(new Highlight(button, selected));
            x += width + 4;
        }
    }

    private void buildModeRow() {
        MagicType type = MagicType.fromId(this.selectedType);
        if (type == null) {
            return;
        }
        List<CastMode> modes = this.availableModes(type);
        int y = this.rowModeY;
        int width = this.rowButtonWidth(modes.size());
        int x = CONTENT_X;
        for (CastMode mode : modes) {
            boolean selected = mode.id().equals(this.selectedMode);
            Button button = this.addRenderableWidget(Button.builder(
                            Component.translatable(mode.langKey()),
                            b -> this.selectMode(mode.id()))
                    .bounds(x, y, width, 20)
                    .build());
            this.highlights.add(new Highlight(button, selected));
            x += width + 4;
        }
    }

    private void buildSubRow() {
        MagicType type = MagicType.fromId(this.selectedType);
        CastMode mode = CastMode.fromId(this.selectedMode);
        if (type == null || mode == null || mode == CastMode.POINT) {
            return;
        }
        List<Component> labels = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        if (mode == CastMode.SKILL) {
            for (MagicSkill skill : MagicSkill.values()) {
                if (type.supports(skill) && this.hasEntry(type.id(), mode.id(), skill.id())) {
                    ids.add(skill.id());
                    labels.add(skill.langKey() == null
                            ? Component.literal(skill.id())
                            : Component.translatable(skill.langKey()));
                }
            }
        } else {
            for (CastSubMode sub : CastSubMode.values()) {
                if (sub.mode() == mode && type.supports(sub)
                        && this.hasEntry(type.id(), mode.id(), sub.id())) {
                    ids.add(sub.id());
                    labels.add(Component.translatable(sub.langKey()));
                }
            }
        }

        int y = this.rowSubY;
        int width = this.rowButtonWidth(labels.size());
        int x = CONTENT_X;
        for (int i = 0; i < labels.size(); i++) {
            String id = ids.get(i);
            boolean selected = id.equals(this.selectedSub);
            Button button = this.addRenderableWidget(Button.builder(labels.get(i),
                            b -> this.selectSub(id))
                    .bounds(x, y, width, 20)
                    .build());
            this.highlights.add(new Highlight(button, selected));
            x += width + 4;
        }
    }

    private List<MagicType> availableTypes() {
        List<MagicType> types = new ArrayList<>();
        for (MagicType type : MagicType.values()) {
            if (!type.isImplemented()) {
                continue;
            }
            if (this.hasEntry(type.id(), null, null)) {
                types.add(type);
            }
        }
        return types;
    }

    private List<CastMode> availableModes(MagicType type) {
        List<CastMode> modes = new ArrayList<>();
        for (CastMode mode : CastMode.values()) {
            if (type.supports(mode) && this.hasEntry(type.id(), mode.id(), null)) {
                modes.add(mode);
            }
        }
        return modes;
    }

    private boolean hasEntry(String type, String mode, String sub) {
        for (CertEntry entry : this.staff.entries()) {
            if (!entry.type().equals(type)) {
                continue;
            }
            if (mode != null && !entry.mode().equals(mode)) {
                continue;
            }
            if (sub != null && !entry.sub().equals(sub)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean contains(String type, String mode, String sub) {
        for (CertEntry entry : this.staff.entries()) {
            if (entry.type().equals(type) && entry.mode().equals(mode) && entry.sub().equals(sub)) {
                return true;
            }
        }
        return false;
    }

    private int rowButtonWidth(int count) {
        if (count <= 0) {
            return 0;
        }
        int gap = 4;
        return Math.min(84, Math.max(36,
                (this.width - CONTENT_X - 20 - gap * (count - 1)) / count));
    }

    private void selectType(String type) {
        if (type.equals(this.selectedType)) {
            return;
        }
        this.selectedType = type;
        this.selectedMode = "";
        this.selectedSub = "";
        this.rebuild();
    }

    private void selectMode(String mode) {
        if (mode.equals(this.selectedMode)) {
            return;
        }
        this.selectedMode = mode;
        this.selectedSub = "";
        this.rebuild();
    }

    private void selectSub(String sub) {
        this.selectedSub = sub;
        this.rebuild();
    }

    private boolean canConfirm() {
        if (this.selectedType.isEmpty() || this.selectedMode.isEmpty()) {
            return false;
        }
        if (!this.selectedMode.equals(CastMode.POINT.id()) && this.selectedSub.isEmpty()) {
            return false;
        }
        String sub = this.selectedMode.equals(CastMode.POINT.id()) ? "" : this.selectedSub;
        return this.contains(this.selectedType, this.selectedMode, sub);
    }

    private void updateConfirmState() {
        if (this.confirmButton != null) {
            this.confirmButton.active = this.canConfirm();
        }
    }

    private void confirm() {
        if (!this.canConfirm()) {
            return;
        }
        String sub = this.selectedMode.equals(CastMode.POINT.id()) ? "" : this.selectedSub;
        ClientPlayNetworking.send(
                new MagicStaffSelectionPayload(this.selectedType, this.selectedMode, sub));
        this.onClose();
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

        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.magic_staff.title"),
                this.width / 2, this.rowTypeY - 54, 0xFFFFFFFF);

        if (this.staff.entries().isEmpty()) {
            graphics.centeredText(this.font,
                    Component.translatable("gui.mugewrayce-mod.magic_staff.empty"),
                    this.width / 2, this.height / 2, 0xFFFFAA00);
            return;
        }

        graphics.text(this.font,
                Component.translatable("gui.mugewrayce-mod.magic_staff.row_type"),
                LABEL_X, this.rowTypeY + 6, 0xFF00AAFF);
        if (!this.selectedType.isEmpty()) {
            graphics.text(this.font,
                    Component.translatable("gui.mugewrayce-mod.magic_staff.row_mode"),
                    LABEL_X, this.rowModeY + 6, 0xFF00AAFF);
        }
        CastMode mode = CastMode.fromId(this.selectedMode);
        if (mode != null && mode != CastMode.POINT) {
            graphics.text(this.font,
                    Component.translatable("gui.mugewrayce-mod.magic_staff.row_sub"),
                    LABEL_X, this.rowSubY + 6, 0xFF00AAFF);
        }
        if (!this.canConfirm()) {
            // W19：提示移到具体项行下方（确认键上方），不再压住模式按钮；
            // 点式没有“具体项”，文案改成只提类型与模式。
            boolean pointMode = mode == CastMode.POINT;
            graphics.centeredText(this.font,
                    Component.translatable(pointMode
                            ? "gui.mugewrayce-mod.magic_staff.incomplete_mode"
                            : "gui.mugewrayce-mod.magic_staff.incomplete"),
                    this.width / 2, this.rowSubY + 28, 0xFFAAAAAA);
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
