package legacyconnector.v1_6_4.net.minecraft.src.forge.adaptors;

import legacyconnector.v1_6_4.net.minecraft.src.forge.ISoundHandler;

/** Forge 3.x's convenience base class for {@link ISoundHandler}: most mods only care about onSetupAudio/onLoadSoundSettings. */
public class SoundHandlerAdaptor implements ISoundHandler {
    @Override
    public void onSetupAudio(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager manager) {}

    @Override
    public void onLoadSoundSettings(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager manager) {}

    @Override
    public String onPlaySoundAtEntity(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, String soundName, float volume, float pitch) {
        return soundName;
    }
}
