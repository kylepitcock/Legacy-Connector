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
    public static void register(IEventBus bus){
        bus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e)->{
            for(var info:AnimalsB173.all())e.registerEntityRenderer(info.type(),AnimalRendererB173::new);
            for(var info:MobsB173.all())e.registerEntityRenderer(info.type(),context->new CowRenderer(context){
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
