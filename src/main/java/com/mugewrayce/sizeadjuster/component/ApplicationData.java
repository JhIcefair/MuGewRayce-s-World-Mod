package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 考试 / 试炼申请书数据（接口约定 §2，组件 ID {@code application_data}）。
 *
 * <p>两份申请书共用：{@code targetLevel=1} → 初学魔法考试申请书；
 * {@code targetLevel=2~5} → 魔法试炼申请书（草案 §3.21 / §3.23）。</p>
 *
 * <p>只记录"类型 + 模式（+具体项）+ 目标等级"，**不记录编号、不绑定学习证**。</p>
 */
public record ApplicationData(String type, String mode, String sub, int targetLevel) {

    public static final ApplicationData DEFAULT = new ApplicationData("", "", "", 0);

    public static final Codec<ApplicationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type", "").forGetter(ApplicationData::type),
            Codec.STRING.optionalFieldOf("mode", "").forGetter(ApplicationData::mode),
            Codec.STRING.optionalFieldOf("sub", "").forGetter(ApplicationData::sub),
            Codec.intRange(0, 5).optionalFieldOf("targetLevel", 0).forGetter(ApplicationData::targetLevel)
    ).apply(instance, ApplicationData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ApplicationData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ApplicationData::type,
                    ByteBufCodecs.STRING_UTF8, ApplicationData::mode,
                    ByteBufCodecs.STRING_UTF8, ApplicationData::sub,
                    ByteBufCodecs.INT, ApplicationData::targetLevel,
                    ApplicationData::new);
}
