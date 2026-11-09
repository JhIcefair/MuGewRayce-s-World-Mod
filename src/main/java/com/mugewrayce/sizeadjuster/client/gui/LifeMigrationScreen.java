package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.payload.LifeMigrationPayload;
import com.mugewrayce.sizeadjuster.payload.RoleActionPayload;
import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 生命迁移石 GUI（W12；无背景）。
 *
 * <p>右键迁移石打开；只列默认 / 有生命的魔法体（不含特殊角色）。
 * 打开时查询当前角色；点「确认」发 C2S，服务端再次校验 role 0/1，
 * 真正发生转换时消耗 1 个迁移石。失败/非法/无变化不消耗。</p>
 */
public class LifeMigrationScreen extends Screen {

    private int currentRole = -1;
    private int selectedRole = -1;

    private Button confirmButton;
    private Button optionDefault;
    private Button optionMagicBody;
    private int optionY;

    public LifeMigrationScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("gui.mugewrayce-mod.life_migration_stone.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        this.optionY = this.height / 2 - 16;

        this.optionDefault = this.addRenderableWidget(Button.builder(
                        PlayerRoles.roleComponent(PlayerRoles.ROLE_DEFAULT),
                        b -> this.selectRole(PlayerRoles.ROLE_DEFAULT))
                .bounds(cx - 100, this.optionY, 200, 20)
                .build());
        this.optionMagicBody = this.addRenderableWidget(Button.builder(
                        PlayerRoles.roleComponent(PlayerRoles.ROLE_MAGIC_BODY),
                        b -> this.selectRole(PlayerRoles.ROLE_MAGIC_BODY))
                .bounds(cx - 100, this.optionY + 30, 200, 20)
                .build());

        this.confirmButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mugewrayce-mod.common.confirm"),
                        b -> this.confirm())
                .bounds(cx - 60, this.optionY + 70, 120, 20)
                .build());

        this.refresh();
        ClientPlayNetworking.send(new RoleActionPayload(RoleActionPayload.ACTION_QUERY, 0));
    }

    /** 服务端回发当前角色（客户端接收器调用）。 */
    public void onRoleStatus(int role) {
        this.currentRole = role;
        if (this.selectedRole < 0
                && (role == PlayerRoles.ROLE_DEFAULT || role == PlayerRoles.ROLE_MAGIC_BODY)) {
            this.selectedRole = role;
        }
        this.refresh();
    }

    private void selectRole(int role) {
        if (role != PlayerRoles.ROLE_DEFAULT && role != PlayerRoles.ROLE_MAGIC_BODY) {
            return;
        }
        this.selectedRole = role;
        this.refresh();
    }

    private void refresh() {
        if (this.confirmButton != null) {
            boolean valid = this.selectedRole == PlayerRoles.ROLE_DEFAULT
                    || this.selectedRole == PlayerRoles.ROLE_MAGIC_BODY;
            this.confirmButton.active = valid && this.selectedRole != this.currentRole;
        }
    }

    private void confirm() {
        if (this.selectedRole != PlayerRoles.ROLE_DEFAULT
                && this.selectedRole != PlayerRoles.ROLE_MAGIC_BODY) {
            return;
        }
        ClientPlayNetworking.send(new LifeMigrationPayload(this.selectedRole));
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;

        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.life_migration_stone.title"),
                cx, this.optionY - 84, 0xFFFFFFFF);
        if (this.currentRole >= 0) {
            graphics.centeredText(this.font,
                    Component.translatable("gui.mugewrayce-mod.role.current",
                            PlayerRoles.roleComponent(this.currentRole)),
                    cx, this.optionY - 64, 0xFFAAAAAA);
        }
        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.life_migration_stone.hint"),
                cx, this.optionY - 44, 0xFFFFAA00);

        if (this.selectedRole == PlayerRoles.ROLE_DEFAULT && this.optionDefault != null) {
            drawFrame(graphics, this.optionDefault);
        } else if (this.selectedRole == PlayerRoles.ROLE_MAGIC_BODY && this.optionMagicBody != null) {
            drawFrame(graphics, this.optionMagicBody);
        }
    }

    private static void drawFrame(GuiGraphicsExtractor graphics, Button button) {
        int x = button.getX();
        int y = button.getY();
        int w = button.getWidth();
        int h = button.getHeight();
        graphics.fill(x - 2, y - 2, x + w + 2, y, 0xFF00AAFF);
        graphics.fill(x - 2, y + h, x + w + 2, y + h + 2, 0xFF00AAFF);
        graphics.fill(x - 2, y - 2, x, y + h + 2, 0xFF00AAFF);
        graphics.fill(x + w, y - 2, x + w + 2, y + h + 2, 0xFF00AAFF);
    }
}
