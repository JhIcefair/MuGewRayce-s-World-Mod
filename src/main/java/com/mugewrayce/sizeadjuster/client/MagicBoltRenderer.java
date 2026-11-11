package com.mugewrayce.sizeadjuster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mugewrayce.sizeadjuster.entity.MagicBoltEntity;
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
 * 魔法弹渲染（W19 由空实现改为实体球）。
 *
 * <ul>
 *   <li>{@link MagicBoltEntity#KIND_POINT}：面向相机的平面贴图球（billboard），
 *       贴图 {@code textures/entity/magic_orb.png}（由 {@code magic_point.png} 字节复制），
 *       顶点颜色用 {@link MagicBoltEntity#getColor()} 染色；飞行缩放 0.4，蓄力悬停略放大到 0.5；</li>
 *   <li>{@link MagicBoltEntity#KIND_LINE}：不画几何，仍由 {@code magic_line} 粒子拖尾表现。</li>
 * </ul>
 */
public class MagicBoltRenderer extends EntityRenderer<MagicBoltEntity, MagicBoltRenderer.State> {

    private static final Identifier ORB_TEXTURE = Identifier.fromNamespaceAndPath(
            "mugewrayce-mod", "textures/entity/magic_orb.png");

    private static final RenderType ORB_RENDER_TYPE = RenderTypes.entityCutout(ORB_TEXTURE);

    /** 飞行时的球尺寸。 */
    private static final float FLY_SCALE = 0.4F;

    /** 蓄力悬停时的球尺寸（略微放大作为“充能”提示）。 */
    private static final float CHARGE_SCALE = 0.5F;

    public MagicBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** 渲染状态：颜色 / 种类 / 是否蓄力。 */
    public static class State extends EntityRenderState {
        public int color;
        public int kind;
        public boolean charging;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean affectedByCulling(MagicBoltEntity entity) {
        return false;
    }

    @Override
    public void extractRenderState(MagicBoltEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.color = entity.getColor();
        state.kind = entity.getKind();
        state.charging = entity.isCharging();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.kind != MagicBoltEntity.KIND_POINT) {
            // 线式：粒子拖尾负责表现，不提交几何
            super.submit(state, poseStack, collector, camera);
            return;
        }
        float scale = state.charging ? CHARGE_SCALE : FLY_SCALE;
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(camera.orientation);
        collector.submitCustomGeometry(poseStack, ORB_RENDER_TYPE, (pose, consumer) -> {
            vertex(consumer, pose, state.lightCoords, state.color, -0.5F, -0.5F, 0.0F, 0.0F, 0.0F);
            vertex(consumer, pose, state.lightCoords, state.color, 0.5F, -0.5F, 0.0F, 1.0F, 0.0F);
            vertex(consumer, pose, state.lightCoords, state.color, 0.5F, 0.5F, 0.0F, 1.0F, 1.0F);
            vertex(consumer, pose, state.lightCoords, state.color, -0.5F, 0.5F, 0.0F, 0.0F, 1.0F);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light, int color,
                               float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}
