package legacyconnector.v1_7_10.net.minecraft.client.audio;

import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** A sound a legacy mod asks the sound handler to play. */
public interface ISound {
    ResourceLocation getPositionedSoundLocation();

    boolean canRepeat();

    int getRepeatDelay();

    float getVolume();

    float getPitch();

    float getXPosF();

    float getYPosF();

    float getZPosF();

    AttenuationType getAttenuationType();

    enum AttenuationType {
        NONE(0),
        LINEAR(2);

        private final int type;

        AttenuationType(int type) {
            this.type = type;
        }

        public int getTypeInt() {
            return type;
        }
    }
}
