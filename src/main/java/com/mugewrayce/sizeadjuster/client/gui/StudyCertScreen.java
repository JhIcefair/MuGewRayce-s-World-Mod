package com.mugewrayce.sizeadjuster.client.gui;

import com.mugewrayce.sizeadjuster.component.CertData;
import com.mugewrayce.sizeadjuster.component.CertEntry;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.magic.CastMode;
import com.mugewrayce.sizeadjuster.magic.CastSubMode;
import com.mugewrayce.sizeadjuster.magic.MagicDataRules;
import com.mugewrayce.sizeadjuster.magic.MagicLevel;
import com.mugewrayce.sizeadjuster.magic.MagicSkill;
import com.mugewrayce.sizeadjuster.magic.MagicType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * 魔法学习证查看 GUI（W12；无背景、只读）。
 *
 * <p>右键学习证打开；显示编号 + 每条 type/mode/sub/level。
 * 技能条目不显示等级；其他条目等级用文字（初学者 / I~V）。
 * 无组件 / no<=0 / 内容非法 → 显示“无效学习证”，不崩溃。</p>
 */
public class StudyCertScreen extends Screen {

    private CertData cert;
    private boolean valid;

    public StudyCertScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("gui.mugewrayce-mod.magic_study_certificate.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            this.cert = null;
            this.valid = false;
            return;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        this.cert = stack.get(ModComponents.CERT_DATA);
        this.valid = this.cert != null && MagicDataRules.isValidCert(this.cert);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;

        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.magic_study_certificate.title"),
                cx, this.height / 2 - 100, 0xFFFFFFFF);

        if (!this.valid || this.cert == null) {
            graphics.centeredText(this.font,
                    Component.translatable("gui.mugewrayce-mod.magic_study_certificate.invalid"),
                    cx, this.height / 2 - 40, 0xFFFF5555);
            return;
        }

        graphics.centeredText(this.font,
                Component.translatable("gui.mugewrayce-mod.magic_study_certificate.number",
                        String.format(Locale.ROOT, "%04d", this.cert.no())),
                cx, this.height / 2 - 70, 0xFFFFAA00);

        int startY = this.height / 2 - 42;
        for (int i = 0; i < this.cert.entries().size(); i++) {
            CertEntry entry = this.cert.entries().get(i);
            graphics.centeredText(this.font, describe(entry),
                    cx, startY + i * 16, 0xFFFFFFFF);
        }
    }

    /** 一条学习记录：类型 · 模式 [· 具体项] [等级]；技能不显示等级。 */
    private static Component describe(CertEntry entry) {
        MutableComponent line = Component.empty();

        MagicType type = MagicType.fromId(entry.type());
        line.append(type == null
                ? Component.literal(entry.type())
                : Component.translatable(type.langKey()));

        CastMode mode = CastMode.fromId(entry.mode());
        line.append(" · ");
        line.append(mode == null
                ? Component.literal(entry.mode())
                : Component.translatable(mode.langKey()));

        if (mode == CastMode.SKILL) {
            MagicSkill skill = MagicSkill.fromId(entry.sub());
            line.append(" · ");
            if (skill == null || skill.langKey() == null) {
                line.append(Component.literal(entry.sub()));
            } else {
                line.append(Component.translatable(skill.langKey()));
            }
        } else if (mode != CastMode.POINT && entry.sub() != null && !entry.sub().isEmpty()) {
            CastSubMode sub = CastSubMode.fromId(entry.sub());
            line.append(" · ");
            line.append(sub == null
                    ? Component.literal(entry.sub())
                    : Component.translatable(sub.langKey()));
        }

        if (!MagicDataRules.isSkill(entry)) {
            MagicLevel level = MagicLevel.fromNumericLevel(entry.level());
            if (level != null) {
                line.append("  ");
                line.append(level.isBeginner()
                        ? Component.translatable(level.langKey())
                        : Component.literal(level.enName()));
            }
        }
        return line;
    }
}
