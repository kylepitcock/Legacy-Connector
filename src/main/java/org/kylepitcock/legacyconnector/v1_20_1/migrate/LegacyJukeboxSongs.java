package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Music discs of translated 1.20.1 mods. 1.20.1 played a {@code RecordItem} through the item class; 1.21.1 plays any item that carries the
 * {@code jukebox_playable} component, naming a {@code jukebox_song} entry of a data pack. Both halves are produced the way a native mod provides them: a
 * generated data pack file per {@link LegacyRecordItem} ({@code data/<ns>/jukebox_song/<item>.json}: sound, description, length, comparator output) and the
 * default component of the item ({@link ModifyDefaultComponentsEvent}), so vanilla's jukebox, comparators and the "now playing" overlay work for the disc.
 * The song is named after the item.
 */
public final class LegacyJukeboxSongs {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Jukebox1201");

    private LegacyJukeboxSongs() {}

    /** Mod bus listener of the runtime: the default {@code jukebox_playable} component of every legacy disc. */
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        for (Item item : event.getAllItems().toList()) {
            if (!(item instanceof LegacyRecordItem)) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            ResourceKey<JukeboxSong> song = ResourceKey.create(Registries.JUKEBOX_SONG, id);
            event.modify(item, builder -> builder.set(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(new EitherHolder<>(song), true)));
        }
    }

    /** {@code GeneratedPack.Contributor}: the song entry of every registered legacy disc. */
    public static void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof LegacyRecordItem disc)) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            try {
                byte[] json = song(id, disc).toString().getBytes(StandardCharsets.UTF_8);
                out.put("data/" + id.getNamespace() + "/jukebox_song/" + id.getPath() + ".json", () -> json);
            } catch (RuntimeException | LinkageError ex) {
                LOG.warn("disc {} could not be converted to a jukebox song: {}", id, ex.toString());
            }
        }
    }

    /** The {@code jukebox_song} JSON of a disc: sound event id, description line, length in seconds, comparator output (0 to 15). */
    static JsonObject song(ResourceLocation id, LegacyRecordItem disc) {
        ResourceLocation sound = BuiltInRegistries.SOUND_EVENT.getKey(disc.getSound());
        if (sound == null) throw new IllegalStateException("the sound event of " + id + " is not registered");
        return song(sound, disc.getDescriptionId() + ".desc", disc.getLengthInTicks(), disc.getAnalogOutput());
    }

    /** Pure form of {@link #song(ResourceLocation, LegacyRecordItem)} (unit tests). */
    static JsonObject song(ResourceLocation sound, String descriptionKey, int lengthInTicks, int analogOutput) {
        JsonObject json = new JsonObject();
        json.addProperty("sound_event", sound.toString());
        JsonObject description = new JsonObject();
        description.addProperty("translate", descriptionKey);
        json.add("description", description);
        json.addProperty("length_in_seconds", Math.max(1, lengthInTicks) / 20.0f);
        json.addProperty("comparator_output", Math.max(0, Math.min(15, analogOutput)));
        return json;
    }
}
