package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Explicit native-cow geometry fallback; the mod's skin is preserved, custom models are not executed yet. */
public final class MobClientB173 {
    private static final Map<String,ResourceLocation> TEXTURES=new HashMap<>();
    private static final Set<String> MISSING=new HashSet<>();
    private static final Set<Class<?>> FAILED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static void register(IEventBus bus){
        legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$installFlashReader(level->level instanceof net.minecraft.client.multiplayer.ClientLevel client?client.getSkyFlashTime():0);
        bus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e)->{
            if(org.kylepitcock.legacyconnector.vb1_7_3.TileEntitiesB173.type()!=null)e.registerBlockEntityRenderer(org.kylepitcock.legacyconnector.vb1_7_3.TileEntitiesB173.type(),LegacyB173BlockEntityRendererB173::new);
            e.registerEntityRenderer(org.kylepitcock.legacyconnector.LegacyConnector.BETA_LIGHTNING.get(),NeutralEntityRendererB173::new);
            e.registerEntityRenderer(org.kylepitcock.legacyconnector.LegacyConnector.BETA_ENTITY.get(),NeutralEntityRendererB173::new);
            e.registerEntityRenderer(org.kylepitcock.legacyconnector.LegacyConnector.BETA_ITEM_ENTITY.get(),NeutralEntityRendererB173::new);
            for(var info:AnimalsB173.all())e.registerEntityRenderer(info.type(),AnimalRendererB173::new);
            for(var info:MobsB173.all())e.registerEntityRenderer(info.type(),context->new CowRenderer(context){
                // a captured original renderer (Twilight Forest's deer passes its own model to Beta's RenderCow) draws instead of the native cow
                @Override public void render(net.minecraft.world.entity.animal.Cow cow,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
                    if(cow instanceof LegacyB173Cow beta&&beta.legacy()!=null&&RenderersB173.info(beta.legacy().getClass())!=null&&!FAILED.contains(beta.legacy().getClass())){
                        var info=RenderersB173.info(beta.legacy().getClass());shadowRadius=info.renderer().mcc$shadowRadius();
                        org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.beginWorld(pose,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.KIND_WORLD,true);
                        pose.pushPose();boolean ok=false;
                        try(var readOnly=EntityViewsB173.readOnly()){
                            var extras=NativeRendererExtrasB173.create(beta,pose,buffers);
                            AnimalRenderingB173.drawView(beta.prepareRenderView(partial),net.minecraft.util.Mth.rotLerp(partial,beta.yRotO,beta.getYRot()),partial,(part,texture)->{
                                NativeRendererExtrasB173.applyStyle(RendererContextB173.style());int color=RendererContextB173.argb();
                                org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.color(((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,((color>>>24)&255)/255F);
                                org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.bindTexture(texture);ModelPartsB173.draw(part);
                            },extras);ok=true;
                        }catch(RuntimeException|LinkageError failure){
                            FAILED.add(beta.legacy().getClass());org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Render").error("Original Beta renderer failed for {}; native cow model from now on",cow.getType(),failure);
                        }finally{pose.popPose();org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.end();}
                        if(ok){renderNameTagIfAny(cow,yaw,partial,pose,buffers,light);return;}
                    }
                    super.render(cow,yaw,partial,pose,buffers,light);
                }
                private void renderNameTagIfAny(net.minecraft.world.entity.animal.Cow cow,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
                    if(shouldShowName(cow))renderNameTag(cow,cow.getDisplayName(),pose,buffers,light,partial);
                }
                @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.animal.Cow cow){
                    return cow instanceof LegacyB173Cow beta?texture(beta,super.getTextureLocation(cow)):super.getTextureLocation(cow);
                }
            });
        });
        bus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent e)->e.registerReloadListener(
            (net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(()->{
                for(var id:TEXTURES.values())Minecraft.getInstance().getTextureManager().release(id);TEXTURES.clear();MISSING.clear();
            })));
    }
    private static ResourceLocation texture(LegacyB173Cow cow,ResourceLocation fallback){
        String key=cow.getType().toString()+":"+cow.texturePath();
        if(TEXTURES.containsKey(key))return TEXTURES.get(key);
        if(MISSING.contains(key))return fallback;
        byte[] bytes=MobsB173.textureBytes(MobsB173.info(cow.getType()),cow.texturePath());
        if(bytes==null){MISSING.add(key);return fallback;}
        try{
            var image=com.mojang.blaze3d.platform.NativeImage.read(new ByteArrayInputStream(bytes));
            var id=Minecraft.getInstance().getTextureManager().register("b173_mob_"+HexFormat.of().formatHex(key.getBytes(StandardCharsets.UTF_8)),new DynamicTexture(image));
            TEXTURES.put(key,id);return id;
        }catch(java.io.IOException e){MISSING.add(key);return fallback;}
    }
}
