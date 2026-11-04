package com.mugewrayce.sizeadjuster.item;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 净化法杖的施法模式（服务端按玩家记录；本任务没有模式选择 GUI，默认点式）。
 *
 * <p>编号按草案顺序冻结（W07），{@code staff_mode} 包与 {@code StaffModePayload} 的
 * 服务端校验范围为 {@code 0..6}：</p>
 * <pre>
 * 0=点式（魔法球，默认）  1=单发线式  2=持续线式
 * 3=单体治愈魔法阵        4=群体治愈魔法阵  5=群体伤害魔法阵  6=普通防御魔法阵
 * </pre>
 */
public final class StaffCastModes {

    public static final int MODE_ORB = 0;
    public static final int MODE_LINE_SINGLE = 1;
    public static final int MODE_LINE_SUSTAINED = 2;
    public static final int MODE_SINGLE_HEAL = 3;
    public static final int MODE_GROUP_HEAL = 4;
    public static final int MODE_GROUP_DAMAGE = 5;
    public static final int MODE_DEFENSE = 6;

    /** 模式数量（0..MODE_COUNT-1 合法）。 */
    public static final int MODE_COUNT = 7;

    private static final Map<UUID, Integer> MODES = new HashMap<>();

    private StaffCastModes() {
    }

    public static int get(Player player) {
        return MODES.getOrDefault(player.getUUID(), MODE_ORB);
    }

    public static boolean isValid(int mode) {
        return mode >= MODE_ORB && mode < MODE_COUNT;
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
