package com.mugewrayce.sizeadjuster.magic;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/**
 * 2.0.0 魔法伤害类型（草案 §8.1，W14a 注册）。
 *
 * <p>注册 ID {@code mugewrayce-mod:magic_damage}，只进 {@code minecraft:bypasses_armor}
 * 标签：不吃护甲，但仍受“保护”附魔与抗性提升减免。死亡消息沿用
 * {@code death.attack.magic_damage[.player]}（W02 已登记）。</p>
 *
 * <p>26.1 的 {@code DamageSources} 没有通用 {@code source(key, ...)}，因此用
 * {@code new DamageSource(holder, direct, causing)} 构造。</p>
 */
public final class MagicDamage {

    /** 伤害类型注册键（数据包文件 {@code data/mugewrayce-mod/damage_type/magic_damage.json}）。 */
    public static final ResourceKey<DamageType> KEY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "magic_damage"));

    private MagicDamage() {
    }

    /** 是否本模组定义的魔法伤害；只认类型，不再按来源实体/原版 magic 猜。 */
    public static boolean is(DamageSource source) {
        return source != null && source.typeHolder().is(KEY);
    }

    /**
     * 构造一次魔法伤害。
     *
     * @param direct  直接来源（魔法球 / 魔法阵 / 攻击者本人）
     * @param causing 归属实体（施法者 / 攻击者，可为 null）
     */
    public static DamageSource of(ServerLevel level, Entity direct, Entity causing) {
        Holder<DamageType> holder = level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(KEY);
        return new DamageSource(holder, direct, causing);
    }
}
