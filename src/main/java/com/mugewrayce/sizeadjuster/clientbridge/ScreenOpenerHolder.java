package com.mugewrayce.sizeadjuster.clientbridge;

/**
 * {@link ScreenOpener} 的静态持有者（W01 4.2）。
 *
 * <p>默认实现为空操作，保证专用服务器/未初始化客户端时不抛异常；
 * 客户端在 {@code MuGewRayceModClient.onInitializeClient()} 中注入真实实现。
 */
public final class ScreenOpenerHolder {

    private static final ScreenOpener NOOP = new ScreenOpener() {
        @Override
        public void openBookOfLing() {
        }

        @Override
        public void openRoleSwitch() {
        }

        @Override
        public void openEquipoiseStaffSettings() {
        }

        @Override
        public void openMagicStaffSettings() {
        }

        @Override
        public void openCrystalSettings() {
        }

        @Override
        public void openMagicBook() {
        }

        @Override
        public void openStudyCert() {
        }

        @Override
        public void openManaDetector() {
        }

        @Override
        public void openLifeMigration() {
        }
    };

    private static volatile ScreenOpener opener = NOOP;

    private ScreenOpenerHolder() {
    }

    /** 注入客户端实现；传 null 恢复空实现。 */
    public static void set(ScreenOpener screenOpener) {
        opener = screenOpener != null ? screenOpener : NOOP;
    }

    public static void openBookOfLing() {
        opener.openBookOfLing();
    }

    public static void openRoleSwitch() {
        opener.openRoleSwitch();
    }

    public static void openEquipoiseStaffSettings() {
        opener.openEquipoiseStaffSettings();
    }

    public static void openMagicStaffSettings() {
        opener.openMagicStaffSettings();
    }

    public static void openCrystalSettings() {
        opener.openCrystalSettings();
    }

    public static void openMagicBook() {
        opener.openMagicBook();
    }

    public static void openStudyCert() {
        opener.openStudyCert();
    }

    public static void openManaDetector() {
        opener.openManaDetector();
    }

    public static void openLifeMigration() {
        opener.openLifeMigration();
    }
}
