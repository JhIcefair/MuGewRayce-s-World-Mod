package com.mugewrayce.sizeadjuster.util;

import com.mugewrayce.sizeadjuster.entity.IcefairShields;
import com.mugewrayce.sizeadjuster.entity.EquipoiseFormationEntity;
import com.mugewrayce.sizeadjuster.item.EquipoiseCrystalItem;
import com.mugewrayce.sizeadjuster.item.EquipoiseStaffItem;
import com.mugewrayce.sizeadjuster.item.StaffCastModes;
import com.mugewrayce.sizeadjuster.magic.mana.ManaDetectorRequests;
import com.mugewrayce.sizeadjuster.magic.mana.ManaService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * 玩家断开连接时的状态清理（W01 4.4）。
 *
 * 这些按玩家 UUID 记录的静态表不会随玩家对象回收而释放；若不清理，玩家退出后
 * 表项会残留到下一次进入世界，可能让新会话误用旧实体/旧施法模式。
 * 这里在 DISCONNECT 时统一丢弃实体并移除表项：
 * - 法杖：充能中的魔法球、当前召唤的阵法；
 * - 净化水晶：脚下的净化法阵；
 * - icefair：自动防御魔法阵列表；
 * - 手动防御阵：ACTIVE_DEFENSE；
 * - 法杖施法模式：MODES。
 *
 * 客户端侧的静态 GUI 状态（角色编号/施法模式）在 MuGewRayceModClient 的
 * ClientPlayConnectionEvents.DISCONNECT 中重置。
 */
public final class PlayerStateCleanup {

    private PlayerStateCleanup() {
    }

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (player != null) {
                cleanup(player);
            }
        });
    }

    /** 清理该玩家在服务端静态表中的全部瞬态状态。 */
    public static void cleanup(ServerPlayer player) {
        UUID id = player.getUUID();
        EquipoiseStaffItem.clearPlayer(id);               // 充能魔法球 / 阵法 / 持续线式
        EquipoiseCrystalItem.clearPlayer(id);             // 净化法阵
        IcefairShields.clearShields(id);                  // icefair 自动防御阵列表
        EquipoiseFormationEntity.deactivateDefense(id); // 手动防御阵（若仍在生效）
        StaffCastModes.clear(id);                         // 施法模式
        ManaDetectorRequests.clear(id);                   // 探测器待回包请求（W12）
        ManaService.clearPlayer(id);                      // 施法轮流步数 + 区块不足锁定（W13）
    }
}
