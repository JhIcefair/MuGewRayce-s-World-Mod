package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * 魔法学习证数据（接口约定 §2，组件 ID {@code cert_data}）。
 *
 * <p>{@code no} 为服务端分配的编号（1 起，显示 {@code %04d}）；{@code 0} 表示未分配/无效。
 * {@code entries} 记录该证学过的类型 / 模式 / 子模式 / 技能与等级。</p>
 *
 * <p>没有此组件（或其字段非法）= 无效证：创造模式直接拿到的学习证不可用（草案 §3.19 / §13.4）。</p>
 */
public record CertData(int no, List<CertEntry> entries) {

    public static final CertData DEFAULT = new CertData(0, List.of());

    /** 防御性复制：record 不可变，不暴露可变列表。 */
    public CertData {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static final Codec<CertData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("no", 0).forGetter(CertData::no),
            CertEntry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(CertData::entries)
    ).apply(instance, CertData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CertData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, CertData::no,
                    CertEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), CertData::entries,
                    CertData::new);
}
