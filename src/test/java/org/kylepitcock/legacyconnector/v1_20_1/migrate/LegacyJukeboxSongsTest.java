package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** The {@code jukebox_song} entry written for a legacy {@code RecordItem}. */
class LegacyJukeboxSongsTest {
    @Test
    void songEntryHasTheFieldsOfTheJukeboxSongCodec() {
        JsonObject json = LegacyJukeboxSongs.song(ResourceLocation.fromNamespaceAndPath("mod", "disc_sound"), "item.mod.disc.desc", 2400, 5);
        assertEquals("mod:disc_sound", json.get("sound_event").getAsString());
        assertEquals("item.mod.disc.desc", json.getAsJsonObject("description").get("translate").getAsString());
        assertEquals(120.0f, json.get("length_in_seconds").getAsFloat());
        assertEquals(5, json.get("comparator_output").getAsInt());
    }

    @Test
    void comparatorOutputAndLengthAreClampedToWhatTheCodecAccepts() {
        JsonObject json = LegacyJukeboxSongs.song(ResourceLocation.fromNamespaceAndPath("mod", "s"), "k", 0, 99);
        assertEquals(15, json.get("comparator_output").getAsInt());
        assertEquals(0.05f, json.get("length_in_seconds").getAsFloat());
        assertEquals(0, LegacyJukeboxSongs.song(ResourceLocation.fromNamespaceAndPath("mod", "s"), "k", 20, -3).get("comparator_output").getAsInt());
    }
}
