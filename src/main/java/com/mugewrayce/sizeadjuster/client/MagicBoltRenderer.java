package com.mugewrayce.sizeadjuster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mugewrayce.sizeadjuster.entity.MagicBoltEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * 魔法弹本身不渲染：外观由 {@code magic_point} / {@code magic_line} 粒子在
 * {@link MagicBoltEntity#tick()} 的客户端分支拖尾（W14c）。
 */
public class MagicBoltRenderer extends EntityRenderer<MagicBoltEntity, EntityRenderState> {

    public MagicBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public boolean affectedByCulling(MagicBoltEntity entity) {
        return false;
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        // 无几何：粒子拖尾负责表现
    }
}
