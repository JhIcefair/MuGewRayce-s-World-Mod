package com.mugewrayce.sizeadjuster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mugewrayce.sizeadjuster.entity.PurificationCircleEntity;
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
 * 净化法阵渲染：地面水平方形贴图（法阵跟随玩家脚下），边长 2 格，
 * 贴图 textures/entity/purification_circle.png（ls\12.png，原 O12 隐藏贴图）。
 * 半透明 + 自发光（entityTranslucentEmissive）：贴图为半透明材质，
 * 使用正确的半透明混合管线，并全亮度渲染实现发光效果。
 */
public class PurificationCircleRenderer extends EntityRenderer<PurificationCircleEntity, EntityRenderState> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "mugewrayce-mod", "textures/entity/purification_circle.png");

    /** 半透明 + 自发光：正确处理半透明混合，并全亮度渲染（发光）。 */
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);

    public PurificationCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(2.0F, 2.0F, 2.0F);
        // 地面水平四边形（法线朝上 +Y；顶点按从上方看逆时针顺序）
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
            vertex(consumer, pose, state.lightCoords, -0.5F, 0.0F, 0.5F, 0.0F, 1.0F);
            vertex(consumer, pose, state.lightCoords, 0.5F, 0.0F, 0.5F, 1.0F, 1.0F);
            vertex(consumer, pose, state.lightCoords, 0.5F, 0.0F, -0.5F, 1.0F, 0.0F);
            vertex(consumer, pose, state.lightCoords, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                               float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                // 0xFF808080：发光亮度降为 50%（RGB 各乘 0.5，alpha 不变）
                .setColor(0xFF808080)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
