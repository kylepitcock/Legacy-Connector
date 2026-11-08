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
            var extras=new RendererContextB173.Extras(){
                @Override public void item(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity living,
                        legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack,Matrix4f transform,RendererContextB173.Style style){
                    var modern=StacksB173.toModern(stack);if(modern.isEmpty())return;
                    applyStyle(style);pose.pushPose();
                    try {
                        pose.mulPose(transform);
                        var nativeLiving=living!=null&&living.mcc$modern instanceof net.minecraft.world.entity.LivingEntity l?l:null;
                        Minecraft.getInstance().getItemRenderer().renderStatic(nativeLiving,modern,ItemDisplayContext.NONE,
                                false,pose,buffers,entity.level(),RenderContext.vertexLight(),RenderContext.overlay(),entity.getId());
                    }finally{pose.popPose();}
                }
                @Override public void text(String text,float x,float y,int argb,boolean shadow,Matrix4f transform,RendererContextB173.Style style){
                    applyStyle(style);pose.pushPose();
                    try {
                        pose.mulPose(transform);
                        Minecraft.getInstance().font.drawInBatch(text,x,y,(argb&0xFC000000)==0?argb|0xFF000000:argb,shadow,pose.last().pose(),buffers,
                                style.depthTest()?Font.DisplayMode.NORMAL:Font.DisplayMode.SEE_THROUGH,0,RenderContext.vertexLight());
                    }finally{pose.popPose();}
                }
                @Override public void quad(java.util.List<RendererContextB173.Vertex> vertices,Matrix4f transform,
                        ResourceLocation texture,RendererContextB173.Style style){
                    if(vertices.size()!=4)throw new IllegalArgumentException("Beta quad needs four vertices");
                    applyStyle(style);RenderContext.bindTexture(texture);pose.pushPose();
                    try {
                        pose.mulPose(transform);double[][] corners=new double[4][8];
                        for(int i=0;i<4;i++){
                            var vertex=vertices.get(i);corners[i]=new double[]{vertex.x(),vertex.y(),vertex.z(),vertex.u(),vertex.v(),vertex.argb(),RenderContext.vertexLight(),1};
                        }
                        var normal=vertices.getFirst();RenderContext.quad(corners,normal.nx(),normal.ny(),normal.nz());
                    }finally{pose.popPose();}
                }
                @Override public int width(String text){return Minecraft.getInstance().font.width(text);}
                @Override public legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity camera(){
                    var camera=Minecraft.getInstance().getCameraEntity();
                    var view=EntityViewsB173.wrap(camera);return view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity living?living:null;
                }
                @Override public float yaw(){return Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();}
                @Override public float pitch(){return Minecraft.getInstance().gameRenderer.getMainCamera().getXRot();}
            };
            AnimalRenderingB173.draw(entity,partial,(part,texture)->{
                applyStyle(RendererContextB173.style());int color=RendererContextB173.argb();
                RenderContext.color(((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,((color>>>24)&255)/255F);
                RenderContext.bindTexture(texture);ModelPartsB173.draw(part);
            },extras);
        }catch(RuntimeException|LinkageError e){
            if(REPORTED.add(info.renderer().getClass()))org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Render").error("Original Beta renderer failed for {}",entity.getType(),e);
        }finally{pose.popPose();RenderContext.end();}
        super.render(entity,yaw,partial,pose,buffers,light);
    }
    private static void applyStyle(RendererContextB173.Style style){
        RenderContext.setTexture2d(style.texture2d());RenderContext.setLighting(style.lighting());
        RenderContext.setDepthTest(style.depthTest());RenderContext.setDepthWrite(style.depthWrite());RenderContext.setDepthFunc(style.depthFunc());
        RenderContext.setBlend(style.blend());RenderContext.setBlendFunc(style.blendSrc(),style.blendDst());
        RenderContext.matrixMode(5890);RenderContext.loadMatrix(style.textureMatrix());RenderContext.matrixMode(5888);
    }

}
