package org.kylepitcock.legacyconnector.vb1_7_3.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173;

/** Upload legacy RGBA frames at the stitched sprite location, including every allocated mip level. */
final class AnimatedTexturesB173 {
    private static TextureAtlas atlas;
    static void register(net.neoforged.bus.api.IEventBus modBus){
        modBus.addListener((net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent e)->{
            if(e.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS))atlas=e.getAtlas();
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e)->{
            if(atlas==null || net.minecraft.client.Minecraft.getInstance().isPaused())return;
            atlas.bind();AnimationsB173.tick(AnimatedTexturesB173::upload);
        });
    }
    private static void upload(AnimationsB173.Frame frame){
        var sprite=atlas.getTextures().get(ResourceLocation.parse(frame.texture()));if(sprite==null)return;
        int width=sprite.contents().width(),height=sprite.contents().height();
        var image=new NativeImage(width,height,false);NativeImage[] levels={image};
        try{
            byte[] rgba=frame.pixels();
            for(int y=0;y<height;y++)for(int x=0;x<width;x++){
                int i=((y*frame.size()/height)*frame.size()+x*frame.size()/width)*4;
                image.setPixelRGBA(x,y,((rgba[i+3]&255)<<24)|((rgba[i+2]&255)<<16)|((rgba[i+1]&255)<<8)|(rgba[i]&255));
            }
            levels=MipmapGenerator.generateMipLevels(levels,sprite.contents().byMipLevel.length-1);
            for(int mip=0;mip<levels.length;mip++)levels[mip].upload(mip,sprite.getX()>>mip,sprite.getY()>>mip,0,0,width>>mip,height>>mip,levels.length>1,false);
        }finally{for(var level:levels)level.close();}
    }
}
