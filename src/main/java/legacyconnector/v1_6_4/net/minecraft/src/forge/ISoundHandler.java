package legacyconnector.v1_6_4.net.minecraft.src.forge;

/**
 * Forge 3.x's sound hook. The layer calls the members that map onto the sound system's events (the library is (re)loaded, an entity plays a
 * sound); the {@code SoundPoolEntry}-based ones (background music, streaming, positional replacement) have no counterpart in the modern sound engine.
 */
public interface ISoundHandler {
    void onSetupAudio(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager manager);

    void onLoadSoundSettings(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager manager);

    /** The sound name an entity plays, possibly replaced; null cancels it. */
    String onPlaySoundAtEntity(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, String soundName, float volume, float pitch);
}
