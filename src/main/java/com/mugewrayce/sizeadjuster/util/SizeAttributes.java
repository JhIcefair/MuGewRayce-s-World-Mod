package com.mugewrayce.sizeadjuster.util;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;

public class SizeAttributes {

    private static final double DEFAULT_HEIGHT_CM = 185.0;
    private static final int MIN_HEIGHT = 10;
    private static final int MAX_HEIGHT = 1000;

    public static boolean isHeightAllowed(int heightCm) {
        return heightCm >= MIN_HEIGHT && heightCm <= MAX_HEIGHT;
    }

    public static void applyHeight(ServerPlayer player, int heightCm) {
        double scale = heightCm / DEFAULT_HEIGHT_CM;

        AttributeInstance scaleAttr = player.getAttribute(Attributes.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(scale);

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(0.1 * scale);

        AttributeInstance jumpAttr = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpAttr != null) jumpAttr.setBaseValue(0.42 * scale);

        double health = scale < 1 ? 20 * scale : 20 + 20 * (scale - 1) * 0.8;
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(health);
            player.setHealth((float) health);
        }

        AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) attackAttr.setBaseValue(scale);

        AttributeInstance knockbackAttr = player.getAttribute(Attributes.ATTACK_KNOCKBACK);
        if (knockbackAttr != null) knockbackAttr.setBaseValue(scale > 1 ? scale - 1 : 0);

        double blockRange = 4.5 + 5 * (scale - 1);
        if (blockRange < 1.0) blockRange = 1.0;
        AttributeInstance blockRangeAttr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockRangeAttr != null) blockRangeAttr.setBaseValue(blockRange);

        double entityRange = 3.0 + 4 * (scale - 1);
        if (entityRange < 0.5) entityRange = 0.5;
        AttributeInstance entityRangeAttr = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityRangeAttr != null) entityRangeAttr.setBaseValue(entityRange);

        // 安全摔落距离 (safe_fall_distance)
        double safeFall = 3.0 * scale;
        AttributeInstance safeFallAttr = player.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        if (safeFallAttr != null) safeFallAttr.setBaseValue(safeFall);

        // 最大行走高度 (step_height)
        double stepHeight = 0.6 * scale;
        AttributeInstance stepHeightAttr = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeightAttr != null) stepHeightAttr.setBaseValue(stepHeight);
    }

    public static void resetToDefault(ServerPlayer player) {
        applyHeight(player, (int) DEFAULT_HEIGHT_CM);
    }

    public static void applySpecial(ServerPlayer player) {
        // 先重置为默认
        resetToDefault(player);

        double scale = 0.65;
        AttributeInstance scaleAttr = player.getAttribute(Attributes.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(scale);

        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(12);
            player.setHealth(12);
        }

        AttributeInstance blockRangeAttr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockRangeAttr != null) blockRangeAttr.setBaseValue(24);

        AttributeInstance entityRangeAttr = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityRangeAttr != null) entityRangeAttr.setBaseValue(20);
    }

    /**
     * 获取玩家当前尺寸状态（多行文本组件）。
     * 服务端只发翻译键与数值，客户端按自身语言渲染，避免服务端固定中文。
     */
    public static List<Component> getStatusLines(Player player) {
        double scale = getAttributeValue(player, Attributes.SCALE, 1.0);
        double heightCm = scale * DEFAULT_HEIGHT_CM;
        double speed = getAttributeValue(player, Attributes.MOVEMENT_SPEED, 0.1);
        double jump = getAttributeValue(player, Attributes.JUMP_STRENGTH, 0.42);
        double maxHealth = getAttributeValue(player, Attributes.MAX_HEALTH, 20.0);
        double attackDamage = getAttributeValue(player, Attributes.ATTACK_DAMAGE, 1.0);
        double attackKnockback = getAttributeValue(player, Attributes.ATTACK_KNOCKBACK, 0.0);
        double blockRange = getAttributeValue(player, Attributes.BLOCK_INTERACTION_RANGE, 4.5);
        double entityRange = getAttributeValue(player, Attributes.ENTITY_INTERACTION_RANGE, 3.0);
        double safeFall = getAttributeValue(player, Attributes.SAFE_FALL_DISTANCE, 3.0);
        double stepHeight = getAttributeValue(player, Attributes.STEP_HEIGHT, 0.6);

        return List.of(
                Component.translatable("gui.mugewrayce-mod.size.status.title"),
                Component.translatable("gui.mugewrayce-mod.size.status.height",
                        fmt("%.1f", heightCm), fmt("%.3f", scale)),
                Component.translatable("gui.mugewrayce-mod.size.status.movement_speed", fmt("%.3f", speed)),
                Component.translatable("gui.mugewrayce-mod.size.status.jump_strength", fmt("%.3f", jump)),
                Component.translatable("gui.mugewrayce-mod.size.status.health",
                        fmt("%.1f", player.getHealth()), fmt("%.1f", maxHealth)),
                Component.translatable("gui.mugewrayce-mod.size.status.attack_damage", fmt("%.2f", attackDamage)),
                Component.translatable("gui.mugewrayce-mod.size.status.attack_knockback", fmt("%.2f", attackKnockback)),
                Component.translatable("gui.mugewrayce-mod.size.status.block_range", fmt("%.2f", blockRange)),
                Component.translatable("gui.mugewrayce-mod.size.status.entity_range", fmt("%.2f", entityRange)),
                Component.translatable("gui.mugewrayce-mod.size.status.safe_fall", fmt("%.2f", safeFall)),
                Component.translatable("gui.mugewrayce-mod.size.status.step_height", fmt("%.2f", stepHeight))
        );
    }

    /** 固定用 Locale.ROOT 格式化，避免不同系统区域设置把小数点变成逗号。 */
    private static String fmt(String format, double value) {
        return String.format(Locale.ROOT, format, value);
    }

    private static double getAttributeValue(Player player, Holder<Attribute> attributeHolder, double defaultValue) {
        AttributeInstance instance = player.getAttribute(attributeHolder);
        return instance != null ? instance.getValue() : defaultValue;
    }
}