package com.mugewrayce.sizeadjuster.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mugewrayce.sizeadjuster.entity.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EntitySpawnReason;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * W18 新功能：召唤“没有生命的魔法体”。
 *
 * <pre>
 * /mugewrayce summon lifeless_magic_body [数量] [目标玩家]
 * </pre>
 *
 * <ul>
 *   <li>数量 1~16，省略为 1；</li>
 *   <li>目标省略 = 自己，填了 = 该玩家脚下；</li>
 *   <li>权限与 {@code itemgive} 同级（{@code COMMANDS_GAMEMASTER}）。</li>
 * </ul>
 */
public final class SummonCommand {

    /** 一次召唤的数量上限。 */
    private static final int MAX_COUNT = 16;

    private SummonCommand() {
    }

    /** 生成 {@code summon} 分支，由 {@link SizeCommand} 挂到 {@code /mugewrayce} 根指令下。 */
    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return literal("summon")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(literal("lifeless_magic_body")
                        .executes(ctx -> summon(ctx, 1, ctx.getSource().getPlayerOrException()))
                        .then(argument("count", IntegerArgumentType.integer(1, MAX_COUNT))
                                .executes(ctx -> summon(ctx,
                                        IntegerArgumentType.getInteger(ctx, "count"),
                                        ctx.getSource().getPlayerOrException()))
                                .then(argument("target", EntityArgument.player())
                                        .executes(ctx -> summon(ctx,
                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                EntityArgument.getPlayer(ctx, "target"))))
                        )
                        .then(argument("target", EntityArgument.player())
                                .executes(ctx -> summon(ctx, 1,
                                        EntityArgument.getPlayer(ctx, "target"))))
                );
    }

    private static int summon(CommandContext<CommandSourceStack> ctx, int count, ServerPlayer target) {
        if (!(target.level() instanceof ServerLevel level)) {
            ctx.getSource().sendFailure(Component.translatable("command.mugewrayce.summon.failed"));
            return 0;
        }
        BlockPos pos = target.blockPosition();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (ModEntities.LIFELESS_MAGIC_BODY.spawn(level, pos, EntitySpawnReason.COMMAND) != null) {
                spawned++;
            }
        }
        if (spawned <= 0) {
            ctx.getSource().sendFailure(Component.translatable("command.mugewrayce.summon.failed"));
            return 0;
        }
        int result = spawned;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.mugewrayce.summon.given", result, target.getDisplayName()), false);
        return result;
    }
}
