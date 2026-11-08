package org.kylepitcock.legacyconnector.vb1_7_3.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Native render entry point; transforms, model selection and animation remain original Beta callbacks. */
public final class AnimalRendererB173 extends EntityRenderer<LegacyB173Animal> {
    private static final java.util.Set<Class<?>> REPORTED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public AnimalRendererB173(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(LegacyB173Animal entity){var info=RenderersB173.info(entity.legacy().getClass());return MobTexturesB173.resolve(info==null?null:info.mod(),entity.legacy().mcc$texture());}
    /** Shared with headless actual-mod proofs; the sink receives the same transformed textured parts as the client. */
    public static void draw(LegacyB173Animal entity,float partial,BiConsumer<ModelGeometryB173.Draw,ResourceLocation> sink){
        AnimalRenderingB173.draw(entity,partial,sink);
    }
    @Override public void render(LegacyB173Animal entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        var info=RenderersB173.info(entity.legacy().getClass());if(info==null)return;
        shadowRadius=info.renderer().mcc$shadowRadius();
        RenderContext.beginWorld(pose,buffers,light,LivingEntityRenderer.getOverlayCoords(entity,0));
        pose.pushPose();
        try {
            var extras=NativeRendererExtrasB173.create(entity,pose,buffers);
            AnimalRenderingB173.draw(entity,partial,(part,texture)->{
                NativeRendererExtrasB173.applyStyle(RendererContextB173.style());int color=RendererContextB173.argb();
                RenderContext.color(((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,((color>>>24)&255)/255F);
                RenderContext.bindTexture(texture);ModelPartsB173.draw(part);
            },extras);
        }catch(RuntimeException|LinkageError e){
            if(REPORTED.add(info.renderer().getClass()))org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Render").error("Original Beta renderer failed for {}",entity.getType(),e);
        }finally{pose.popPose();RenderContext.end();}
        super.render(entity,yaw,partial,pose,buffers,light);
    }
}
