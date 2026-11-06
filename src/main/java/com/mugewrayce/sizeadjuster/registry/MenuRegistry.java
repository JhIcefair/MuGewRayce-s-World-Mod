package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.container.BaishanBagMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;

/**
 * 统一菜单注册入口（W09）。
 *
 * <p>注册时机：由 {@link ModRegistry} 在物品/实体之后、创造分类之前调用，
 * 因为菜单可能引用已经注册的方块与物品。</p>
 *
 * <p>W09 只登记百杉包；聚合台 / 附加台 / 讲台三个方块菜单按任务边界留到 W10，
 * 不要在本任务提前注册。</p>
 */
public final class MenuRegistry {

    private MenuRegistry() {
    }

    /** 触发菜单注册；W10 在这里追加三个方块菜单。 */
    public static void register() {
        // 百杉包菜单的 MenuType 在 BaishanBagMenu 的静态字段中注册；
        // 这里显式引用一次，确保 ModRegistry 的注册顺序真的把它带起来。
        Objects.requireNonNull(BaishanBagMenu.TYPE, "baishan_bag MenuType");
    }

    /**
     * 注册一个菜单类型（给 W10 的三个方块菜单复用）。
     *
     * <p>26.1 的 {@code MenuType} 构造器依赖 fabric-menu-api-v1 的 classtweaker 公开，
     * 与现有百杉包写法一致。</p>
     */
    public static <T extends AbstractContainerMenu> MenuType<T> registerMenu(
            String id, MenuType.MenuSupplier<T> factory) {
        return Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id),
                new MenuType<>(factory, FeatureFlags.VANILLA_SET));
    }
}
