package com.mugewrayce.sizeadjuster.client;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.magic.exam.ExamText;
import com.mugewrayce.sizeadjuster.payload.ExamHudPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 考试 / 试炼屏幕文字（W15，接口约定 §3.9 / 草案 §9.7）。
 *
 * <p>26.1 没有 {@code HudRenderCallback}（见卡 §6 坑 14），改用
 * {@link HudElementRegistry#addFirst}：本模组文字固定在最上方（D-4 保底方案），
 * 原版 boss bar 位置不动。</p>
 *
 * <p>只渲染两行纯文字（无底图、无图标）；第一行 = 类型|具体项|初学/升N级，
 * 第二行 = mm:ss。整段只有发起者会收到包（§6 坑 7）。</p>
 */
public final class ExamHudRenderer {

    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "exam_hud");
    private static final int TOP = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int COLOR = 0xFFFFFFFF;
    private static final int COLOR_RESULT = 0xFFFFE36B;

    private static ExamHudPayload current;

    private ExamHudRenderer() {
    }

    /** 客户端初始化：注册 HUD 层 + 包接收。 */
    public static void register() {
        HudElementRegistry.addFirst(ID, ExamHudRenderer::render);
    }

    /** 收到 S2C（state=0 清除）。 */
    public static void accept(ExamHudPayload payload) {
        current = payload.state() == ExamHudPayload.STATE_NONE ? null : payload;
    }

    /** 断开连接时清空，避免跨服务器残留。 */
    public static void reset() {
        current = null;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        ExamHudPayload payload = current;
        if (payload == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        Component line1;
        Component line2;
        int color = COLOR;
        switch (payload.state()) {
            case ExamHudPayload.STATE_PASSED -> {
                line1 = Component.translatable("message.mugewrayce-mod.exam_passed");
                line2 = Component.empty();
                color = COLOR_RESULT;
            }
            case ExamHudPayload.STATE_FAILED -> {
                line1 = Component.translatable("message.mugewrayce-mod.exam_failed");
                line2 = Component.empty();
                color = COLOR_RESULT;
            }
            default -> {
                line1 = ExamText.firstLine(payload.typeId(), payload.modeId(), payload.subId(),
                        payload.targetLevel());
                line2 = Component.literal(ExamText.clock(payload.remainingSeconds()));
            }
        }
        int centerX = graphics.guiWidth() / 2;
        graphics.centeredText(minecraft.font, line1, centerX, TOP, color);
        if (line2.getString().isEmpty()) {
            return;
        }
        graphics.centeredText(minecraft.font, line2, centerX, TOP + LINE_HEIGHT, color);
    }
}
