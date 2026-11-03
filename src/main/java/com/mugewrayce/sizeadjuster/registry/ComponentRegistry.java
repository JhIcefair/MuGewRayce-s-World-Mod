package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * 数据组件类型注册落点（W03）。
 *
 * <p>组件是 <b>common</b> 内容，只在 common initializer 注册，不能放客户端初始化；
 * 且必须排在物品之前（W07/W09 可能在 {@code Item.Properties.component(...)} 里直接引用组件），
 * 顺序由 {@link ModRegistry} 保证。</p>
 *
 * <p>组件 ID 是存档 / 网络标识，注册后不要改名。</p>
 */
public final class ComponentRegistry {

    /** W03 临时测试组件 ID（接口约定 §2.1，W23 清理）。 */
    public static final Identifier W03_TEST_DATA_ID =
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "w03_test_data");

    private ComponentRegistry() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                W03_TEST_DATA_ID, ModComponents.W03_TEST_DATA);
    }
}
