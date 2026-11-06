package com.mugewrayce.sizeadjuster.client;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.clientbridge.ScreenOpener;
import com.mugewrayce.sizeadjuster.clientbridge.ScreenOpenerHolder;
import com.mugewrayce.sizeadjuster.client.network.W04TestClient;
import com.mugewrayce.sizeadjuster.container.BaishanBagMenu;
import com.mugewrayce.sizeadjuster.entity.ModEntities;
import com.mugewrayce.sizeadjuster.payload.RoleStatusPayload;
import com.mugewrayce.sizeadjuster.payload.SizeStatusPayload;
import com.mugewrayce.sizeadjuster.switchitem.EquipoiseSwitchMatrix;
import com.mugewrayce.sizeadjuster.switchitem.EquipoiseSwitchPayload;
import com.mugewrayce.sizeadjuster.switchitem.SwitchItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端入口。
 *
 * <p>净化系切换改为 4 个方向键（默认 ↑↓←→，可在控制设置中修改），
 * 客户端只发送方向意图，由服务端按当前主手重算目标；旧的 i 键切换 GUI 已删除。</p>
 *
 * <p>{@code key.mugewrayce.settings}（i 键）保留给 W16 的法杖/水晶/魔法书设置 GUI，
 * 本任务不注册其按键行为。</p>
 */
public class MuGewRayceModClient implements ClientModInitializer {

    /** 方向键：下标与 {@link EquipoiseSwitchMatrix} 的方向码一致（0=↑、1=↓、2=←、3=→）。 */
    private static final KeyMapping[] SWITCH_KEYS = new KeyMapping[EquipoiseSwitchMatrix.DIRECTION_COUNT];

    @Override
    public void onInitializeClient() {
        // 公共物品类 → 客户端 GUI 的桥接实现（W01 4.2；服务端/未初始化时为空实现）
        ScreenOpenerHolder.set(new ScreenOpener() {
            @Override
            public void openBookOfLing() {
                Minecraft.getInstance().setScreen(new BookOfLingScreen());
            }

            @Override
            public void openRoleSwitch() {
                Minecraft.getInstance().setScreen(new RoleSwitchScreen());
            }
        });

        // 4 个方向键：↑ 法杖、↓ 圣剑、← 圣弓、→ 归零
        KeyMapping.Category category = new KeyMapping.Category(
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "mugewrayce"));
        SWITCH_KEYS[EquipoiseSwitchMatrix.DIR_UP] =
                registerSwitchKey("key.mugewrayce.switch_staff", GLFW.GLFW_KEY_UP, category);
        SWITCH_KEYS[EquipoiseSwitchMatrix.DIR_DOWN] =
                registerSwitchKey("key.mugewrayce.switch_holy_sword", GLFW.GLFW_KEY_DOWN, category);
        SWITCH_KEYS[EquipoiseSwitchMatrix.DIR_LEFT] =
                registerSwitchKey("key.mugewrayce.switch_holy_bow", GLFW.GLFW_KEY_LEFT, category);
        SWITCH_KEYS[EquipoiseSwitchMatrix.DIR_RIGHT] =
                registerSwitchKey("key.mugewrayce.switch_zero_reset", GLFW.GLFW_KEY_RIGHT, category);

        // 2.0.0 网络骨架（W04，临时）：S2C 状态接收 + 客户端测试命令（W23 清理）
        W04TestClient.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (int direction = 0; direction < SWITCH_KEYS.length; direction++) {
                while (SWITCH_KEYS[direction].consumeClick()) {
                    if (client.player != null && SwitchItems.isSwitchable(client.player.getMainHandItem())) {
                        ClientPlayNetworking.send(new EquipoiseSwitchPayload(direction));
                    }
                }
            }
        });

        // 断开连接时重置客户端静态 GUI 状态（W01 4.4，避免跨世界/跨服务器残留）
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RoleSwitchScreen.resetClientState());

        // 净化魔法球 / 净化法阵 / 净化魔法阵（法杖施法）渲染器
        EntityRendererRegistry.register(ModEntities.EQUIPOISE_ORB, EquipoiseOrbRenderer::new);
        EntityRendererRegistry.register(ModEntities.EQUIPOISE_CIRCLE, EquipoiseCircleRenderer::new);
        EntityRendererRegistry.register(ModEntities.EQUIPOISE_FORMATION, EquipoiseFormationRenderer::new);

        // 两种魔法体共用玩家模型渲染器（贴图 ls\19.png）
        EntityRendererRegistry.register(ModEntities.LIFELESS_MAGIC_BODY, MagicBodyRenderer::new);
        EntityRendererRegistry.register(ModEntities.TRIAL_MAGIC_BODY, MagicBodyRenderer::new);

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
                (MenuScreens.ScreenConstructor<BaishanBagMenu, BagScreen>) (menu, inventory, title) ->
                        new BagScreen(menu, inventory, title));

        // 预注册百杉包 GUI 贴图：客户端完全启动后同步加载
        // （注意：onInitializeClient 在 Minecraft.<init> 期间执行，getTextureManager() 尚为 null）
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                client.getTextureManager()
                        .registerAndLoad(BagScreen.TEXTURE, new SimpleTexture(BagScreen.TEXTURE)));
    }

    private static KeyMapping registerSwitchKey(String translationKey, int defaultKey, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(translationKey, defaultKey, category));
    }
}
