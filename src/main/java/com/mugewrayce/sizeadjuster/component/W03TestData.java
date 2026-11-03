package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * W03 临时测试组件的数据（接口约定 §2.1）。
 *
 * <p><b>非正式组件</b>：只用来验证 26.1 的 DataComponent 管线——注册、持久化 Codec、
 * 网络 StreamCodec、缺字段默认值、物品栈复制；不绑任何正式物品，最迟 W23 清理。</p>
 *
 * <p>字段与默认值（冻结）：</p>
 * <ul>
 *   <li>{@code number}：{@code int}，非负，默认 {@code 0}；</li>
 *   <li>{@code label}：{@code String}，默认 {@code ""}；</li>
 *   <li>{@code optional}：{@code int}，默认 {@code 0}（专门验证"缺字段落默认值"）。</li>
 * </ul>
 *
 * <p>规则：record 不可变、不持有 {@code Level} / {@code Player} / {@code ItemStack}；
 * 持久化 Codec 三个字段全部 {@code optionalFieldOf}，旧数据缺字段不报错；
 * 网络 Codec 两端总是写全字段（默认值只用于持久化/旧数据）。</p>
 */
public record W03TestData(int number, String label, int optional) {

    /** 默认值（全字段缺省时的取值）。 */
    public static final W03TestData DEFAULT = new W03TestData(0, "", 0);

    /** 持久化 Codec：用于存档 / 物品 NBT；缺字段落默认值，非法 number 由 intRange 拒绝。 */
    public static final Codec<W03TestData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE)
                    .optionalFieldOf("number", 0)
                    .forGetter(W03TestData::number),
            Codec.STRING
                    .optionalFieldOf("label", "")
                    .forGetter(W03TestData::label),
            Codec.INT
                    .optionalFieldOf("optional", 0)
                    .forGetter(W03TestData::optional)
    ).apply(instance, W03TestData::new));

    /** 网络 Codec：用于物品栈同步（两端总写全字段，不承担默认值）。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, W03TestData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, W03TestData::number,
                    ByteBufCodecs.STRING_UTF8, W03TestData::label,
                    ByteBufCodecs.INT, W03TestData::optional,
                    W03TestData::new);
}
