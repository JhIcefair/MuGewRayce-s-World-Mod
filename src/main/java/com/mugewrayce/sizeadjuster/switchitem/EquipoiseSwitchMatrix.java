package com.mugewrayce.sizeadjuster.switchitem;

/**
 * 净化系方向键切换矩阵（纯函数，不依赖 Minecraft 类型，便于静态自检）。
 *
 * <p>五件物品用 0..4 表示，四个方向用 0..3 表示（与 {@code EquipoiseSwitchPayload.direction} 一致）：</p>
 * <pre>
 * 方向：0=↑、1=↓、2=←、3=→
 * 物品：0=净化水晶、1=净化法杖、2=净化圣剑、3=净化圣弓、4=归零
 * </pre>
 *
 * <p>规则：</p>
 * <ul>
 *   <li>手持净化水晶时，四个方向分别直达法杖 / 圣剑 / 圣弓 / 归零；</li>
 *   <li>手持其余四件时，按下<strong>该物品自己的方向</strong>回净化水晶，按其他方向直达对应目标；</li>
 *   <li>非五件、方向越界、目标等于当前物品 → 返回 {@link #NONE}（服务端拒绝，无副作用）。</li>
 * </ul>
 */
public final class EquipoiseSwitchMatrix {

    public static final int NONE = -1;

    public static final int DIR_UP = 0;
    public static final int DIR_DOWN = 1;
    public static final int DIR_LEFT = 2;
    public static final int DIR_RIGHT = 3;

    public static final int ITEM_CRYSTAL = 0;
    public static final int ITEM_STAFF = 1;
    public static final int ITEM_SWORD = 2;
    public static final int ITEM_BOW = 3;
    public static final int ITEM_ZERO_RESET = 4;

    /** 物品数量 / 方向数量（用于范围校验）。 */
    public static final int ITEM_COUNT = 5;
    public static final int DIRECTION_COUNT = 4;

    private EquipoiseSwitchMatrix() {
    }

    /** 该物品“自己的方向”（按它回到净化水晶）；净化水晶没有自己的方向，返回 {@link #NONE}。 */
    public static int ownDirection(int itemCode) {
        return switch (itemCode) {
            case ITEM_STAFF -> DIR_UP;
            case ITEM_SWORD -> DIR_DOWN;
            case ITEM_BOW -> DIR_LEFT;
            case ITEM_ZERO_RESET -> DIR_RIGHT;
            default -> NONE;
        };
    }

    /** 方向对应的直接目标（不含“回水晶”规则）。 */
    public static int directionTarget(int direction) {
        return switch (direction) {
            case DIR_UP -> ITEM_STAFF;
            case DIR_DOWN -> ITEM_SWORD;
            case DIR_LEFT -> ITEM_BOW;
            case DIR_RIGHT -> ITEM_ZERO_RESET;
            default -> NONE;
        };
    }

    /**
     * 计算切换目标。
     *
     * @param heldItemCode 当前主手物品码（0..4）
     * @param direction    方向码（0..3）
     * @return 目标物品码；非法输入或目标等于当前物品时返回 {@link #NONE}
     */
    public static int target(int heldItemCode, int direction) {
        if (heldItemCode < 0 || heldItemCode >= ITEM_COUNT) {
            return NONE;
        }
        if (direction < 0 || direction >= DIRECTION_COUNT) {
            return NONE;
        }
        int target = direction == ownDirection(heldItemCode)
                ? ITEM_CRYSTAL
                : directionTarget(direction);
        return target == heldItemCode ? NONE : target;
    }

    public static boolean isValidItem(int itemCode) {
        return itemCode >= 0 && itemCode < ITEM_COUNT;
    }

    public static boolean isValidDirection(int direction) {
        return direction >= 0 && direction < DIRECTION_COUNT;
    }
}
