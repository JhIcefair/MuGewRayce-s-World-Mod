package com.mugewrayce.sizeadjuster.registry;

import com.mugewrayce.sizeadjuster.entity.LifelessMagicBodyEntity;
import com.mugewrayce.sizeadjuster.entity.MagicBodySpawner;
import com.mugewrayce.sizeadjuster.entity.ModEntities;
import com.mugewrayce.sizeadjuster.entity.TrialMagicBodyEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/**
 * 实体类型注册落点。
 * 净化系实体由 {@link ModEntities} 注册；W08 追加两种魔法体并注册默认属性。
 */
public final class EntityRegistry {

    private EntityRegistry() {
    }

    public static void register() {
        ModEntities.register();

        FabricDefaultAttributeRegistry.register(
                ModEntities.LIFELESS_MAGIC_BODY, LifelessMagicBodyEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(
                ModEntities.TRIAL_MAGIC_BODY, TrialMagicBodyEntity.createAttributes());

        // 每 3 分钟生成调度骨架（W13 接区块魔力概率/消耗；W08 不实际生成）。
        MagicBodySpawner.register();
    }
}
