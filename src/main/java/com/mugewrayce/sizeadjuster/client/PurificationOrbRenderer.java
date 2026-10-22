package com.mugewrayce.sizeadjuster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mugewrayce.sizeadjuster.entity.PurificationOrbEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * 净化魔法球渲染：面向相机的平面贴图（billboard），尺寸 0.25（与物品掉落物一致）。
 * 按实体变体选择贴图：
 * - 法杖（VARIANT_STAFF）：textures/entity/purification_orb.png（ls\15.png）
 * - 归零（VARIANT_ZERO_RESET）：textures/entity/purification_orb_zero.png（ls\16.png）
 */
public class PurificationOrbRenderer extends EntityRenderer<PurificationOrbEntity, PurificationOrbRenderer.State> {

    private static final Identifier STAFF_TEXTURE = Identifier.fromNamespaceAndPath(
            "mugewrayce-mod", "textures/entity/purification_orb.png");

    private static final Identifier ZERO_RESET_TEXTURE = Identifier.fromNamespaceAndPath(
            "mugewrayce-mod", "textures/entity/purification_orb_zero.png");

    private static final RenderType STAFF_RENDER_TYPE = RenderTypes.entityCutout(STAFF_TEXTURE);
    private static final RenderType ZERO_RESET_RENDER_TYPE = RenderTypes.entityCutout(ZERO_RESET_TEXTURE);

    public PurificationOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** 渲染状态：携带变体，供 submit 选择贴图。 */
    public static class State extends EntityRenderState {
        public int variant;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PurificationOrbEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.variant = entity.getVariant();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(camera.orientation);
        RenderType renderType =
                state.variant == PurificationOrbEntity.VARIANT_ZERO_RESET ? ZERO_RESET_RENDER_TYPE : STAFF_RENDER_TYPE;
        collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            vertex(consumer, pose, state.lightCoords, -0.5F, -0.5F, 0.0F, 0.0F, 0.0F);
            vertex(consumer, pose, state.lightCoords, 0.5F, -0.5F, 0.0F, 1.0F, 0.0F);
            vertex(consumer, pose, state.lightCoords, 0.5F, 0.5F, 0.0F, 1.0F, 1.0F);
            vertex(consumer, pose, state.lightCoords, -0.5F, 0.5F, 0.0F, 0.0F, 1.0F);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                               float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}
