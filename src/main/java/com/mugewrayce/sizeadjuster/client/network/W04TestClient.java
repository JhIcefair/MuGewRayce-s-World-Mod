package com.mugewrayce.sizeadjuster.client.network;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.network.W04TestIntentPayload;
import com.mugewrayce.sizeadjuster.network.W04TestStatePayload;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * W04 客户端测试入口（接口约定 §3.1，临时，W23 清理）。
 *
 * <p>只做两件事：</p>
 * <ul>
 *   <li>注册 S2C receiver：收到 {@link W04TestStatePayload} 后把四个字段写进日志（不弹 GUI）；</li>
 *   <li>注册临时客户端命令 {@code /mugewrayce-client network-test <action> <value>}，
 *       用递增 {@code requestId} 发送 {@link W04TestIntentPayload}。</li>
 * </ul>
 *
 * <p>这是调试入口，<b>不是正式 GUI</b>：不新增语言键、不加按钮；W23 清理时连同命令一起删。</p>
 */
public final class W04TestClient {

    /** 客户端请求号：只需在本客户端内单调递增，不持久化。 */
    private static final AtomicInteger NEXT_REQUEST_ID = new AtomicInteger(1);

    private W04TestClient() {
    }

    public static void register() {
        // S2C：只能在客户端注册；common 侧不得引用本类
        ClientPlayNetworking.registerGlobalReceiver(W04TestStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> SizeAdjusterMod.LOGGER.info(
                        "[W04-TEST] state requestId={} accepted={} value={} reason={}",
                        payload.requestId(), payload.accepted(), payload.value(), payload.reason())));

        // 临时客户端命令（W23 清理）
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
                dispatcher.register(ClientCommands.literal("mugewrayce-client")
                        .then(ClientCommands.literal("network-test")
                                .then(ClientCommands.argument("action", IntegerArgumentType.integer())
                                        .then(ClientCommands.argument("value", IntegerArgumentType.integer())
                                                .executes(ctx -> sendTestIntent(
                                                        IntegerArgumentType.getInteger(ctx, "action"),
                                                        IntegerArgumentType.getInteger(ctx, "value"))))))));
    }

    private static int sendTestIntent(int action, int value) {
        int requestId = NEXT_REQUEST_ID.getAndIncrement();
        ClientPlayNetworking.send(new W04TestIntentPayload(requestId, action, value));
        SizeAdjusterMod.LOGGER.info(
                "[W04-TEST] sent intent requestId={} action={} value={}", requestId, action, value);
        return 1;
    }
}
