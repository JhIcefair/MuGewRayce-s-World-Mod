package com.mugewrayce.sizeadjuster.worldgen;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * 水晶块的世界生成注入（W20）。
 *
 * <p>26.1 的数据包只有 {@code configured_feature} / {@code placed_feature}，
 * 没有 {@code biome_modifier}；主世界注入走 Fabric 的 {@link BiomeModifications}：</p>
 * <ul>
 *   <li>{@code crystal_block_underground}：地下矿团，每次尝试 3 团、高度 0~120、稀有度 1/500；</li>
 *   <li>{@code crystal_block_surface}：海平面以上地表，稀有度 1/10000。</li>
 * </ul>
 *
 * <p>只加在主世界（{@link BiomeSelectors#foundInOverworld()}），下界 / 末地不生成。</p>
 */
public final class CrystalWorldgen {

    private CrystalWorldgen() {
    }

    public static void register() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                key("crystal_block_underground"));
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
                key("crystal_block_surface"));
    }

    private static ResourceKey<PlacedFeature> key(String id) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id));
    }
}
