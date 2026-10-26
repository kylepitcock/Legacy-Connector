package legacyconnector.v1_7_10.net.minecraftforge.client.event.sound;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.client.audio.ISound;
import legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager;

/** Base of Forge's client sound events: the sound manager they come from. */
public class SoundEvent extends Event {
    public final SoundManager manager;

    public SoundEvent(SoundManager manager) {
        this.manager = manager;
    }

    /** A sound whose source the engine just started: {@code uuid} names the source while it plays. */
    public static class SoundSourceEvent extends SoundEvent {
        public final ISound sound;
        public final String uuid;
        public final String name;

        public SoundSourceEvent(SoundManager manager, ISound sound, String uuid) {
            super(manager);
            this.name = sound == null || sound.getPositionedSoundLocation() == null ? null : sound.getPositionedSoundLocation().getResourcePath();
            this.sound = sound;
            this.uuid = uuid;
        }
    }
}
