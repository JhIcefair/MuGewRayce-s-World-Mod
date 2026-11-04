package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 普通魔法水晶数据（接口约定 §2，组件 ID {@code crystal_data}）。
 *
 * <p>{@code mana} 0~30000（容量 30000、维持存在值 800）；{@code supplyEnabled} 是否供能；
 * {@code autoSupplyEnabled} 是否自动供能。</p>
 *
 * <p>无效态：{@code autoSupplyEnabled && !supplyEnabled}（只开自动供能，草案 §3.17 / §10.8）。</p>
 */
public record CrystalData(int mana, boolean supplyEnabled, boolean autoSupplyEnabled) {

    public static final int CAPACITY = 30000;

    public static final CrystalData DEFAULT = new CrystalData(0, false, false);

    public static final Codec<CrystalData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, CAPACITY).optionalFieldOf("mana", 0).forGetter(CrystalData::mana),
            Codec.BOOL.optionalFieldOf("supplyEnabled", false).forGetter(CrystalData::supplyEnabled),
            Codec.BOOL.optionalFieldOf("autoSupplyEnabled", false).forGetter(CrystalData::autoSupplyEnabled)
    ).apply(instance, CrystalData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrystalData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, CrystalData::mana,
                    ByteBufCodecs.BOOL, CrystalData::supplyEnabled,
                    ByteBufCodecs.BOOL, CrystalData::autoSupplyEnabled,
                    CrystalData::new);
}
