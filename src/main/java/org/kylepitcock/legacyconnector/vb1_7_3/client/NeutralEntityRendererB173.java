package org.kylepitcock.legacyconnector.vb1_7_3.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Native drawing adapter; shape, UVs and camera-facing transforms remain original callbacks. */
public final class NeutralEntityRendererB173<T extends Entity & B173EntityHost> extends EntityRenderer<T> {
    private static final java.util.Set<Class<?>> REPORTED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public NeutralEntityRendererB173(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(T entity){return ResourceLocation.withDefaultNamespace("missingno");}
    @Override public void render(T entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        try(var readOnly=EntityViewsB173.readOnly()){
        var view=entity.prepareRenderView(partial);if(view==null)return;
        var info=RenderersB173.info(view.getClass());if(info==null)return;
        shadowRadius=info.renderer().mcc$shadowRadius();
        RenderContext.beginWorld(pose,buffers,light,OverlayTexture.NO_OVERLAY);pose.pushPose();
        try{
            EntityRenderingB173.drawPrepared(entity,view,partial,(part,texture)->{
                NativeRendererExtrasB173.applyStyle(RendererContextB173.style());int color=RendererContextB173.argb();
                RenderContext.color(((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,((color>>>24)&255)/255F);
                RenderContext.bindTexture(texture);ModelPartsB173.draw(part);
            },NativeRendererExtrasB173.create(entity,pose,buffers));
        }catch(RuntimeException|LinkageError error){if(REPORTED.add(info.renderer().getClass()))org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Render").error("Original Beta entity renderer failed for {}",entity.getType(),error);}
        finally{pose.popPose();RenderContext.end();}
        super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
}
