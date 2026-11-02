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
}
