package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.net.minecraft.item.ItemRecord;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.LevelAccessor;
import org.kylepitcock.legacyconnector.core.Ids;

/**
 * Legacy music discs in the native jukebox song registry. 1.7.10 played a record by the item id of the disc (world effect 1005, stopped with 0);
 * the modern level event takes the registry id of a {@link JukeboxSong}. A vanilla disc has its song already; a mod's disc ({@link ItemRecord}
 * subclasses, whose sound is {@code getRecordResource("records." + name)}) gets one generated into the data pack, so a machine that plays and
 * stops records through the world effect (an automatic jukebox) starts and stops the real streaming sound.
 */
public final class Records1710 {
    private Records1710() {}

    private static ResourceLocation key(ItemRecord disc) {
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "records/" + Ids.normalizePath(disc.recordName));
    }

    /** The registry id of the song of a legacy disc item id, or -1 when the item is no disc or the level does not know its song. */
    public static int songId(LevelAccessor level, int legacyItemId) {
        if (level == null) return -1;
        try {
            var item = ItemBridge.byId(legacyItemId);
            if (!(item instanceof ItemRecord disc)) return -1;
            var registry = level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG);
            if (disc.mcc$vanilla != null) {
                var stack = ItemBridge.toModern(item, 1, 0);
                var song = JukeboxSong.fromStack(level.registryAccess(), stack);
                return song.isPresent() ? registry.getId(song.get().value()) : -1;
            }
            var holder = registry.getHolder(key(disc));
            return holder.isPresent() ? registry.getId(holder.get().value()) : -1;
        } catch (RuntimeException | LinkageError e) {
            return -1;
        }
    }

    /** Data pack: one song per mod disc. */
    public static void contribute(Map<String, Supplier<byte[]>> out) {
        for (ItemRecord disc : ItemRecord.mcc$all()) {
            if (disc.mcc$vanilla != null) continue;
            try {
                String sound = SoundBridge.sound(disc.getRecordResource("records." + disc.recordName).toString()).getLocation().toString();
                JsonObject json = new JsonObject();
                JsonObject event = new JsonObject();
                event.addProperty("sound_id", sound);
                json.add("sound_event", event);
                JsonObject description = new JsonObject();
                description.addProperty("translate", "item.record." + disc.recordName + ".desc");
                json.add("description", description);
                // A legacy jukebox never timed out a disc (the machine stops it itself); the stream ends at its end of file.
                json.addProperty("length_in_seconds", 100000000F);
                json.addProperty("comparator_output", 1);
                byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
                ResourceLocation id = key(disc);
                out.put("data/" + id.getNamespace() + "/jukebox_song/" + id.getPath() + ".json", () -> bytes);
            } catch (RuntimeException | LinkageError e) {
                // a disc whose sound cannot be named has no song
            }
        }
    }
}
