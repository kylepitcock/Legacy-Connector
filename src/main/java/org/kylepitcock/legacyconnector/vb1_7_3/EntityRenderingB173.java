package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Executes registered original nonliving renderers with the same sinks used by living models. */
public final class EntityRenderingB173 {
    private static final ResourceLocation ITEMS=ResourceLocation.fromNamespaceAndPath("legacyconnector","b173/sheets/items");
    private static final ResourceLocation TERRAIN=ResourceLocation.fromNamespaceAndPath("legacyconnector","b173/sheets/terrain");
    private EntityRenderingB173(){}
    public record Quad(List<RendererContextB173.Vertex> vertices,ResourceLocation texture){}
    public static Quad resolveQuad(List<RendererContextB173.Vertex> vertices,ResourceLocation texture){
        int group=ITEMS.equals(texture)?2:TERRAIN.equals(texture)?1:0;
        if(group==0)return new Quad(vertices,texture);
        if(vertices.size()!=4)throw new IllegalArgumentException("Legacy sheet requires quads");
        double minU=Double.POSITIVE_INFINITY,minV=minU,maxU=Double.NEGATIVE_INFINITY,maxV=maxU;
        for(var v:vertices){minU=Math.min(minU,v.u());maxU=Math.max(maxU,v.u());minV=Math.min(minV,v.v());maxV=Math.max(maxV,v.v());}
        int column=(int)Math.floor(minU*16+1E-6),row=(int)Math.floor(minV*16+1E-6);
        if(!Double.isFinite(minU+minV+maxU+maxV)||column<0||row<0||column>15||row>15||maxU*16>column+1+1E-6||maxV*16>row+1+1E-6)
            throw new IllegalArgumentException("Legacy sheet quad spans unsupported atlas cells");
        ResourceLocation sprite=AssetsB173.particleSprite(group,row*16+column);
        if(sprite==null)throw new IllegalArgumentException("Unknown legacy sheet cell "+group+":"+(row*16+column));
        final int x=column,y=row;
        return new Quad(vertices.stream().map(v->new RendererContextB173.Vertex(v.x(),v.y(),v.z(),v.u()*16-x,v.v()*16-y,v.argb(),v.nx(),v.ny(),v.nz())).toList(),sprite);
    }
    public static ResourceLocation texture(org.kylepitcock.legacyconnector.core.LegacyModRecord mod,String path){
        return switch(path){case "/gui/items.png","gui/items.png"->ITEMS;case "/terrain.png","terrain.png"->TERRAIN;default->MobTexturesB173.resolve(mod,path);};
    }
    /** Both living and nonliving original renderers may emit immediate quads from old sheets. */
    public static RendererContextB173.Extras sheets(RendererContextB173.Extras extras){
        return new RendererContextB173.Extras(){
            @Override public void quad(List<RendererContextB173.Vertex> vertices,Matrix4f transform,ResourceLocation texture,RendererContextB173.Style style){var q=resolveQuad(vertices,texture);extras.quad(q.vertices(),transform,q.texture(),style);}
            @Override public void line(RendererContextB173.Vertex start,RendererContextB173.Vertex end,Matrix4f transform,RendererContextB173.Style style){extras.line(start,end,transform,style);}
            @Override public void item(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity living,legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack,Matrix4f transform,RendererContextB173.Style style){extras.item(living,stack,transform,style);}
            @Override public void text(String text,float x,float y,int argb,boolean shadow,Matrix4f transform,RendererContextB173.Style style){extras.text(text,x,y,argb,shadow,transform,style);}
            @Override public int width(String text){return extras.width(text);}
            @Override public legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity camera(){return extras.camera();}
            @Override public float yaw(){return extras.yaw();}
            @Override public float pitch(){return extras.pitch();}
        };
    }
    public static void draw(net.minecraft.world.entity.Entity entity,B173EntityHost host,float partial,
            BiConsumer<ModelGeometryB173.Draw,ResourceLocation> sink,RendererContextB173.Extras extras){
        try(var readOnly=EntityViewsB173.readOnly()){
            var view=host.prepareRenderView(partial);if(view==null)return;
            drawPrepared(entity,view,partial,sink,extras);
        }
    }
    /** A native adapter may prepare once before choosing its renderer and drawing state. */
    public static void drawPrepared(net.minecraft.world.entity.Entity entity,legacyconnector.vb1_7_3.net.minecraft.entity.Entity view,float partial,
            BiConsumer<ModelGeometryB173.Draw,ResourceLocation> sink,RendererContextB173.Extras extras){
        if(view==null)return;
        try(var readOnly=EntityViewsB173.readOnly()){
            var info=RenderersB173.info(view.getClass());
            if(info==null)return; // Original absence of a registration is intentionally invisible.
            var routed=sheets(extras);
            synchronized(info.renderer()){
                info.renderer().mcc$camera(new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.EntityRenderer.LivingEntityCamera(extras.camera(),extras.yaw(),extras.pitch()));
                info.renderer().mcc$world(view.world);
                try(var scope=RendererContextB173.open(path->texture(info.mod(),path),sink,routed)){
                    info.renderer().render(view,0,view.standingEyeHeight,0,Mth.rotLerp(partial,entity.yRotO,entity.getYRot()),partial);
                }
            }
        }
    }
}
