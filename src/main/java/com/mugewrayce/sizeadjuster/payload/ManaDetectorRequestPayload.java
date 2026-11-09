package com.mugewrayce.sizeadjuster.payload;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.magic.mana.ManaDetectorRequests;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * 魔力值探测器请求（C2S，W12）。
 *
 * <p>客户端发送“按下那一刻”所在区块；服务端校验主手是探测器、中心区块与玩家
 * 当前位置不过分偏离后，在请求瞬间取样 25 个值，3 秒后回
 * {@link ManaDetectorResultPayload}。W13 才填真实值。</p>
 */
public record ManaDetectorRequestPayload(int centerChunkX, int centerChunkZ)
        implements CustomPacketPayload {

    /** 允许请求中心与玩家当前区块的最大偏差（防异常请求）。 */
    public static final int MAX_CHUNK_DISTANCE = 8;

    public static final CustomPacketPayload.Type<ManaDetectorRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    SizeAdjusterMod.MOD_ID, "mana_detector_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManaDetectorRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, ManaDetectorRequestPayload::centerChunkX,
                    ByteBufCodecs.INT, ManaDetectorRequestPayload::centerChunkZ,
                    ManaDetectorRequestPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, ManaDetectorRequestPayload::handle);
    }

    private static void handle(ManaDetectorRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player.getMainHandItem().getItem() != MagicItems.manaDetector) {
                return;
            }
            ChunkPos current = player.chunkPosition();
            if (Math.abs(payload.centerChunkX() - current.x()) > MAX_CHUNK_DISTANCE
                    || Math.abs(payload.centerChunkZ() - current.z()) > MAX_CHUNK_DISTANCE) {
                return;
            }
            ManaDetectorRequests.schedule(player, payload.centerChunkX(), payload.centerChunkZ());
        });
    }
}
