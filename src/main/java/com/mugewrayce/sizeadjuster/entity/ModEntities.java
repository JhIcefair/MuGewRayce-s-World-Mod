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
 * - equipoise_orb（净化魔法球）：直线飞行投射物，由净化法杖 / 归零 发射
 *   （实体变体 variant 区分，见 EquipoiseOrbEntity）；尺寸 0.25x0.25。
 * - equipoise_circle（净化法阵）：净化水晶长按右键时出现在玩家脚下的魔法阵；
 *   尺寸 2.0x0.1（贴图 256x256 地面方形）。
 * - equipoise_formation（净化魔法阵·法杖施法）：单体/群体/防御五类阵法
 *   （类型见 EquipoiseFormationEntity）；尺寸随类型（单2 / 群6 / 防御按身高）。
 */
public final class ModEntities {

    public static EntityType<EquipoiseOrbEntity> EQUIPOISE_ORB;
    public static EntityType<EquipoiseCircleEntity> EQUIPOISE_CIRCLE;
    public static EntityType<EquipoiseFormationEntity> EQUIPOISE_FORMATION;

    private ModEntities() {
    }

    public static void register() {
        EQUIPOISE_ORB = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                key("equipoise_orb"),
                EntityType.Builder.<EquipoiseOrbEntity>of(
                        (type, level) -> new EquipoiseOrbEntity(type, level), MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .noSave()
                        .clientTrackingRange(8)
                        .updateInterval(2)
                        .build(key("equipoise_orb")));

        EQUIPOISE_CIRCLE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                key("equipoise_circle"),
                EntityType.Builder.<EquipoiseCircleEntity>of(
                        (type, level) -> new EquipoiseCircleEntity(type, level), MobCategory.MISC)
                        .sized(2.0F, 0.1F)
                        .noSave()
                        .clientTrackingRange(4)
                        .updateInterval(1)
                        .build(key("equipoise_circle")));

        EQUIPOISE_FORMATION = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                key("equipoise_formation"),
                EntityType.Builder.<EquipoiseFormationEntity>of(
                        (type, level) -> new EquipoiseFormationEntity(type, level), MobCategory.MISC)
                        .sized(6.0F, 0.1F)
                        .noSave()
                        .clientTrackingRange(8)
                        .updateInterval(1)
                        .build(key("equipoise_formation")));
    }

    private static ResourceKey<EntityType<?>> key(String id) {
        return ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id));
    }
}
