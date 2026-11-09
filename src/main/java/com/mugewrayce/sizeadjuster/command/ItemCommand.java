package com.mugewrayce.sizeadjuster.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.item.InvalidItemRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 2.0.0 物品指令获取入口（草案 §14.9 / 接口约定 §7；W17 改为单字段 {@code itemgive}）。
 *
 * <p>用法（挂在已有的 {@code /mugewrayce} 根指令下）：</p>
 * <pre>
 * /mugewrayce itemgive &lt;id&gt; [数量] [目标玩家]
 * </pre>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>{@code id} <b>只接受本模组物品</b>：带冒号必须写 {@code mugewrayce-mod:}；
 *       不带冒号一律按本模组命名空间解析；其它命名空间按“未知的本模组物品”拒绝；</li>
 *   <li>建议列表只列本模组命名空间下的物品 ID，不再列全注册表；</li>
 *   <li>走的是注册表查询，<b>不新增任何物品注册</b>；</li>
 *   <li>对"无组件即无效"的物品（见 {@link InvalidItemRules}）额外提示当前视为无效物品，
 *       但<b>不阻止</b>发放：草案 §14.9 要求这些物品"拿得到、只是无效"；</li>
 *   <li>权限：与 {@code /give} 一致，需要"游戏管理员"级指令权限；普通玩家不能用它给自己刷物品。</li>
 * </ul>
 */
public final class ItemCommand {

    private ItemCommand() {
    }

    /** 生成 {@code itemgive} 分支，由 {@link SizeCommand} 挂到 {@code /mugewrayce} 根指令下。 */
    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return literal("itemgive")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(argument("id", StringArgumentType.string())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                suggestions(), builder))
                        .executes(ctx -> give(ctx, 1, ctx.getSource().getPlayerOrException()))
                        .then(argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> give(ctx,
                                        IntegerArgumentType.getInteger(ctx, "count"),
                                        ctx.getSource().getPlayerOrException()))
                                .then(argument("target", EntityArgument.player())
                                        .executes(ctx -> give(ctx,
                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                EntityArgument.getPlayer(ctx, "target"))))
                        )
                        .then(argument("target", EntityArgument.player())
                                .executes(ctx -> give(ctx, 1,
                                        EntityArgument.getPlayer(ctx, "target"))))
                );
    }

    private static int give(CommandContext<CommandSourceStack> ctx, int count, ServerPlayer target) {
        String raw = StringArgumentType.getString(ctx, "id");
        Identifier id = resolve(raw);
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.mugewrayce.itemgive.unknown", raw));
            return 0;
        }

        ItemStack stack = new ItemStack(item, count);
        // 名称要在发放前取：add() 后栈可能被清空，空栈 getHoverName() 会变成 "Air"
        Component name = stack.getHoverName();
        if (!target.getInventory().add(stack)) {
            target.drop(stack, false);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.mugewrayce.item.given", name, count, target.getDisplayName()), false);

        if (InvalidItemRules.requiresDataComponent(id)) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "command.mugewrayce.item.invalid", name), false);
        }
        return count;
    }

    /**
     * 解析物品 ID：只接受本模组命名空间。
     *
     * <p>带冒号按全名解析（命名空间不是 {@code mugewrayce-mod} → 拒绝）；
     * 不带冒号一律补本模组命名空间。解析失败或不合法返回 {@code null}。</p>
     */
    private static Identifier resolve(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String full = raw.indexOf(':') >= 0 ? raw : SizeAdjusterMod.MOD_ID + ":" + raw;
        Identifier id = Identifier.tryParse(full);
        if (id == null || !SizeAdjusterMod.MOD_ID.equals(id.getNamespace())) {
            return null;
        }
        return id;
    }

    /** 建议列表：只列本模组命名空间下的物品 ID。 */
    private static List<String> suggestions() {
        return BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> SizeAdjusterMod.MOD_ID.equals(id.getNamespace()))
                .map(Identifier::toString)
                .toList();
    }
}
