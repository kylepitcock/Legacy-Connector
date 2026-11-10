package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.List;
import java.util.function.Function;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.PlayerEntityRenderer;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Graphics-free recording of original player callbacks, including their complete per-draw state. */
public final class PlayerRenderingB173 {
    private record ClientIdentity(legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity rendered,
            legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity local){}
    private static final ThreadLocal<ClientIdentity> CLIENT_IDENTITY=new ThreadLocal<>();
    @FunctionalInterface public interface Models {
        void draw(ModelGeometryB173.Draw draw,ResourceLocation texture,RendererContextB173.Style style,int argb);
    }
    private PlayerRenderingB173(){}
    /** Minecraft.player remains the local client, even when a different player's renderer is called. */
    public static void withClientIdentity(legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity rendered,
            legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity local,Runnable callback){
        var previous=CLIENT_IDENTITY.get();CLIENT_IDENTITY.set(new ClientIdentity(rendered,local));
        try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(rendered.world,rendered)){
            legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$set$player(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance(),local);
            callback.run();
        }finally{if(previous==null)CLIENT_IDENTITY.remove();else CLIENT_IDENTITY.set(previous);}
    }
    /** Reuse snapshots during nested original previews rather than syncing over pending callback edits. */
    public static PlayerViewB173 cachedClientView(net.minecraft.world.entity.player.Player player){
        var identity=CLIENT_IDENTITY.get();if(identity==null||player==null)return null;
        if(identity.local() instanceof PlayerViewB173 local&&local.mcc$modern==player)return local;
        return identity.rendered() instanceof PlayerViewB173 rendered&&rendered.mcc$modern==player?rendered:null;
    }
    public static PlayerRenderPhasesB173.Plan capture(PlayerEntityRenderer renderer,PlayerEntity player,float partial,
            boolean hand,Function<String,ResourceLocation> textures,Models models,RendererContextB173.Extras extras){
        var capture=new RendererContextB173.Extras(){
            @Override public void item(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity entity,
                    legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack,Matrix4f transform,RendererContextB173.Style style){
                var copy=stack==null?null:stack.copy();PlayerRenderPhasesB173.emit(()->extras.item(entity,copy,transform,style));
            }
            @Override public void text(String text,float x,float y,int color,boolean shadow,Matrix4f transform,RendererContextB173.Style style){
                PlayerRenderPhasesB173.emit(()->extras.text(text,x,y,color,shadow,transform,style));
            }
            @Override public void quad(List<RendererContextB173.Vertex> vertices,Matrix4f transform,ResourceLocation texture,RendererContextB173.Style style){
                var copy=List.copyOf(vertices);PlayerRenderPhasesB173.emit(()->extras.quad(copy,transform,texture,style));
            }
            @Override public void line(RendererContextB173.Vertex start,RendererContextB173.Vertex end,Matrix4f transform,RendererContextB173.Style style){
                PlayerRenderPhasesB173.emit(()->extras.line(start,end,transform,style));
            }
            @Override public int width(String text){return extras.width(text);}
            @Override public legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity camera(){return extras.camera();}
            @Override public float yaw(){return extras.yaw();}
            @Override public float pitch(){return extras.pitch();}
        };
        synchronized(renderer){
            renderer.mcc$camera(new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.EntityRenderer.LivingEntityCamera(extras.camera(),extras.yaw(),extras.pitch()));
            try(var scope=RendererContextB173.open(textures,(draw,texture)->{
                var style=RendererContextB173.style();int color=RendererContextB173.argb();
                PlayerRenderPhasesB173.emit(()->models.draw(draw,texture,style,color));
            },EntityRenderingB173.sheets(capture))){
                RendererContextB173.bind("/mob/char.png");
                return PlayerRenderPhasesB173.capture(()->{
                    if(hand)renderer.renderHand();
                    else renderer.render((legacyconnector.vb1_7_3.net.minecraft.entity.Entity)player,0,player.standingEyeHeight,0,player.yaw,partial);
                });
            }
        }
    }
    public static ResourceLocation texture(RenderersB173.Info info,String path,ResourceLocation skin){
        return "/mob/char.png".equals(path)||"mob/char.png".equals(path)?skin:EntityRenderingB173.texture(info.mod(),path);
    }
}
