package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * 数据组件类型注册落点（W03 骨架，W06 加正式组件）。
 *
 * <p>组件是 <b>common</b> 内容，只在 common initializer 注册，不能放客户端初始化；
 * 且必须排在物品之前（W07/W09 会在 {@code Item.Properties.component(...)} 里直接引用组件），
 * 顺序由 {@link ModRegistry} 保证。</p>
 *
 * <p>组件 ID 是存档 / 网络标识，注册后不要改名；正式组件 ID 见接口约定 §2。</p>
 */
public final class ComponentRegistry {

    /** 魔法学习证（接口约定 §2）。 */
    public static final Identifier CERT_DATA_ID =
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "cert_data");
    /** 魔法熟练证明。 */
    public static final Identifier PROFICIENCY_DATA_ID =
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "proficiency_data");
    /** 魔法试炼晋级认证。 */
    public static final Identifier PROMOTION_DATA_ID =
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "promotion_data");
    /** 考试 / 试炼申请书（两份共用）。 */
    public static final Identifier APPLICATION_DATA_ID =
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "application_data");
    /** 普通魔法杖。 */
    public static final Identifier STAFF_DATA_ID =
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "staff_data");
    /** 被附加书附加过的武器。 */
    public static final Identifier ATTACHMENT_DATA_ID =
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "attachment_data");
    /** 普通魔法水晶。 */
    public static final Identifier CRYSTAL_DATA_ID =
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "crystal_data");

    /** W03 临时测试组件 ID（接口约定 §2.2，W23 清理）。 */
    public static final Identifier W03_TEST_DATA_ID =
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "w03_test_data");

    private ComponentRegistry() {
    }

    public static void register() {
        // W06 正式组件（接口约定 §2）
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, CERT_DATA_ID, ModComponents.CERT_DATA);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, PROFICIENCY_DATA_ID, ModComponents.PROFICIENCY_DATA);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, PROMOTION_DATA_ID, ModComponents.PROMOTION_DATA);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, APPLICATION_DATA_ID, ModComponents.APPLICATION_DATA);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, STAFF_DATA_ID, ModComponents.STAFF_DATA);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ATTACHMENT_DATA_ID, ModComponents.ATTACHMENT_DATA);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, CRYSTAL_DATA_ID, ModComponents.CRYSTAL_DATA);

        // W03 临时测试组件（W23 清理）
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                W03_TEST_DATA_ID, ModComponents.W03_TEST_DATA);
    }
}
