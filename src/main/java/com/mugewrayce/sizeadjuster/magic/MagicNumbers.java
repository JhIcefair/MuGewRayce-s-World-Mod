package com.mugewrayce.sizeadjuster.magic;

/**
 * W14 的伤害 / 治疗 / 等级加成 / 附加书 / 耗魔纯函数表（草案 §8.2~§8.5、§7.2）。
 *
 * <p>全部为纯函数，便于 {@code W14NumbersSelfCheck} 在不启动游戏的情况下逐项断言。
 * 等级 0=初学者、1~5=I~V；越界按最近端点处理。</p>
 *
 * <p>加成口径（草案 §8.3 / §8.4）：<b>直接替换、不叠加</b>；
 * 治疗 IV/V 是“III 级效果 + 5/10 秒生命恢复 II/V”，不是继续加点数。</p>
 */
public final class MagicNumbers {

    // ---- 基础伤害（§8.2） ----
    public static final float POINT_DAMAGE = 10.0F;
    public static final float LINE_SINGLE_DAMAGE = 15.0F;
    public static final float LINE_SUSTAINED_DAMAGE = 1.0F;
    public static final float FORMATION_ATTACK_DAMAGE = 5.0F;

    /** 单发线式的点数（有限高频点式，合计 15）。 */
    public static final int LINE_SINGLE_ORBS = 3;

    // ---- 基础治疗（§8.4） ----
    public static final float POINT_HEAL = 10.0F;
    public static final float LINE_SINGLE_HEAL = 18.0F;
    public static final float LINE_SUSTAINED_HEAL = 2.0F;
    public static final float FORMATION_HEAL = 1.0F;

    // ---- 耗魔（§7.2） ----
    public static final int MANA_POINT = 10;
    public static final int MANA_LINE_SINGLE = 12;
    public static final int MANA_LINE_SUSTAINED_START = 8;
    public static final int MANA_LINE_SUSTAINED_PER_SECOND = 10;
    public static final int MANA_FORMATION_SELF_HEAL = 200;
    public static final int MANA_FORMATION_GROUP_HEAL = 500;
    public static final int MANA_FORMATION_GROUP_ATTACK = 700;
    public static final int MANA_FORMATION_DEFENSE_START = 80;
    public static final int MANA_FORMATION_DEFENSE_PER_SECOND = 50;

    // ---- 技能耗魔（§6.4） ----
    public static final int MANA_IGNITE_PER_BLOCK = 3;
    public static final int MANA_MELT_PER_BLOCK = 4;
    public static final int MANA_FREEZE_PER_BLOCK = 3;
    public static final int MANA_MAKE_ICE_PER_BLOCK = 4;
    public static final int MANA_SUMMON_LIGHTNING = 6;
    public static final int MANA_CREATE_WATER_PER_BLOCK = 2;
    public static final int MANA_FLASH = 10;
    public static final int MANA_TIME_PAUSE_PER_CHUNK = 5;
    public static final int MANA_TIME_PAUSE_PER_SECOND = 1;
    public static final int MANA_TELEPORT_PER_CHUNK = 7;

    /** 闪光光源维持时长：30 秒。 */
    public static final int FLASH_LIGHT_TICKS = 20 * 30;

    /** 生命恢复效果时长（tick）/ 等级（amplifier）。 */
    public static final int REGEN_TICKS_IV = 20 * 5;
    public static final int REGEN_TICKS_V = 20 * 10;
    public static final int REGEN_AMPLIFIER_II = 1;
    public static final int REGEN_AMPLIFIER_V = 4;

    private MagicNumbers() {
    }

    /** 等级夹到 0..5。 */
    public static int clampLevel(int level) {
        return Math.max(0, Math.min(5, level));
    }

    /** 伤害倍率（直接替换）：0 / +10% / +25% / +50% / +100% / +200%。 */
    public static float damageMultiplier(int level) {
        return switch (clampLevel(level)) {
            case 1 -> 1.10F;
            case 2 -> 1.25F;
            case 3 -> 1.50F;
            case 4 -> 2.00F;
            case 5 -> 3.00F;
            default -> 1.00F;
        };
    }

    public static float pointDamage(int level) {
        return POINT_DAMAGE * damageMultiplier(level);
    }

    public static float lineSingleDamage(int level) {
        return LINE_SINGLE_DAMAGE * damageMultiplier(level);
    }

    /** 单发线式每一点的伤害（总伤害 / 点数）。 */
    public static float lineSingleOrbDamage(int level) {
        return lineSingleDamage(level) / LINE_SINGLE_ORBS;
    }

    public static float sustainedDamage(int level) {
        return LINE_SUSTAINED_DAMAGE * damageMultiplier(level);
    }

    public static float formationDamage(int level) {
        return FORMATION_ATTACK_DAMAGE * damageMultiplier(level);
    }

    /** 点式 / 单发线式的加点：I +2、II +5、III +10；IV/V 在 III 基础上再挂生命恢复。 */
    public static float healBonusFlat(int level) {
        return switch (clampLevel(level)) {
            case 1 -> 2.0F;
            case 2 -> 5.0F;
            case 3, 4, 5 -> 10.0F;
            default -> 0.0F;
        };
    }

    /** 持续线式 / 魔法阵的每刻加点：I +1、II +2、III +4；IV/V 在 III 基础上再挂生命恢复。 */
    public static float healBonusPerTick(int level) {
        return switch (clampLevel(level)) {
            case 1 -> 1.0F;
            case 2 -> 2.0F;
            case 3, 4, 5 -> 4.0F;
            default -> 0.0F;
        };
    }

    public static float pointHeal(int level) {
        return POINT_HEAL + healBonusFlat(level);
    }

    public static float lineSingleHeal(int level) {
        return LINE_SINGLE_HEAL + healBonusFlat(level);
    }

    public static float sustainedHeal(int level) {
        return LINE_SUSTAINED_HEAL + healBonusPerTick(level);
    }

    public static float formationHeal(int level) {
        return FORMATION_HEAL + healBonusPerTick(level);
    }

    /** IV/V 的额外生命恢复时长（tick）；III 及以下为 0。 */
    public static int regenTicks(int level) {
        return switch (clampLevel(level)) {
            case 4 -> REGEN_TICKS_IV;
            case 5 -> REGEN_TICKS_V;
            default -> 0;
        };
    }

    /** IV=生命恢复 II（amplifier 1）、V=生命恢复 V（amplifier 4）。 */
    public static int regenAmplifier(int level) {
        return switch (clampLevel(level)) {
            case 4 -> REGEN_AMPLIFIER_II;
            case 5 -> REGEN_AMPLIFIER_V;
            default -> 0;
        };
    }

    /** 附加书倍率：I/II/III = 1/2/3；0 或越界按 0 处理（不附加）。 */
    public static int attachmentMultiplier(int level) {
        return level >= 1 && level <= 3 ? level : 0;
    }

    /** 附加书追加的魔法伤害 = 实际物理伤害 × 倍率；无附加返回 0。 */
    public static float attachmentMagicDamage(float physicalDamage, int level) {
        int multiplier = attachmentMultiplier(level);
        if (multiplier == 0 || physicalDamage <= 0.0F) {
            return 0.0F;
        }
        return physicalDamage * multiplier;
    }

    /** 时间暂停启动耗魔：5 / 区块。 */
    public static int timePauseStartCost(int chunks) {
        return MANA_TIME_PAUSE_PER_CHUNK * Math.max(0, chunks);
    }

    /** 空间传送启动耗魔：7 / 区块 + 距离费。 */
    public static int teleportCost(int chunks, double distance) {
        return MANA_TELEPORT_PER_CHUNK * Math.max(0, chunks) + teleportDistanceFee(distance);
    }

    /**
     * 传送距离费：每 100 格 3；不满 100 的余数 >50 算 2、≤50 算 1。
     * 0→0、50→1、100→3、101→4、150→4、151→5、200→6。
     */
    public static int teleportDistanceFee(double distance) {
        if (distance <= 0.0D) {
            return 0;
        }
        int fullHundreds = (int) (distance / 100.0D);
        double remainder = distance - fullHundreds * 100.0D;
        int fee = fullHundreds * 3;
        if (remainder > 0.0D) {
            fee += remainder > 50.0D ? 2 : 1;
        }
        return fee;
    }

    /** 单个区块的边长（用于区块数 = n×n）。 */
    public static int chunkCount(int range) {
        int side = Math.max(0, range);
        return side * side;
    }
}
