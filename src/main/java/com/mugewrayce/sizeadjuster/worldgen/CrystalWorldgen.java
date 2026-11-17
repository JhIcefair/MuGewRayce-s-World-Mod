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
 * 水晶块的世界生成注入（W20；W23 删除海平面以上地表规则）。
 *
 * <p>26.1 的数据包只有 {@code configured_feature} / {@code placed_feature}，
 * 没有 {@code biome_modifier}；主世界注入走 Fabric 的 {@link BiomeModifications}：</p>
 * <ul>
 *   <li>{@code crystal_block_underground}：地下矿团，每区块 5 次尝试、每次稀有度 1/25、
 *       高度 0~120（相对世界底部），矿团 3 方块。</li>
 * </ul>
 *
 * <p><b>W23 起不再有“海平面以上地表”规则</b>：该规则原本要求生成块不得暴露于空气，
 * 实现成本与可读性都不划算，整条删除；水晶块只在海平面以下的地下生成。</p>
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
    }

    private static ResourceKey<PlacedFeature> key(String id) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id));
    }
}
