package legacyconnector.v1_7_10.net.minecraftforge.client.event.sound;

import legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager;

/** The sound system is about to load (1.7.10 mods registered codecs here; the modern engine has its own). */
public class SoundSetupEvent extends SoundEvent {
    public SoundSetupEvent(SoundManager manager) {
        super(manager);
    }
}
