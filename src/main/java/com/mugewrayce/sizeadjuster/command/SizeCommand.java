package com.mugewrayce.sizeadjuster.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import com.mugewrayce.sizeadjuster.util.SizeAttributes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /mugewrayce 指令：
 * - size：玩家尺寸（adjust/reset/special/status）；
 * - entity：特殊角色切换——
 *   /mugewrayce entity default [目标玩家]             —— 默认角色（完全没有任何变化）
 *   /mugewrayce entity magic [目标玩家]               —— 魔法体（物理免疫）
 *   /mugewrayce entity special <特殊角色> [目标玩家]   —— 特殊角色
 *     （"特殊"下再增一字段选择采用哪种，目前仅 icefair，将来新增直接追加）。
 */
public class SizeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("mugewrayce")
            .then(literal("size")
                .then(literal("reset")
                    .executes(ctx -> executeReset(ctx.getSource().getPlayerOrException()))
                    .then(argument("target", EntityArgument.player())
                        .executes(ctx -> executeReset(EntityArgument.getPlayer(ctx, "target")))
                    )
                )
                .then(literal("adjust")
                    .then(argument("height", IntegerArgumentType.integer(10, 1000))
                        .executes(ctx -> executeAdjust(ctx.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(ctx, "height")))
                        .then(argument("target", EntityArgument.player())
                            .executes(ctx -> executeAdjust(EntityArgument.getPlayer(ctx, "target"),
                                    IntegerArgumentType.getInteger(ctx, "height")))
                        )
                    )
                )
                .then(literal("special")
                    .executes(ctx -> executeSpecial(ctx.getSource().getPlayerOrException()))
                    .then(argument("target", EntityArgument.player())
                        .executes(ctx -> executeSpecial(EntityArgument.getPlayer(ctx, "target")))
                    )
                )
                .then(literal("status")
                    .executes(ctx -> executeStatus(ctx.getSource().getPlayerOrException()))
                    .then(argument("target", EntityArgument.player())
                        .executes(ctx -> executeStatus(EntityArgument.getPlayer(ctx, "target")))
                    )
                )
            )
            .then(literal("entity")
                .then(literal("default")
                    .executes(ctx -> executeRole(ctx.getSource().getPlayerOrException(),
                            PlayerRoles.ROLE_DEFAULT))
                    .then(argument("target", EntityArgument.player())
                        .executes(ctx -> executeRole(EntityArgument.getPlayer(ctx, "target"),
                                PlayerRoles.ROLE_DEFAULT))
                    )
                )
                .then(literal("magic")
                    .executes(ctx -> executeRole(ctx.getSource().getPlayerOrException(),
                            PlayerRoles.ROLE_MAGIC_BODY))
                    .then(argument("target", EntityArgument.player())
                        .executes(ctx -> executeRole(EntityArgument.getPlayer(ctx, "target"),
                                PlayerRoles.ROLE_MAGIC_BODY))
                    )
                )
                .then(literal("special")
                    .then(argument("specialId", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String id : PlayerRoles.SPECIAL_IDS) {
                                builder.suggest(id);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> executeSpecialRole(ctx, ctx.getSource().getPlayerOrException()))
                        .then(argument("target", EntityArgument.player())
                            .executes(ctx -> executeSpecialRole(ctx, EntityArgument.getPlayer(ctx, "target")))
                        )
                    )
                )
            )
        );
    }

    /** "special" 分支：按特殊角色字段名解析并切换。 */
    private static int executeSpecialRole(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String id = StringArgumentType.getString(ctx, "specialId");
        int index = PlayerRoles.SPECIAL_IDS.indexOf(id);
        if (index < 0) {
            player.sendSystemMessage(Component.literal(
                    "未知特殊角色：" + id + "（目前可选：" + String.join("/", PlayerRoles.SPECIAL_IDS) + "）"));
            return 0;
        }
        return executeRole(player, PlayerRoles.specialCode(index));
    }

    private static int executeRole(ServerPlayer player, int role) {
        PlayerRoles.applyRole(player, role);
        player.sendSystemMessage(Component.literal("已切换角色：" + PlayerRoles.roleLabel(role)));
        return 1;
    }

    private static int executeReset(ServerPlayer player) {
        SizeAttributes.resetToDefault(player);
        player.sendSystemMessage(Component.literal("已恢复默认尺寸"));
        return 1;
    }

    private static int executeAdjust(ServerPlayer player, int heightCm) {
        if (!SizeAttributes.isHeightAllowed(heightCm)) {
            player.sendSystemMessage(Component.literal("身高必须在 10 到 1000 之间"));
            return 0;
        }
        SizeAttributes.applyHeight(player, heightCm);
        player.sendSystemMessage(Component.literal("身高已调整为 " + heightCm + " cm"));
        return 1;
    }

    private static int executeSpecial(ServerPlayer player) {
        SizeAttributes.applySpecial(player);
        player.sendSystemMessage(Component.literal("已应用特殊组合"));
        return 1;
    }

    private static int executeStatus(ServerPlayer player) {
        // 多行合并为一条消息，保持原来"一次输出整块状态"的观感
        MutableComponent status = Component.empty();
        List<Component> lines = SizeAttributes.getStatusLines(player);
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                status.append("\n");
            }
            status.append(lines.get(i));
        }
        player.sendSystemMessage(status);
        return 1;
    }
}
