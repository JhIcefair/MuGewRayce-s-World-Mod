package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * 普通魔法杖数据（接口约定 §2，组件 ID {@code staff_data}）。
 *
 * <p>{@code boundCertNo} = 合成时绑定的学习证编号；{@code 0} 表示未绑定
 * （创造模式直接拿的法杖，升级会被拒绝）。{@code entries} 记录已附加的
 * 类型 / 模式 / 技能；只允许自然魔法（草案 §3.16）。</p>
 */
public record StaffData(int boundCertNo, List<CertEntry> entries) {

    public static final StaffData DEFAULT = new StaffData(0, List.of());

    /** 防御性复制。 */
    public StaffData {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static final Codec<StaffData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("boundCertNo", 0).forGetter(StaffData::boundCertNo),
            CertEntry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(StaffData::entries)
    ).apply(instance, StaffData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StaffData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, StaffData::boundCertNo,
                    CertEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), StaffData::entries,
                    StaffData::new);
}
