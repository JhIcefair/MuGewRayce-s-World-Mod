package com.mugewrayce.sizeadjuster.component;

import net.minecraft.core.component.DataComponentType;

/**
 * 模组数据组件类型（W03 骨架）。
 *
 * <p>这里只放 {@link DataComponentType} 常量，真正的注册在
 * {@code registry/ComponentRegistry}（统一走注册落点，接口约定 §6）。</p>
 *
 * <p>目前只有 W03 的临时测试组件；五类正式组件（学习证 / 证明 / 认证 / 法杖 / 附加书 / 水晶）
 * 在 W06 落地。每个组件都<b>必须同时</b>给 {@code persistent} 与 {@code networkSynchronized}：
 * 只做持久化则客户端拿不到，只做网络则退出重进会丢。</p>
 */
public final class ModComponents {

    /** W03 临时测试组件（接口约定 §2.1，W23 清理）。 */
    public static final DataComponentType<W03TestData> W03_TEST_DATA =
            DataComponentType.<W03TestData>builder()
                    .persistent(W03TestData.CODEC)
                    .networkSynchronized(W03TestData.STREAM_CODEC)
                    .build();

    private ModComponents() {
    }
}
