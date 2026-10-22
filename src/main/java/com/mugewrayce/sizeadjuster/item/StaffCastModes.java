package com.mugewrayce.sizeadjuster.item;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 净化法杖的施法模式（服务端按玩家记录，由切换 GUI 选择）。
 * 0=魔法球（默认）、1=单体治愈魔法阵（对自己）、2=群体治愈魔法阵、
 * 3=群体伤害魔法阵、4=防御魔法阵。
 */
public final class StaffCastModes {

    public static final int MODE_ORB = 0;
    public static final int MODE_SINGLE_HEAL = 1;
    public static final int MODE_GROUP_HEAL = 2;
    public static final int MODE_GROUP_DAMAGE = 3;
    public static final int MODE_DEFENSE = 4;

    private static final Map<UUID, Integer> MODES = new HashMap<>();

    private StaffCastModes() {
    }

    public static int get(Player player) {
        return MODES.getOrDefault(player.getUUID(), MODE_ORB);
    }

    public static void set(Player player, int mode) {
        if (mode >= MODE_ORB && mode <= MODE_DEFENSE) {
            MODES.put(player.getUUID(), mode);
        }
    }
}
