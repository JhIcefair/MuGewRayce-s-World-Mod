package com.mugewrayce.sizeadjuster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mugewrayce.sizeadjuster.entity.EquipoiseFormationEntity;
import com.mugewrayce.sizeadjuster.magic.MagicPalette;
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
 * 魔法阵渲染：净化系与 W14 魔法体系共用同一套“平面四边形 + 贴图”实现。
 *
 * <ul>
 *   <li>净化阵：{@code equipoise_circle.png}，0xFF808080（自发光 50%）；</li>
 *   <li>W14 三阵：{@code magic_formation.png}（ls\40.png），按阵类型染色，
 *       RGB 再乘 0.5（半透明 + 自发光 50%）；</li>
 *   <li>攻击/治疗阵贴地，防御阵竖立并随视角；三阵共用本渲染器。</li>
 * </ul>
 */
public class EquipoiseFormationRenderer
        extends EntityRenderer<EquipoiseFormationEntity, EquipoiseFormationRenderer.State> {

    private static final Identifier PURIFICATION_TEXTURE = Identifier.fromNamespaceAndPath(
            "mugewrayce-mod", "textures/entity/equipoise_circle.png");
    private static final Identifier MAGIC_TEXTURE = Identifier.fromNamespaceAndPath(
            "mugewrayce-mod", "textures/entity/magic_formation.png");

    private static final RenderType PURIFICATION_RENDER_TYPE =
            RenderTypes.entityTranslucentEmissive(PURIFICATION_TEXTURE);
    private static final RenderType MAGIC_RENDER_TYPE =
            RenderTypes.entityTranslucentEmissive(MAGIC_TEXTURE);

    public EquipoiseFormationRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** 渲染状态：阵法类型 / 边长 / 朝向。 */
    public static class State extends EntityRenderState {
        public int formationType;
        public float size;
        public float yRot;
        public float xRot;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EquipoiseFormationEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.formationType = entity.getFormationType();
        state.size = entity.getSize();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public boolean affectedByCulling(EquipoiseFormationEntity entity) {
        return false;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        boolean magic = EquipoiseFormationEntity.isMagicType(state.formationType);
        boolean vertical = state.formationType == EquipoiseFormationEntity.TYPE_DEFENSE
                || state.formationType == EquipoiseFormationEntity.TYPE_ICEFAIR_SHIELD
                || state.formationType == EquipoiseFormationEntity.TYPE_MAGIC_DEFENSE;
        RenderType renderType = magic ? MAGIC_RENDER_TYPE : PURIFICATION_RENDER_TYPE;
        int color = magic
                ? MagicPalette.dim(MagicPalette.colorForFormation(state.formationType), 0.5F)
                : 0xFF808080;

        poseStack.pushPose();
        if (vertical) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
            poseStack.scale(state.size, state.size, 1.0F);
            collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
                vertex(consumer, pose, state.lightCoords, color, -0.5F, 0.5F, 0.0F, 0.0F, 0.0F);
                vertex(consumer, pose, state.lightCoords, color, 0.5F, 0.5F, 0.0F, 1.0F, 0.0F);
                vertex(consumer, pose, state.lightCoords, color, 0.5F, -0.5F, 0.0F, 1.0F, 1.0F);
                vertex(consumer, pose, state.lightCoords, color, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F);
            });
        } else {
            poseStack.scale(state.size, state.size, state.size);
            collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
                vertex(consumer, pose, state.lightCoords, color, -0.5F, 0.0F, 0.5F, 0.0F, 1.0F);
                vertex(consumer, pose, state.lightCoords, color, 0.5F, 0.0F, 0.5F, 1.0F, 1.0F);
                vertex(consumer, pose, state.lightCoords, color, 0.5F, 0.0F, -0.5F, 1.0F, 0.0F);
                vertex(consumer, pose, state.lightCoords, color, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F);
            });
        }
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
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
