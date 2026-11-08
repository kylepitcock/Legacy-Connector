package org.kylepitcock.legacyconnector.vb1_7_3.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.vb1_7_3.*;
/** Native sinks shared by original model and flat-quad renderer families. */
final class NativeRendererExtrasB173 {
    private NativeRendererExtrasB173(){}
    static RendererContextB173.Extras create(net.minecraft.world.entity.Entity entity,PoseStack pose,MultiBufferSource buffers){
        return new RendererContextB173.Extras(){
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
    }
    static void applyStyle(RendererContextB173.Style style){
        RenderContext.setTexture2d(style.texture2d());RenderContext.setLighting(style.lighting());
        RenderContext.setDepthTest(style.depthTest());RenderContext.setDepthWrite(style.depthWrite());RenderContext.setDepthFunc(style.depthFunc());
        RenderContext.setBlend(style.blend());RenderContext.setBlendFunc(style.blendSrc(),style.blendDst());
        RenderContext.matrixMode(5890);RenderContext.loadMatrix(style.textureMatrix());RenderContext.matrixMode(5888);
    }

}
