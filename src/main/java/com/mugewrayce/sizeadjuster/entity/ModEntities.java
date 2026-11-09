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
 * - lifeless_magic_body（没有生命的魔法体）：敌对生物，玩家模型；
 *   生成/战斗数值留 W13/W14，本任务只做内容层与接缝。
 * - trial_magic_body（试炼魔法体）：考试/试炼配置变体，无 AI/无敌，具体配置 W15。
 */
public final class ModEntities {

    public static EntityType<EquipoiseOrbEntity> EQUIPOISE_ORB;
    public static EntityType<MagicBoltEntity> MAGIC_BOLT;
    public static EntityType<EquipoiseCircleEntity> EQUIPOISE_CIRCLE;
    public static EntityType<EquipoiseFormationEntity> EQUIPOISE_FORMATION;
    public static EntityType<LifelessMagicBodyEntity> LIFELESS_MAGIC_BODY;
    public static EntityType<TrialMagicBodyEntity> TRIAL_MAGIC_BODY;

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

        MAGIC_BOLT = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                key("magic_bolt"),
                EntityType.Builder.<MagicBoltEntity>of(
                        (type, level) -> new MagicBoltEntity(type, level), MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .noSave()
                        .clientTrackingRange(8)
                        .updateInterval(1)
                        .build(key("magic_bolt")));

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

        LIFELESS_MAGIC_BODY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                key("lifeless_magic_body"),
                EntityType.Builder.<LifelessMagicBodyEntity>of(
                        (type, level) -> new LifelessMagicBodyEntity(type, level), MobCategory.MONSTER)
                        .sized(0.6F, 1.8F)
                        .clientTrackingRange(10)
                        .updateInterval(2)
                        .build(key("lifeless_magic_body")));

        TRIAL_MAGIC_BODY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                key("trial_magic_body"),
                EntityType.Builder.<TrialMagicBodyEntity>of(
                        (type, level) -> new TrialMagicBodyEntity(type, level), MobCategory.MONSTER)
                        .sized(0.6F, 1.8F)
                        // W15 / D-12：去掉 noSave，草案 §9.4 要求“退出登录 / 重启后继续”
                        .clientTrackingRange(10)
                        .updateInterval(2)
                        .build(key("trial_magic_body")));
    }

    private static ResourceKey<EntityType<?>> key(String id) {
        return ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id));
    }
}
