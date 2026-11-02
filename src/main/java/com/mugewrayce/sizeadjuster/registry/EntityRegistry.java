package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.entity.ModEntities;

/**
 * 实体类型注册落点（W01-B 骨架）。
 * 当前先委托 1.x 的 {@link ModEntities}；2.0.0 新实体后续在此登记或迁移。
 */
public final class EntityRegistry {

    private EntityRegistry() {
    }

    public static void register() {
        ModEntities.register();
    }
}
