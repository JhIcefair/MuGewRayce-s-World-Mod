package com.mugewrayce.sizeadjuster.client;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.container.BaishanBagMenu;
import com.mugewrayce.sizeadjuster.entity.ModEntities;
import com.mugewrayce.sizeadjuster.payload.RoleStatusPayload;
import com.mugewrayce.sizeadjuster.payload.SizeStatusPayload;
import com.mugewrayce.sizeadjuster.switchitem.SwitchItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ChestMenu;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端入口：注册切换键位（默认 I 键，可在控制设置中修改）。
 */
public class MuGewRayceModClient implements ClientModInitializer {

    public static KeyMapping switchKey;

    @Override
    public void onInitializeClient() {
        switchKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mugewrayce.switch",
                GLFW.GLFW_KEY_I,
                new KeyMapping.Category(Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "mugewrayce"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (switchKey.consumeClick()) {
                if (client.player != null && SwitchItems.isSwitchable(client.player.getMainHandItem())) {
                    client.setScreen(new SwitchScreen());
                }
            }
        });

        // 净化魔法球 / 净化法阵 / 净化魔法阵（法杖施法）渲染器
        EntityRendererRegistry.register(ModEntities.PURIFICATION_ORB, PurificationOrbRenderer::new);
        EntityRendererRegistry.register(ModEntities.PURIFICATION_CIRCLE, PurificationCircleRenderer::new);
        EntityRendererRegistry.register(ModEntities.PURIFICATION_FORMATION, PurificationFormationRenderer::new);

        // 柃之册尺寸 GUI：接收服务端回发的当前属性状态（更新已打开的尺寸 GUI 左侧面板）
        ClientPlayNetworking.registerGlobalReceiver(SizeStatusPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (Minecraft.getInstance().screen instanceof SizeAdjustScreen sizeScreen) {
                        sizeScreen.setStatus(payload.lines());
                    }
                }));

        // 角色切换 GUI：接收服务端回发的当前角色编号（刷新已打开的角色切换 GUI）
        ClientPlayNetworking.registerGlobalReceiver(RoleStatusPayload.TYPE, (payload, context) ->
                context.client().execute(() -> RoleSwitchScreen.onRoleStatus(payload.role())));

        // 百杉包 GUI（依赖 fabric-menu-api-v1 的 classtweaker 使 register/ScreenConstructor 公开）
        MenuScreens.register(BaishanBagMenu.TYPE,
                (MenuScreens.ScreenConstructor<ChestMenu, BagScreen>) (menu, inventory, title) ->
                        new BagScreen(menu, inventory, title));

        // 预注册百杉包 GUI 贴图：客户端完全启动后同步加载
        // （注意：onInitializeClient 在 Minecraft.<init> 期间执行，getTextureManager() 尚为 null）
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                client.getTextureManager()
                        .registerAndLoad(BagScreen.TEXTURE, new SimpleTexture(BagScreen.TEXTURE)));
    }
}
