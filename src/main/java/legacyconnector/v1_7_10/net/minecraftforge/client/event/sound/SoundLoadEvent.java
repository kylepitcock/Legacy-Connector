package legacyconnector.v1_7_10.net.minecraftforge.client.event.sound;

import legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager;

/** The sound system has (re)loaded: mods that play through the sound library directly set themselves up here (IC2). */
public class SoundLoadEvent extends SoundEvent {
    public SoundLoadEvent(SoundManager manager) {
        super(manager);
    }
}
