package legacyconnector.v1_12_2.net.minecraftforge.client.event.sound;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Base of Forge's sound engine events. */
public class SoundEvent extends Event {
    private final legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager manager;

    public SoundEvent() {
        this(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager.mcc$INSTANCE);
    }

    public SoundEvent(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager manager) {
        this.manager = manager;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager getManager() {
        return manager;
    }

    /** A sound source started: the sound, its source id and event name. */
    public static class SoundSourceEvent extends SoundEvent {
        private final legacyconnector.v1_12_2.net.minecraft.client.audio.ISound sound;
        private final String uuid;
        private final String name;

        public SoundSourceEvent(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager manager,
                legacyconnector.v1_12_2.net.minecraft.client.audio.ISound sound, String uuid) {
            super(manager);
            this.sound = sound;
            this.uuid = uuid;
            this.name = sound == null || sound.getSoundLocation() == null ? "" : sound.getSoundLocation().getPath();
        }

        public legacyconnector.v1_12_2.net.minecraft.client.audio.ISound getSound() {
            return sound;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }
    }
}
