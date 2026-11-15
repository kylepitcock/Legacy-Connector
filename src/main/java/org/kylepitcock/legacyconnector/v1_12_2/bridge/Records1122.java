package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.item.ItemRecord;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;

/** 1.12.2 mod music discs: a generated native jukebox song per disc, and the item's {@code jukebox_playable} component pointing at it. */
public final class Records1122 {
    private Records1122() {}

    private static ResourceLocation key(ItemRecord disc) {
        var sound = disc.getSound();
        if (sound == null) return null;
        var event = sound.mcc$event();
        if (event == null) return null;
        var loc = event.getLocation();
        if (loc.getNamespace().equals("minecraft")) return null; // a vanilla disc has its song
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "records1122/" + loc.getNamespace() + "/" + loc.getPath());
    }

    public static ResourceKey<JukeboxSong> songKey(Object item) {
        if (!(item instanceof ItemRecord disc)) return null;
        var id = key(disc);
        return id == null ? null : ResourceKey.create(Registries.JUKEBOX_SONG, id);
    }

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        for (ItemRecord disc : ItemRecord.mcc$all()) {
            try {
                ResourceLocation id = key(disc);
                if (id == null) continue;
                JsonObject json = new JsonObject();
                JsonObject event = new JsonObject();
                event.addProperty("sound_id", disc.getSound().mcc$event().getLocation().toString());
                json.add("sound_event", event);
                JsonObject description = new JsonObject();
                description.addProperty("translate", disc.mcc$descriptionKey());
                json.add("description", description);
                json.addProperty("length_in_seconds", 100000000F);
                json.addProperty("comparator_output", 1);
                byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
                out.put("data/" + id.getNamespace() + "/jukebox_song/" + id.getPath() + ".json", () -> bytes);
            } catch (RuntimeException | LinkageError e) {
                // a disc whose sound cannot be named has no song
            }
        }
    }
}
