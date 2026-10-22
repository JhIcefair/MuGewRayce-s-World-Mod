package com.mugewrayce.sizeadjuster;

import com.mugewrayce.sizeadjuster.command.SizeCommand;
import com.mugewrayce.sizeadjuster.entity.IcefairShields;
import com.mugewrayce.sizeadjuster.entity.ModEntities;
import com.mugewrayce.sizeadjuster.entity.PurificationFormationEntity;
import com.mugewrayce.sizeadjuster.item.ModCreativeTabs;
import com.mugewrayce.sizeadjuster.item.ModItems;
import com.mugewrayce.sizeadjuster.payload.RoleActionPayload;
import com.mugewrayce.sizeadjuster.payload.RoleStatusPayload;
import com.mugewrayce.sizeadjuster.payload.SizeActionPayload;
import com.mugewrayce.sizeadjuster.payload.SizeStatusPayload;
import com.mugewrayce.sizeadjuster.payload.StaffModePayload;
import com.mugewrayce.sizeadjuster.switchitem.SwitchItemPayload;
import com.mugewrayce.sizeadjuster.util.PlayerRoles;
import com.mugewrayce.sizeadjuster.util.RoleStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SizeAdjusterMod implements ModInitializer {
    public static final String MOD_ID = "mugewrayce-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("mugewrayce mod initializing for Minecraft 26.1...");

        ModItems.register();
        ModEntities.register();
        ModCreativeTabs.register();
        SwitchItemPayload.register();
        SizeActionPayload.register();
        SizeStatusPayload.register();
        StaffModePayload.register();
        RoleActionPayload.register();
        RoleStatusPayload.register();

        // 伤害拦截（按玩家当前状态逐层判定）：
        // 1) 手动防御魔法阵（法杖·防御）：生效期间抵挡一切伤害（投射物由阵法实体反弹）；
        // 2) 特殊角色系统：
        //    - 强制处决/绝对死亡伤害（/kill、虚空）：任何情况下放行（可被 /kill 杀）；
        //    - 豁免伤害（火焰/冰冻/溺水/凋零效果）：排除在免疫范围之外，会生效（但非"攻击"，
        //      不触发 icefair 自动防御魔法阵）；
        //    - 魔法体（魔法体角色及一切特殊角色）：物理免疫——其余一切物理/环境伤害免疫
        //      （纯魔法体的远程投射物另由 mixin 直接穿过，见 mixin/MagicBodyPlayerProjectileMixin）；
        //    - icefair：受到"攻击"（近战/投射物等，DamageSource 带有直接攻击实体）命中时，
        //      优先执行自动防御魔法阵逻辑——在受伤方向召唤小的净化魔法防御阵并抵挡该方向伤害。
        //    触发来源包括：法杖/归零魔法球等魔法伤害、箭、三叉戟、雪球、鸡蛋、末影珍珠、
        //      烈焰弹/火球/小火球、凋灵之首、风弹、羊驼唾沫、潜影弹、监守者远程/近战、
        //      玩家及几乎所有生物（含蜜蜂、山羊）的近战、戳刺/冲锋等攻击。
        //    注意：ServerLivingEntityEvents.ALLOW_DAMAGE 语义为
        //      false=取消/抵挡伤害，true=允许伤害生效。
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof Player player) {
                // 手动防御魔法阵：像盾牌一样抵御一切伤害
                if (PurificationFormationEntity.isDefenseActive(player.getUUID())) {
                    return false;
                }
                int role = PlayerRoles.getRole(player);
                if (role != PlayerRoles.ROLE_DEFAULT) {
                    // 强制处决/绝对死亡（/kill、虚空）：放行（可被杀死）
                    if (PlayerRoles.isForcedKill(source)) {
                        return true;
                    }
                    // 豁免伤害（火焰/冰冻/溺水/凋零效果）：排除在免疫范围外，会生效，
                    // 但不触发自动防御魔法阵（非"攻击"，无攻击实体）
                    if (PlayerRoles.isExemptDamage(source)) {
                        return true;
                    }
                    // 魔法伤害（法杖魔法球/归零魔法球、群体伤害魔法阵等）
                    if (PlayerRoles.isMagicDamage(source)) {
                        if (PlayerRoles.isIcefair(role)
                                && entity.level() instanceof ServerLevel serverLevel) {
                            return IcefairShields.onAttackDamage(serverLevel, player, source);
                        }
                        return true; // 纯魔法体：放行魔法伤害
                    }
                    // 其余非魔法伤害：icefair 受"攻击"（近战/投射物，带直接攻击实体）命中时触发自动防御
                    if (PlayerRoles.isIcefair(role)
                            && entity.level() instanceof ServerLevel serverLevel
                            && source.getDirectEntity() != null) {
                        return IcefairShields.onAttackDamage(serverLevel, player, source);
                    }
                    // 其余物理/环境伤害（摔落、火焰环境、窒息、药水效果等）：免疫
                    return false;
                }
            }
            return true;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                SizeCommand.register(dispatcher));

        // 角色持久化：服务器启动（进入/重进世界）载入，停止时再写入，保证退出重进仍保留
        ServerLifecycleEvents.SERVER_STARTED.register(RoleStore::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(RoleStore::save);

        LOGGER.info("mugewrayce mod initialized.");
    }
}
