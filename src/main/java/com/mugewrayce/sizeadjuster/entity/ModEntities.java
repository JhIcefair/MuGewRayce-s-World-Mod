package com.mugewrayce.sizeadjuster.entity;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * 模组实体注册入口。
 * - purification_orb（净化魔法球）：直线飞行投射物，由净化法杖 / 归零 发射
 *   （实体变体 variant 区分，见 PurificationOrbEntity）；尺寸 0.25x0.25。
 * - purification_circle（净化法阵）：净化水晶长按右键时出现在玩家脚下的魔法阵；
 *   尺寸 2.0x0.1（贴图 256x256 地面方形）。
 * - purification_formation（净化魔法阵·法杖施法）：单体/群体/防御五类阵法
 *   （类型见 PurificationFormationEntity）；尺寸随类型（单2 / 群6 / 防御按身高）。
 */
public final class ModEntities {

    public static EntityType<PurificationOrbEntity> PURIFICATION_ORB;
    public static EntityType<PurificationCircleEntity> PURIFICATION_CIRCLE;
    public static EntityType<PurificationFormationEntity> PURIFICATION_FORMATION;

    private ModEntities() {
    }

    public static void register() {
        PURIFICATION_ORB = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                key("purification_orb"),
                EntityType.Builder.<PurificationOrbEntity>of(
                        (type, level) -> new PurificationOrbEntity(type, level), MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .noSave()
                        .clientTrackingRange(8)
                        .updateInterval(2)
                        .build(key("purification_orb")));

        PURIFICATION_CIRCLE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                key("purification_circle"),
                EntityType.Builder.<PurificationCircleEntity>of(
                        (type, level) -> new PurificationCircleEntity(type, level), MobCategory.MISC)
                        .sized(2.0F, 0.1F)
                        .noSave()
                        .clientTrackingRange(4)
                        .updateInterval(1)
                        .build(key("purification_circle")));

        PURIFICATION_FORMATION = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                key("purification_formation"),
                EntityType.Builder.<PurificationFormationEntity>of(
                        (type, level) -> new PurificationFormationEntity(type, level), MobCategory.MISC)
                        .sized(6.0F, 0.1F)
                        .noSave()
                        .clientTrackingRange(8)
                        .updateInterval(1)
                        .build(key("purification_formation")));
    }

    private static ResourceKey<EntityType<?>> key(String id) {
        return ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id));
    }
}
