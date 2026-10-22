package com.mugewrayce.sizeadjuster.util;

import com.mugewrayce.sizeadjuster.entity.IcefairShields;
import com.mugewrayce.sizeadjuster.entity.PurificationFormationEntity;
import com.mugewrayce.sizeadjuster.entity.PurificationOrbEntity;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 玩家特殊角色系统（可用指令 /mugewrayce entity、柃之册"角色切换"GUI 或
 * "生命转换令牌"切换）。
 *
 * 角色：
 * - 默认（ROLE_DEFAULT=0）：完全没有任何变化；
 * - 魔法体（ROLE_MAGIC_BODY=1）：物理免疫（免疫一切物理伤害；弓箭/烈焰弹/雪球等
 *   远程投射物直接穿过），只能受到魔法伤害；
 * - 特殊角色（ROLE_SPECIAL_BASE=2 起，SPECIAL_IDS 每项一个）：
 *   · icefair：属于魔法体（拥有其全部特性），另外自带自动防御魔法
 *     （见 IcefairShields），且尺寸属性套用"icefair 特殊尺寸组"。
 *
 * 说明：角色持久化到世界存档目录的 mugewrayce_roles.json（RoleStore），
 * 退出游戏重进/服务器重启后角色仍保留，不会恢复默认；
 * 尺寸类属性值本身随玩家属性存档保留。
 */
public final class PlayerRoles {

    public static final int ROLE_DEFAULT = 0;
    public static final int ROLE_MAGIC_BODY = 1;

    /** 特殊角色起始编号：ROLE_SPECIAL_BASE + i 对应 SPECIAL_IDS 第 i 个。 */
    public static final int ROLE_SPECIAL_BASE = 2;

    /** 特殊角色清单（按此顺序编号；将来新增特殊角色直接在此追加）。 */
    public static final List<String> SPECIAL_IDS = List.of("icefair");

    private PlayerRoles() {
    }

    /** 读取玩家当前角色（取自持久化存储；未设置时默认）。 */
    public static int getRole(Player player) {
        return RoleStore.getRole(player.getUUID());
    }

    /** 是否魔法体（魔法体本身及所有特殊角色——特殊角色均属于魔法体）。 */
    public static boolean isMagicBody(int role) {
        return role == ROLE_MAGIC_BODY || role >= ROLE_SPECIAL_BASE;
    }

    /** 是否 icefair 特殊角色。 */
    public static boolean isIcefair(int role) {
        return role == specialCode(0);
    }

    public static boolean isValid(int role) {
        if (role == ROLE_DEFAULT || role == ROLE_MAGIC_BODY) {
            return true;
        }
        return role >= ROLE_SPECIAL_BASE && role < ROLE_SPECIAL_BASE + SPECIAL_IDS.size();
    }

    /** 特殊角色编号：SPECIAL_IDS 第 index 个。 */
    public static int specialCode(int index) {
        return ROLE_SPECIAL_BASE + index;
    }

    public static String specialId(int role) {
        return SPECIAL_IDS.get(role - ROLE_SPECIAL_BASE);
    }

    /** 角色中文显示名：默认 / 魔法体 / 特殊-icefair… */
    public static String roleLabel(int role) {
        return switch (role) {
            case ROLE_DEFAULT -> "默认";
            case ROLE_MAGIC_BODY -> "魔法体";
            default -> isValid(role) ? "特殊-" + specialId(role) : "未知";
        };
    }

    /** 角色显示名（文本组件，客户端按自身语言渲染）。 */
    public static Component roleComponent(int role) {
        return switch (role) {
            case ROLE_DEFAULT -> Component.translatable("gui.mugewrayce-mod.role.default");
            case ROLE_MAGIC_BODY -> Component.translatable("gui.mugewrayce-mod.role.magic_body");
            default -> isValid(role)
                    ? Component.translatable("gui.mugewrayce-mod.role.special", specialId(role))
                    : Component.translatable("gui.mugewrayce-mod.role.unknown");
        };
    }

    /** 角色选择 GUI / 展示用完整选项：默认、魔法体、特殊-xxx… */
    public static List<String> allLabels() {
        List<String> labels = new ArrayList<>();
        labels.add(roleLabel(ROLE_DEFAULT));
        labels.add(roleLabel(ROLE_MAGIC_BODY));
        for (int i = 0; i < SPECIAL_IDS.size(); i++) {
            labels.add(roleLabel(specialCode(i)));
        }
        return labels;
    }

    /** GUI 用完整角色编号列表（顺序同 allLabels：默认、魔法体、特殊角色…）。 */
    public static List<Integer> allRoleCodes() {
        List<Integer> roles = new ArrayList<>();
        roles.add(ROLE_DEFAULT);
        roles.add(ROLE_MAGIC_BODY);
        for (int i = 0; i < SPECIAL_IDS.size(); i++) {
            roles.add(specialCode(i));
        }
        return roles;
    }

    /** GUI 选项下标 → 角色编号（0 默认、1 魔法体、2 起为特殊角色）。 */
    public static int optionToRole(int optionIndex) {
        if (optionIndex <= 0) {
            return ROLE_DEFAULT;
        }
        if (optionIndex == 1) {
            return ROLE_MAGIC_BODY;
        }
        return specialCode(optionIndex - 2);
    }

    /**
     * 应用角色切换（服务端）。
     * - 切到 icefair：套用 icefair 特殊尺寸组（同时获得魔法体特性与自动防御）；
     * - 切到其他角色：若原先是 icefair，撤销其特殊尺寸并清理其自动防御魔法阵；
     * - 角色持久化到存档（RoleStore），退出游戏重进仍保留。
     */
    public static void applyRole(ServerPlayer player, int role) {
        if (!isValid(role)) {
            return;
        }
        UUID uuid = player.getUUID();
        int previous = getRole(player);
        if (previous == role) {
            return;
        }
        if (isIcefair(role)) {
            // icefair：玩家尺寸等属性值为 icefair 特殊尺寸组
            SizeAttributes.applySpecial(player);
        } else {
            if (isIcefair(previous)) {
                // 离开 icefair：撤销其特殊尺寸（恢复默认尺寸组）
                SizeAttributes.resetToDefault(player);
            }
            if (previous >= ROLE_SPECIAL_BASE) {
                IcefairShields.clearShields(uuid);
            }
        }
        RoleStore.setRole(uuid, role);
    }

    /**
     * 是否"强制处决/绝对死亡"伤害（/kill 与虚空），即使魔法体物理免疫也应生效。
     * LivingEntity.kill() 会以 genericKill() 调用 hurtServer（见 /kill 实现），
     * 虚空为 fellOutOfWorld()；二者不应被物理免疫拦截。
     */
    public static boolean isForcedKill(DamageSource source) {
        Holder<DamageType> type = source.typeHolder();
        return type.is(DamageTypes.GENERIC_KILL) || type.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    /**
     * 是否"豁免伤害"——排除在魔法体/特殊角色免疫范围之外的几种伤害类型，
     * 即使魔法体物理免疫，这类伤害仍会生效（魔法体/icefair 会被其伤害）：
     * 火焰（in_fire / on_fire / lava / hot_floor / campfire）、
     * 冰冻（freeze）、溺水（drown）、凋零（wither）。
     */
    public static boolean isExemptDamage(DamageSource source) {
        Holder<DamageType> type = source.typeHolder();
        return type.is(DamageTypes.IN_FIRE)
                || type.is(DamageTypes.ON_FIRE)
                || type.is(DamageTypes.LAVA)
                || type.is(DamageTypes.HOT_FLOOR)
                || type.is(DamageTypes.CAMPFIRE)
                || type.is(DamageTypes.DROWN)
                || type.is(DamageTypes.FREEZE)
                || type.is(DamageTypes.WITHER);
    }

    /**
     * 是否算作"魔法伤害"（魔法体只能受到此类伤害）：
     * - 净化法杖 / 魔法阵 / 归零 造成的伤害（indirectMagic，来源实体为本模组
     *   魔法球或魔法阵实体）——目前先如此标记，后续为魔法体系定义专门体系；
     * - 原版魔法类伤害（magic / indirect_magic：如伤害药水、女巫、守卫者激光等）。
     * 其余一切伤害（近战、弓箭等远程、火焰、摔落、环境等）都视为物理伤害。
     */
    public static boolean isMagicDamage(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof PurificationOrbEntity || direct instanceof PurificationFormationEntity) {
            return true;
        }
        Holder<DamageType> type = source.typeHolder();
        return type.is(DamageTypes.MAGIC) || type.is(DamageTypes.INDIRECT_MAGIC);
    }
}
