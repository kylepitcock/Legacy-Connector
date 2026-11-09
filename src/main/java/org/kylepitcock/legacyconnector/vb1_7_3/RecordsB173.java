package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;

/** Original record sound names enter the native jukebox registry and lifecycle. */
public final class RecordsB173 {
    private static final Map<ResourceLocation,String> CUSTOM=new LinkedHashMap<>();
    private RecordsB173(){}
    public static synchronized ResourceKey<JukeboxSong> song(ResourceLocation item,String sound){
        // The two Beta built-in streaming resources already have native song definitions.
        if("cat".equals(sound)||"13".equals(sound))return ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG,ResourceLocation.withDefaultNamespace(sound));
        var id=ResourceLocation.fromNamespaceAndPath(item.getNamespace(),"beta/"+item.getPath());CUSTOM.put(id,sound);
        return ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG,id);
    }
    public static synchronized void contribute(Map<String,Supplier<byte[]>> out){
        for(var entry:CUSTOM.entrySet()){
            var json=new com.google.gson.JsonObject();
            var sound=new com.google.gson.JsonObject();sound.addProperty("sound_id",AudioB173.identifier(AudioB173.Kind.STREAMING,entry.getValue()).toString());
            json.add("sound_event",sound);
            json.addProperty("description",entry.getValue());
            // Beta did not time out its jukebox state. The sound stream itself ends at EOF.
            json.addProperty("length_in_seconds",100000000F);json.addProperty("comparator_output",1);
            byte[] bytes=json.toString().getBytes(StandardCharsets.UTF_8);var id=entry.getKey();
            out.put("data/"+id.getNamespace()+"/jukebox_song/"+id.getPath()+".json",()->bytes);
        }
    }
}
