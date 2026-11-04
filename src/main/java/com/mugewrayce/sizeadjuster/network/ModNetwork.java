package com.mugewrayce.sizeadjuster.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * 2.0.0 网络注册入口（W04 起）。
 *
 * <p>边界（接口约定 §3.1）：这里<b>只注册新包</b>；1.x 的 {@code payload/}
 * （Size / Role / Staff / Switch）保持原有注册方式，不搬进来，避免 W04 变成网络大重构。</p>
 *
 * <p>方向不能写反：C2S 注册在 {@code serverboundPlay}，S2C 注册在 {@code clientboundPlay}；
 * 同一个 {@code CustomPacketPayload.Type} 只能注册一次。</p>
 *
 * <p>客户端专属的 S2C receiver 不在这里注册——见 {@code client/network/W04TestClient}，
 * common 侧不得引用客户端网络类。</p>
 */
public final class ModNetwork {

    private ModNetwork() {
    }

    public static void register() {
        // W04 临时测试包（W23 清理）
        PayloadTypeRegistry.serverboundPlay().register(
                W04TestIntentPayload.TYPE, W04TestIntentPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                W04TestStatePayload.TYPE, W04TestStatePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                W04TestIntentPayload.TYPE, W04TestIntentPayload::handle);
    }
}
