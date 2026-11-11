package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.magic.MagicSkill;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 净化法杖的施法模式（服务端按玩家记录）。
 *
 * <p>编号前 7 个按草案顺序冻结（W07）；W19 追加 8 个自然魔法技能（7~14），
 * {@code staff_mode} 包与 {@code StaffModePayload} 的服务端校验范围随之变为 {@code 0..14}：</p>
 * <pre>
 * 0=点式（魔法球，默认）  1=单发线式  2=持续线式
 * 3=单体治愈魔法阵        4=群体治愈魔法阵  5=群体伤害魔法阵  6=普通防御魔法阵
 * 7=点火  8=融化  9=冻结  10=制冰  11=召雷  12=生水  13=前方闪光  14=指向闪光
 * </pre>
 * <p>7~14 是“技能”：按下右键立即结算，不进入蓄力 / 长按流程（与普通魔法杖的技能一致）。</p>
 */
public final class StaffCastModes {

    public static final int MODE_ORB = 0;
    public static final int MODE_LINE_SINGLE = 1;
    public static final int MODE_LINE_SUSTAINED = 2;
    public static final int MODE_SINGLE_HEAL = 3;
    public static final int MODE_GROUP_HEAL = 4;
    public static final int MODE_GROUP_DAMAGE = 5;
    public static final int MODE_DEFENSE = 6;

    /** W19：8 个自然魔法技能模式（编号冻结，顺序与 {@link MagicSkill} 的自然魔法段一致）。 */
    public static final int MODE_IGNITE = 7;
    public static final int MODE_MELT = 8;
    public static final int MODE_FREEZE = 9;
    public static final int MODE_MAKE_ICE = 10;
    public static final int MODE_SUMMON_LIGHTNING = 11;
    public static final int MODE_CREATE_WATER = 12;
    public static final int MODE_FRONT_FLASH = 13;
    public static final int MODE_TARGETED_FLASH = 14;

    /** 第一个技能模式（7）。 */
    public static final int FIRST_SKILL_MODE = MODE_IGNITE;

    /** 模式数量（0..MODE_COUNT-1 合法）。 */
    public static final int MODE_COUNT = 15;

    private static final Map<UUID, Integer> MODES = new HashMap<>();

    private StaffCastModes() {
    }

    public static int get(Player player) {
        return MODES.getOrDefault(player.getUUID(), MODE_ORB);
    }

    public static boolean isValid(int mode) {
        return mode >= MODE_ORB && mode < MODE_COUNT;
    }

    /** W19：7~14 为自然魔法技能（按下即结算，不走蓄力 / 长按）。 */
    public static boolean isSkill(int mode) {
        return mode >= FIRST_SKILL_MODE && mode < MODE_COUNT;
    }

    /** W19：技能模式 → {@link MagicSkill}；非技能模式返回 {@code null}。 */
    public static MagicSkill skillFor(int mode) {
        return switch (mode) {
            case MODE_IGNITE -> MagicSkill.IGNITE;
            case MODE_MELT -> MagicSkill.MELT_ICE;
            case MODE_FREEZE -> MagicSkill.FREEZE;
            case MODE_MAKE_ICE -> MagicSkill.MAKE_ICE;
            case MODE_SUMMON_LIGHTNING -> MagicSkill.SUMMON_LIGHTNING;
            case MODE_CREATE_WATER -> MagicSkill.CREATE_WATER;
            case MODE_FRONT_FLASH -> MagicSkill.FRONT_FLASH;
            case MODE_TARGETED_FLASH -> MagicSkill.TARGETED_FLASH;
            default -> null;
        };
    }

    public static void set(Player player, int mode) {
        if (isValid(mode)) {
            MODES.put(player.getUUID(), mode);
        }
    }

    /** 玩家离开时移除其施法模式记录（W01 4.4）。 */
    public static void clear(UUID uuid) {
        MODES.remove(uuid);
    }
}
