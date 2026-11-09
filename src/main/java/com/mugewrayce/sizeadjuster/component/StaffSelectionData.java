package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 普通魔法杖当前选择（W12 新增，接口约定 §2，组件 ID {@code staff_selection_data}）。
 *
 * <p>三个字段都是稳定 ID 字符串；空三元组表示“未选择”。服务端写入前必须确认
 * {@code (type, mode, sub)} 确实存在于该法杖的 {@link StaffData#entries()} 中。
 * W12 只保存，W14 读取用于施法。</p>
 */
public record StaffSelectionData(String type, String mode, String sub) {

    public static final StaffSelectionData DEFAULT = new StaffSelectionData("", "", "");

    /** 防御性处理：缺省/null 一律落空串。 */
    public StaffSelectionData {
        type = type == null ? "" : type;
        mode = mode == null ? "" : mode;
        sub = sub == null ? "" : sub;
    }

    public static final Codec<StaffSelectionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type", "").forGetter(StaffSelectionData::type),
            Codec.STRING.optionalFieldOf("mode", "").forGetter(StaffSelectionData::mode),
            Codec.STRING.optionalFieldOf("sub", "").forGetter(StaffSelectionData::sub)
    ).apply(instance, StaffSelectionData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StaffSelectionData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, StaffSelectionData::type,
                    ByteBufCodecs.STRING_UTF8, StaffSelectionData::mode,
                    ByteBufCodecs.STRING_UTF8, StaffSelectionData::sub,
                    StaffSelectionData::new);

    /** 是否为空三元组（未选择）。 */
    public boolean isEmpty() {
        return type.isEmpty() && mode.isEmpty() && sub.isEmpty();
    }
}
