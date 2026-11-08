package org.kylepitcock.legacyconnector.vb1_7_3.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow;

/** Beta's thrown-item sprite or a standalone, camera-facing projectile PNG. */
public final class ThrownRendererB173 extends EntityRenderer<LegacyB173Arrow> {
    private final net.minecraft.client.renderer.entity.ItemRenderer items;
    public ThrownRendererB173(EntityRendererProvider.Context context) { super(context);items=context.getItemRenderer(); }
    @Override public ResourceLocation getTextureLocation(LegacyB173Arrow entity) { return entity.texture(); }
    @Override public void render(LegacyB173Arrow entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light) {
        poses.pushPose();
        poses.mulPose(entityRenderDispatcher.cameraOrientation());
        if(!entity.sprite().isEmpty()) {
            items.renderStatic(entity.sprite(),net.minecraft.world.item.ItemDisplayContext.GROUND,light,OverlayTexture.NO_OVERLAY,
                    poses,buffers,entity.level(),entity.getId());
        } else {
            VertexConsumer vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(entity.texture()));
            vertex(vertices,poses,-0.5F,-0.25F,0,0,light);
            vertex(vertices,poses,0.5F,-0.25F,0,1,light);
            vertex(vertices,poses,0.5F,0.75F,1,1,light);
            vertex(vertices,poses,-0.5F,0.75F,1,0,light);
        }
        poses.popPose();
        super.render(entity,yaw,partial,poses,buffers,light);
    }
    private static void vertex(VertexConsumer out,PoseStack poses,float x,float y,float u,float v,int light) {
        out.addVertex(poses.last(),x,y,0).setColor(255,255,255,255).setUv(u,v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(poses.last(),0,0,1);
    }
}
