package com.mugewrayce.sizeadjuster.client;

import com.mugewrayce.sizeadjuster.payload.RoleActionPayload;
import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 角色切换 GUI（无背景贴图）：
 * 列表展示全部可选角色：默认 / 魔法体 / 特殊-icefair（将来新增特殊角色自动追加），
 * 点击即切换并关闭 GUI。
 * - 柃之册：功能列表首位"角色切换"打开（第二级 GUI）；
 * - 生命转换令牌：右键直接打开（第一级 GUI）。
 * 打开时自动向服务端查询一次当前角色；服务端回发（RoleStatusPayload）
 * 刷新高亮与"当前角色"文字。
 *
 * 注意：26.1 中 GuiGraphicsExtractor 的文本绘制要求颜色 alpha 非 0
 * （ARGB.alpha(color)==0 时直接跳过绘制），自定义文本颜色必须用 0xFF 开头。
 */
public class RoleSwitchScreen extends Screen {

    /** 客户端当前角色编号（回发/点击后更新，用于高亮与"当前角色"显示）。 */
    public static int clientRoleCode = PlayerRoles.ROLE_DEFAULT;

    /** 打开的角色切换 GUI 收到服务端回发时刷新静态当前角色。 */
    public static void onRoleStatus(int role) {
        clientRoleCode = role;
    }

    public RoleSwitchScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("role_switch"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        List<Integer> roles = PlayerRoles.allRoleCodes();
        int buttonWidth = 160;
        int buttonHeight = 20;
        int pitch = 28;
        int firstY = this.height / 2 - (roles.size() - 1) * pitch / 2 - buttonHeight / 2;
        int x = this.width / 2 - buttonWidth / 2;
        for (int i = 0; i < roles.size(); i++) {
            int index = i;
            this.addRenderableWidget(Button.builder(PlayerRoles.roleComponent(roles.get(i)), b -> selectRole(index))
                    .bounds(x, firstY + i * pitch, buttonWidth, buttonHeight)
                    .build());
        }
        // 向服务端查询当前角色（回发刷新高亮与文字）
        ClientPlayNetworking.send(new RoleActionPayload(RoleActionPayload.ACTION_QUERY, 0));
    }

    private void selectRole(int optionIndex) {
        int role = PlayerRoles.optionToRole(optionIndex);
        clientRoleCode = role;
        ClientPlayNetworking.send(new RoleActionPayload(RoleActionPayload.ACTION_SELECT, role));
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        List<Integer> roles = PlayerRoles.allRoleCodes();
        int buttonWidth = 160;
        int buttonHeight = 20;
        int pitch = 28;
        int firstY = this.height / 2 - (roles.size() - 1) * pitch / 2 - buttonHeight / 2;
        int x = this.width / 2 - buttonWidth / 2;

        // 标题
        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.role.title"),
                this.width / 2, firstY - 32, 0xFFFFFFFF);

        // 当前角色：高亮边框（不遮按钮文字）+ 下方文字
        int currentIndex = optionIndexOf(clientRoleCode);
        if (currentIndex >= 0 && currentIndex < roles.size()) {
            int cy = firstY + currentIndex * pitch;
            graphics.fill(x - 2, cy - 2, x + buttonWidth + 2, cy, 0xFF00AAFF);
            graphics.fill(x - 2, cy + buttonHeight, x + buttonWidth + 2, cy + buttonHeight + 2, 0xFF00AAFF);
            graphics.fill(x - 2, cy - 2, x, cy + buttonHeight + 2, 0xFF00AAFF);
            graphics.fill(x + buttonWidth, cy - 2, x + buttonWidth + 2, cy + buttonHeight + 2, 0xFF00AAFF);
        }
        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.role.current",
                        PlayerRoles.roleComponent(clientRoleCode)),
                this.width / 2, firstY + roles.size() * pitch + 6, 0xFFFFFFFF);
    }

    private static int optionIndexOf(int role) {
        if (role < PlayerRoles.ROLE_SPECIAL_BASE) {
            return role;
        }
        return 2 + (role - PlayerRoles.ROLE_SPECIAL_BASE);
    }
}
