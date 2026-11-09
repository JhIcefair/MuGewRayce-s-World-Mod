package com.mugewrayce.sizeadjuster.magic.cast;

import com.mugewrayce.sizeadjuster.entity.MagicBoltEntity;
import com.mugewrayce.sizeadjuster.magic.MagicNumbers;
import com.mugewrayce.sizeadjuster.magic.MagicSkill;
import com.mugewrayce.sizeadjuster.magic.SoundFx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 8 个自然技能的实际效果（W14b）。
 *
 * <p>本类只做“目标解析 + 效果”；耗魔由 {@link StaffCaster} 先通过
 * {@link Plan#cost()} 支付，成功后才执行 {@link Plan#action()}。
 * 目标不合法时返回 {@code null}（不施法、不扣魔）。</p>
 *
 * <p>技能范围按 W14 §2.10 A 定案：只作用瞄准的 1 格；耗魔 = 单价 × 1。
 * “可选择范围”记为遗留，不在本任务加 GUI。</p>
 */
public final class SkillEffects {

    /** 一次技能施法的计划：先扣魔、再执行。 */
    public record Plan(int cost, Runnable action) {
    }

    private static final double REACH = 64.0D;
    private static final double FREEZE_ENTITY_RADIUS = 2.5D;

    private SkillEffects() {
    }

    /**
     * 解析技能目标并返回计划；目标不合法返回 null。
     */
    public static Plan plan(ServerLevel level, Player player, MagicSkill skill) {
        if (skill == null) {
            return null;
        }
        return switch (skill) {
            case IGNITE -> planIgnite(level, player);
            case MELT_ICE -> planMelt(level, player);
            case FREEZE -> planFreeze(level, player);
            case MAKE_ICE -> planMakeIce(level, player);
            case SUMMON_LIGHTNING -> planLightning(level, player);
            case CREATE_WATER -> planWater(level, player);
            case FRONT_FLASH -> planFrontFlash(level, player);
            case TARGETED_FLASH -> planTargetedFlash(level, player);
            default -> null; // 起源技能不走这里
        };
    }

    private static Plan planIgnite(ServerLevel level, Player player) {
        BlockHitResult hit = pickBlock(level, player);
        if (hit == null) {
            return null;
        }
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        if (!level.getBlockState(pos).isAir()) {
            return null;
        }
        if (!BaseFireBlock.canBePlacedAt(level, pos, hit.getDirection())) {
            return null;
        }
        return new Plan(MagicNumbers.MANA_IGNITE_PER_BLOCK,
                () -> {
                    SoundFx.at(level, pos, SoundEvents.FLINTANDSTEEL_USE, 0.8F, 1.0F);
                    level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
                });
    }

    private static Plan planMelt(ServerLevel level, Player player) {
        BlockHitResult hit = pickBlock(level, player);
        if (hit == null) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        if (level.getBlockState(pos).is(Blocks.BLUE_ICE)) {
            return new Plan(MagicNumbers.MANA_MELT_PER_BLOCK,
                    () -> {
                        SoundFx.at(level, pos, SoundEvents.GLASS_BREAK, 0.7F, 1.1F);
                        level.setBlockAndUpdate(pos, Blocks.PACKED_ICE.defaultBlockState());
                    });
        }
        if (level.getBlockState(pos).is(Blocks.PACKED_ICE)) {
            return new Plan(MagicNumbers.MANA_MELT_PER_BLOCK,
                    () -> {
                        SoundFx.at(level, pos, SoundEvents.GLASS_BREAK, 0.7F, 1.1F);
                        level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                    });
        }
        if (level.getBlockState(pos).is(Blocks.ICE)) {
            return new Plan(MagicNumbers.MANA_MELT_PER_BLOCK,
                    () -> {
                        SoundFx.at(level, pos, SoundEvents.GLASS_BREAK, 0.7F, 1.1F);
                        level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                    });
        }
        return null;
    }

    private static Plan planFreeze(ServerLevel level, Player player) {
        BlockHitResult hit = pickBlock(level, player, ClipContext.Fluid.ANY);
        if (hit == null) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(pos).inflate(FREEZE_ENTITY_RADIUS), LivingEntity::isAlive);
        boolean blockFreezable = level.getBlockState(pos).is(Blocks.WATER)
                || level.getBlockState(pos).is(Blocks.ICE)
                || level.getBlockState(pos).is(Blocks.PACKED_ICE);
        if (!blockFreezable && entities.isEmpty()) {
            return null;
        }
        return new Plan(MagicNumbers.MANA_FREEZE_PER_BLOCK, () -> {
            SoundFx.at(level, pos, SoundEvents.POWDER_SNOW_PLACE, 0.8F, 0.9F);
            if (level.getBlockState(pos).is(Blocks.WATER)) {
                level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            } else if (level.getBlockState(pos).is(Blocks.ICE)) {
                level.setBlockAndUpdate(pos, Blocks.PACKED_ICE.defaultBlockState());
            } else if (level.getBlockState(pos).is(Blocks.PACKED_ICE)) {
                level.setBlockAndUpdate(pos, Blocks.BLUE_ICE.defaultBlockState());
            }
            for (LivingEntity entity : entities) {
                entity.setTicksFrozen(Math.min(400, entity.getTicksFrozen() + 100));
            }
        });
    }

    private static Plan planMakeIce(ServerLevel level, Player player) {
        BlockHitResult hit = pickBlock(level, player, ClipContext.Fluid.ANY);
        if (hit == null) {
            return null;
        }
        BlockPos hitPos = hit.getBlockPos();
        BlockPos pos = level.getBlockState(hitPos).is(Blocks.WATER)
                ? hitPos : hitPos.relative(hit.getDirection());
        if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).is(Blocks.WATER)) {
            return null;
        }
        return new Plan(MagicNumbers.MANA_MAKE_ICE_PER_BLOCK,
                () -> {
                    SoundFx.at(level, pos, SoundEvents.GLASS_PLACE, 0.8F, 1.2F);
                    level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                });
    }

    private static Plan planLightning(ServerLevel level, Player player) {
        BlockHitResult hit = pickBlock(level, player);
        if (hit == null) {
            return null;
        }
        Vec3 pos = hit.getLocation();
        return new Plan(MagicNumbers.MANA_SUMMON_LIGHTNING, () -> {
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
            bolt.setPos(pos.x, pos.y, pos.z);
            bolt.setCause(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                    ? serverPlayer : null);
            level.addFreshEntity(bolt);
        });
    }

    private static Plan planWater(ServerLevel level, Player player) {
        BlockHitResult hit = pickBlock(level, player, ClipContext.Fluid.ANY);
        if (hit == null) {
            return null;
        }
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        if (!level.getBlockState(pos).isAir()) {
            return null;
        }
        return new Plan(MagicNumbers.MANA_CREATE_WATER_PER_BLOCK,
                () -> {
                    SoundFx.at(level, pos, SoundEvents.BUCKET_FILL, 0.8F, 1.0F);
                    level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                });
    }

    private static Plan planFrontFlash(ServerLevel level, Player player) {
        // W17：落点改为“沿视线找到的第一个空气块”（从眼位向前步进，最多 4 格），
        // 避免固定脚下前方 1 格落在方块里导致永远放不下。
        BlockPos pos = firstAirAlongLook(level, player, 4);
        if (pos == null) {
            return null;
        }
        return new Plan(MagicNumbers.MANA_FLASH, () -> {
            if (FlashLightTracker.place(level, pos, MagicNumbers.FLASH_LIGHT_TICKS)) {
                SoundFx.at(level, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F, 1.4F);
            }
        });
    }

    /** 从眼位沿视线向前，返回第一个空气方块（最多 maxSteps 格）；找不到返回 null。 */
    private static BlockPos firstAirAlongLook(ServerLevel level, Player player, int maxSteps) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        BlockPos previous = null;
        for (int step = 0; step < maxSteps; step++) {
            Vec3 point = eye.add(look.scale(step + 0.5D));
            BlockPos pos = BlockPos.containing(point);
            if (pos.equals(previous)) {
                continue;
            }
            previous = pos;
            if (level.getBlockState(pos).isAir()) {
                return pos;
            }
        }
        return null;
    }

    private static Plan planTargetedFlash(ServerLevel level, Player player) {
        BlockHitResult hit = pickBlock(level, player);
        if (hit == null) {
            return null;
        }
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        if (!level.getBlockState(pos).isAir()) {
            return null;
        }
        return new Plan(MagicNumbers.MANA_FLASH, () -> {
            if (FlashLightTracker.place(level, pos, MagicNumbers.FLASH_LIGHT_TICKS)) {
                SoundFx.at(level, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F, 1.4F);
            }
        });
    }

    /** 玩家视线与方块碰撞；无命中返回 null（技能不施放）。 */
    private static BlockHitResult pickBlock(ServerLevel level, Player player) {
        return pickBlock(level, player, ClipContext.Fluid.NONE);
    }

    private static BlockHitResult pickBlock(ServerLevel level, Player player, ClipContext.Fluid fluid) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(REACH));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, fluid, player));
        return hit.getType() == HitResult.Type.MISS ? null : hit;
    }

    /** 生成一枚魔法弹（供 W14b 的 StaffCaster 用）。 */
    public static MagicBoltEntity bolt(ServerLevel level, Player player, int color, int kind, Vec3 pos,
                                       Vec3 direction) {
        MagicBoltEntity bolt = new MagicBoltEntity(level, player);
        bolt.setPos(pos);
        bolt.setColor(color);
        bolt.setKind(kind);
        bolt.shoot(direction.x, direction.y, direction.z, 4.5F, 0.0F);
        return bolt;
    }
}
