package com.mugewrayce.sizeadjuster.clientbridge;

/**
 * 公共物品类与客户端 GUI 之间的桥接接口（W01 4.2）。
 *
 * <p>公共物品类不得直接引用客户端 {@code Screen}/{@code Minecraft}；需要打开 GUI 时
 * 统一调用 {@link ScreenOpenerHolder}，由客户端初始化时注入真实实现。
 * 专用服务器上没有客户端，持有者保持空实现。
 */
public interface ScreenOpener {

    /** 打开柃之册的功能选择 GUI。 */
    void openBookOfLing();

    /** 打开角色切换 GUI。 */
    void openRoleSwitch();

    /** 打开净化法杖设置 GUI。 */
    void openEquipoiseStaffSettings();

    /** 打开普通魔法杖设置 GUI。 */
    void openMagicStaffSettings();

    /** 打开魔法水晶设置 GUI。 */
    void openCrystalSettings();

    /** 打开魔法书设置 GUI。 */
    void openMagicBook();

    /** 打开魔法书·空间传送目标坐标界面（W14b 右键入口）。 */
    void openMagicBookTeleport();

    /** 打开魔法书·时间暂停排除目标界面（W14b 右键入口）。 */
    void openMagicBookExclusion();

    /** 打开魔法学习证查看 GUI。 */
    void openStudyCert();

    /** 打开魔力值探测器 GUI。 */
    void openManaDetector();

    /** 打开生命迁移石 GUI。 */
    void openLifeMigration();
}
