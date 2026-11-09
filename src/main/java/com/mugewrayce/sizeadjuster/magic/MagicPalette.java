package com.mugewrayce.sizeadjuster.magic;

import com.mugewrayce.sizeadjuster.entity.EquipoiseFormationEntity;

/**
 * W14 粒子 / 魔法阵的颜色表（灰度贴图靠代码染色，不按元素出图）。
 *
 * <p>全部用 ARGB；渲染阵体时再乘 0.5 得到“自发光 50%”。</p>
 */
public final class MagicPalette {

    public static final int NATURAL = 0xFFE8E8E8;
    public static final int FIRE = 0xFFFF6A00;
    public static final int ICE = 0xFF7FE7FF;
    public static final int LIGHTNING = 0xFFFFF36B;
    public static final int WATER = 0xFF3A7BFF;
    public static final int FLASH = 0xFFFFF7B0;
    public static final int HEAL = 0xFF6BFF8E;
    public static final int ORIGIN = 0xFFB26BFF;
    public static final int EQUIPOISE = 0xFFFFFFFF;

    public static final int FORMATION_ATTACK = 0xFFFF5A5A;
    public static final int FORMATION_DEFENSE = 0xFF5AA8FF;
    public static final int FORMATION_HEAL = 0xFF6BFF8E;

    private MagicPalette() {
    }

    /** 按魔法类型取粒子颜色。 */
    public static int colorFor(MagicType type) {
        if (type == null) {
            return NATURAL;
        }
        return switch (type) {
            case FIRE -> FIRE;
            case ICE -> ICE;
            case LIGHTNING -> LIGHTNING;
            case WATER -> WATER;
            case FLASH -> FLASH;
            case HEAL -> HEAL;
            case ORIGIN -> ORIGIN;
            case PURIFICATION -> EQUIPOISE;
            default -> NATURAL;
        };
    }

    /** 按魔法阵类型取阵体颜色。 */
    public static int colorForFormation(int formationType) {
        if (formationType == EquipoiseFormationEntity.TYPE_MAGIC_GROUP_ATTACK) {
            return FORMATION_ATTACK;
        }
        if (formationType == EquipoiseFormationEntity.TYPE_MAGIC_DEFENSE) {
            return FORMATION_DEFENSE;
        }
        if (formationType == EquipoiseFormationEntity.TYPE_MAGIC_SELF_HEAL
                || formationType == EquipoiseFormationEntity.TYPE_MAGIC_GROUP_HEAL) {
            return FORMATION_HEAL;
        }
        return EQUIPOISE;
    }

    /** RGB 各乘 factor，alpha 不变。 */
    public static int dim(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = (int) (((argb >> 16) & 0xFF) * factor);
        int g = (int) (((argb >> 8) & 0xFF) * factor);
        int b = (int) ((argb & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** 只取 RGB（粒子 options 用 0xRRGGBB 或 ARGB 均可，这里统一 ARGB）。 */
    public static int rgb(int argb) {
        return argb & 0xFFFFFF;
    }
}
