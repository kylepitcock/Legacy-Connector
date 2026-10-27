package legacyconnector.v1_12_2.net.minecraft.client.audio;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;

/** A sound to play: its event name, category, volume, pitch, position and how it fades with distance. */
public interface ISound {
    ResourceLocation getSoundLocation();

    SoundCategory getCategory();

    boolean canRepeat();

    int getRepeatDelay();

    float getVolume();

    float getPitch();

    float getXPosF();

    float getYPosF();

    float getZPosF();

    AttenuationType getAttenuationType();

    /** Looks up the sound event of {@link #getSoundLocation} and picks the sound to play; returns the event (null when unknown). */
    default SoundEventAccessor createAccessor(SoundHandler handler) {
        return handler.getAccessor(getSoundLocation());
    }

    /** The sound picked by {@link #createAccessor}. */
    default Sound getSound() {
        return SoundHandler.MISSING_SOUND;
    }

    enum AttenuationType {
        NONE(0),
        LINEAR(2);

        private final int type;

        AttenuationType(int typeIn) {
            this.type = typeIn;
        }

        public int getTypeInt() {
            return type;
        }
    }
}
