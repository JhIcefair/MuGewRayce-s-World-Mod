package com.mugewrayce.sizeadjuster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mugewrayce.sizeadjuster.entity.PurificationFormationEntity;
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
 * 净化魔法阵（法杖施法）渲染：贴图同净化水晶法阵（purification_circle.png），
 * 半透明 + 自发光（entityTranslucentEmissive），发光亮度 50%（同净化水晶法阵）。
 * - 单体/群体（水平）：地面水平方形，边长 size（单 2 / 群 6）。
 * - 防御（竖立）：竖直方形，边长 size（随身高），旋转朝向玩家视角（跟随视角）。
 */
public class PurificationFormationRenderer
        extends EntityRenderer<PurificationFormationEntity, PurificationFormationRenderer.State> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "mugewrayce-mod", "textures/entity/purification_circle.png");

    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);

    public PurificationFormationRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** 渲染状态：阵法类型 / 边长 / 朝向（防御阵用：随视角上下左右转动）。 */
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
    public void extractRenderState(PurificationFormationEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.formationType = entity.getFormationType();
        state.size = entity.getSize();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public boolean affectedByCulling(PurificationFormationEntity entity) {
        return false; // 阵法大小/形状随类型变化，关闭剔除避免远处/侧面不可见
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        if (state.formationType == PurificationFormationEntity.TYPE_DEFENSE
                || state.formationType == PurificationFormationEntity.TYPE_ICEFAIR_SHIELD) {
            // 防御阵 / icefair 自动防御阵：竖立竖直平面，绕 Y 旋转朝向来源方向
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
            poseStack.scale(state.size, state.size, 1.0F);
            collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
                // XY 平面竖立四边形，法线 -Z（朝向玩家一侧）
                vertex(consumer, pose, state.lightCoords, -0.5F, 0.5F, 0.0F, 0.0F, 0.0F);
                vertex(consumer, pose, state.lightCoords, 0.5F, 0.5F, 0.0F, 1.0F, 0.0F);
                vertex(consumer, pose, state.lightCoords, 0.5F, -0.5F, 0.0F, 1.0F, 1.0F);
                vertex(consumer, pose, state.lightCoords, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F);
            });
        } else {
            // 地面水平方形（法线 +Y），同净化水晶法阵
            poseStack.scale(state.size, state.size, state.size);
            collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
                vertex(consumer, pose, state.lightCoords, -0.5F, 0.0F, 0.5F, 0.0F, 1.0F);
                vertex(consumer, pose, state.lightCoords, 0.5F, 0.0F, 0.5F, 1.0F, 1.0F);
                vertex(consumer, pose, state.lightCoords, 0.5F, 0.0F, -0.5F, 1.0F, 0.0F);
                vertex(consumer, pose, state.lightCoords, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F);
            });
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                               float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                // 0xFF808080：发光亮度 50%（与净化水晶法阵一致）
                .setColor(0xFF808080)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
