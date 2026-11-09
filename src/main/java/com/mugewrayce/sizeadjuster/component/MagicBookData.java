package com.mugewrayce.sizeadjuster.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * 魔法书设置（W12 新增，接口约定 §2，组件 ID {@code magic_book_data}）。
 *
 * <p>{@code skill} 为 {@code "time_pause"} / {@code "space_teleport"} / {@code ""}（未选择）；
 * {@code range} 0~7 表示区块边长（0=未设置；时间暂停 1..7、空间传送 1..5）；
 * {@code targetX/Y/Z} 为绝对方块坐标；{@code excluded} 用 {@code entity:<uuid>} /
 * {@code type:<id>} 字符串。W12 只保存，W14 读取并结算效果/耗魔/范围。</p>
 */
public record MagicBookData(String skill, int range, int targetX, int targetY, int targetZ,
                            List<String> excluded) {

    public static final String SKILL_TIME_PAUSE = "time_pause";
    public static final String SKILL_SPACE_TELEPORT = "space_teleport";

    public static final int MAX_RANGE_TIME_PAUSE = 7;
    public static final int MAX_RANGE_SPACE_TELEPORT = 5;

    public static final MagicBookData DEFAULT = new MagicBookData("", 0, 0, 0, 0, List.of());

    /** 防御性复制：列表不对外暴露可变引用。 */
    public MagicBookData {
        skill = skill == null ? "" : skill;
        excluded = excluded == null ? List.of() : List.copyOf(excluded);
    }

    public static final Codec<MagicBookData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("skill", "").forGetter(MagicBookData::skill),
            Codec.intRange(0, 7).optionalFieldOf("range", 0).forGetter(MagicBookData::range),
            Codec.INT.optionalFieldOf("targetX", 0).forGetter(MagicBookData::targetX),
            Codec.INT.optionalFieldOf("targetY", 0).forGetter(MagicBookData::targetY),
            Codec.INT.optionalFieldOf("targetZ", 0).forGetter(MagicBookData::targetZ),
            Codec.STRING.listOf().optionalFieldOf("excluded", List.of()).forGetter(MagicBookData::excluded)
    ).apply(instance, MagicBookData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagicBookData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, MagicBookData::skill,
                    ByteBufCodecs.INT, MagicBookData::range,
                    ByteBufCodecs.INT, MagicBookData::targetX,
                    ByteBufCodecs.INT, MagicBookData::targetY,
                    ByteBufCodecs.INT, MagicBookData::targetZ,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), MagicBookData::excluded,
                    MagicBookData::new);

    /** 技能是否合法（空串表示未选择）。 */
    public static boolean isValidSkill(String skill) {
        return skill == null
                || skill.isEmpty()
                || SKILL_TIME_PAUSE.equals(skill)
                || SKILL_SPACE_TELEPORT.equals(skill);
    }

    /** 指定技能的最大范围（区块边长）；未选择/未知技能返回 0。 */
    public static int maxRange(String skill) {
        if (SKILL_TIME_PAUSE.equals(skill)) {
            return MAX_RANGE_TIME_PAUSE;
        }
        if (SKILL_SPACE_TELEPORT.equals(skill)) {
            return MAX_RANGE_SPACE_TELEPORT;
        }
        return 0;
    }

    /** 范围是否与技能匹配：未选技能必须为 0；已选技能 1..对应上限。 */
    public static boolean isValidRange(String skill, int range) {
        int max = maxRange(skill);
        if (max == 0) {
            return range == 0;
        }
        return range >= 1 && range <= max;
    }
}
