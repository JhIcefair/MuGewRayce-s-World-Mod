package com.mugewrayce.sizeadjuster.magic.cast;

import com.mugewrayce.sizeadjuster.component.MagicBookData;
import com.mugewrayce.sizeadjuster.magic.MagicNumbers;
import com.mugewrayce.sizeadjuster.magic.mana.ManaService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 魔法书两技能的实际结算（W14b）。
 *
 * <p>只由 {@code magic_book_cast} 包在服务端调用；重新校验技能/范围/主手后，
 * 先 {@link ManaService#tryConsume} 再执行。失败无副作用。</p>
 */
public final class BookCaster {

    private BookCaster() {
    }

    public static boolean cast(ServerPlayer player, MagicBookData data) {
        if (data == null || data.skill() == null || data.skill().isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("message.mugewrayce-mod.no_skill_selected"), true);
            return false;
        }
        if (!MagicBookData.isValidSkill(data.skill())
                || !MagicBookData.isValidRange(data.skill(), data.range())) {
            return false;
        }
        if (MagicBookData.SKILL_TIME_PAUSE.equals(data.skill())) {
            return castTimePause(player, data);
        }
        if (MagicBookData.SKILL_SPACE_TELEPORT.equals(data.skill())) {
            return castTeleport(player, data);
        }
        return false;
    }

    private static boolean castTimePause(ServerPlayer player, MagicBookData data) {
        if (!TimePauseZone.TIME_PAUSE_ENABLED) {
            return false;
        }
        int chunks = MagicNumbers.chunkCount(data.range());
        int cost = MagicNumbers.timePauseStartCost(chunks);
        if (!pay(player, cost)) {
            return false;
        }
        return TimePauseZone.start(player.level(), player, data.range(), data.excluded());
    }

    private static boolean castTeleport(ServerPlayer player, MagicBookData data) {
        ServerLevel level = player.level();
        int chunks = MagicNumbers.chunkCount(data.range());
        double targetX = data.targetX() + 0.5D;
        double targetY = data.targetY();
        double targetZ = data.targetZ() + 0.5D;
        double distance = player.position().distanceTo(new Vec3(targetX, targetY, targetZ));
        int cost = MagicNumbers.teleportCost(chunks, distance);
        if (!pay(player, cost)) {
            return false;
        }
        teleportEntities(level, player, data.range(), targetX, targetY, targetZ);
        return true;
    }

    private static void teleportEntities(ServerLevel level, ServerPlayer caster, int range,
                                         double targetX, double targetY, double targetZ) {
        int side = Math.max(1, Math.min(5, range));
        int startX = caster.chunkPosition().x() - (side - 1) / 2;
        int startZ = caster.chunkPosition().z() - (side - 1) / 2;
        double minX = startX * 16.0D;
        double minZ = startZ * 16.0D;
        double maxX = (startX + side) * 16.0D;
        double maxZ = (startZ + side) * 16.0D;
        double minY = level.getMinY();
        double maxY = level.getMaxY() - 1.0D;
        double y = Math.max(minY, Math.min(maxY, targetY));
        AABB area = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, area,
                entity -> entity.isAlive() && !entity.isSpectator());
        for (Entity entity : entities) {
            double dx = entity.getX() - caster.getX();
            double dz = entity.getZ() - caster.getZ();
            entity.teleportTo(targetX + dx, y, targetZ + dz);
            entity.resetFallDistance();
        }
    }

    private static boolean pay(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return true;
        }
        ManaService.Result result = ManaService.tryConsume(player, amount, false);
        if (result == ManaService.Result.INSUFFICIENT) {
            player.sendSystemMessage(
                    Component.translatable("message.mugewrayce-mod.mana_insufficient"), true);
            return false;
        }
        return true;
    }
}
