package com.mugewrayce.sizeadjuster.item;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 净化治愈效果集合（净化水晶长按 / 治愈魔法阵共用）。
 * 力量X、生命恢复20、抗性提升50、抗火、伤害吸收X、饱和。
 *
 * 注意：每次施加都必须 new 一个全新的 MobEffectInstance——
 * 游戏会把传入的实例直接存入目标效果表并逐 tick 递减其时长，
 * 若复用同一个实例，首次使用后其 duration 被扣到 0，后续施加将完全无效。
 */
public final class PurificationEffects {

    /** 净化水晶长按效果时长：60 秒。 */
    public static final int CRYSTAL_DURATION_TICKS = 20 * 60;

    /** 治愈魔法阵效果时长：3 秒（阵内每 tick 刷新保持）。 */
    public static final int FORMATION_DURATION_TICKS = 20 * 3;

    /** 效果定义：(效果, 等级放大数) —— X=9、20=19、50=49、抗火/饱和=0。 */
    private static final EffectSpec[] SPECS = {
            new EffectSpec(MobEffects.STRENGTH, 9),
            new EffectSpec(MobEffects.REGENERATION, 19),
            new EffectSpec(MobEffects.RESISTANCE, 49),
            new EffectSpec(MobEffects.FIRE_RESISTANCE, 0),
            new EffectSpec(MobEffects.ABSORPTION, 9),
            new EffectSpec(MobEffects.SATURATION, 0)
    };

    private PurificationEffects() {
    }

    /** 为目标施加全部净化治愈效果（每次全新实例，避免共享实例被游戏 tick 修改失效）。 */
    public static void apply(LivingEntity entity, int durationTicks) {
        for (EffectSpec spec : SPECS) {
            entity.addEffect(new MobEffectInstance(spec.effect(), durationTicks, spec.amplifier()));
        }
    }

    /** 效果定义记录（只存规格，不存可变实例）。 */
    private record EffectSpec(Holder<MobEffect> effect, int amplifier) {
    }
}
