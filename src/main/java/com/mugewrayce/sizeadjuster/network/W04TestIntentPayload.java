package com.mugewrayce.sizeadjuster.network;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * W04 测试意图包（客户端 → 服务端，接口约定 §3.1）。
 *
 * <p><b>临时测试包</b>：只用来验证 2.0.0 的「客户端发意图 → 服务端重算 → 回状态」链路，
 * 不承载正式业务，最迟 W23 清理。</p>
 *
 * <p>服务端只信任 {@code action} 与 {@code value} 是"客户端请求"，不信任结果：
 * 一律交给 {@link W04TestProtocol#evaluate} 重算后再回 {@link W04TestStatePayload}；
 * 拒绝分支不改任何服务端状态（草案 §14.5）。</p>
 */
public record W04TestIntentPayload(int requestId, int action, int value) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<W04TestIntentPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "w04_test_intent"));

    public static final StreamCodec<RegistryFriendlyByteBuf, W04TestIntentPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, W04TestIntentPayload::requestId,
                    ByteBufCodecs.INT, W04TestIntentPayload::action,
                    ByteBufCodecs.INT, W04TestIntentPayload::value,
                    W04TestIntentPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 服务端接收：回主线程 → 纯函数重算 → 回状态。
     *
     * <p>拒绝分支没有副作用（不改世界/背包/资源），只回一个 rejected 状态。</p>
     */
    public static void handle(W04TestIntentPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            W04TestStatePayload state =
                    W04TestProtocol.evaluate(payload.requestId(), payload.action(), payload.value());
            ServerPlayNetworking.send(player, state);
            SizeAdjusterMod.LOGGER.debug(
                    "[W04-TEST] intent from {} requestId={} action={} value={} -> accepted={} reason={}",
                    player.getName().getString(), payload.requestId(), payload.action(),
                    payload.value(), state.accepted(), state.reason());
        });
    }
}
