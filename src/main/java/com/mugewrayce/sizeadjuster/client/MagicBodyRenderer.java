package com.mugewrayce.sizeadjuster.client;

import com.mugewrayce.sizeadjuster.entity.AbstractMagicBodyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * 两种魔法体共用的玩家模型渲染器（贴图 ls\19.png）。
 *
 * <p>26.1 的 {@code PlayerModel} 绑定 {@code AvatarRenderState}，不能直接给普通 Mob 用；
 * 这里用同一套玩家模型层 {@link ModelLayers#PLAYER} 构建 {@link HumanoidModel}，
 * 外观仍是玩家模型，皮肤用 {@code textures/entity/lifeless_magic_body.png}。</p>
 */
public class MagicBodyRenderer extends HumanoidMobRenderer<AbstractMagicBodyEntity, HumanoidRenderState,
        HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "mugewrayce-mod", "textures/entity/lifeless_magic_body.png");

    public MagicBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
