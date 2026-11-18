package legacyconnector.v1_7_10.net.minecraft.client.audio;

import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** A one-shot sound at a position (or, with no attenuation, a UI sound). */
public class PositionedSoundRecord extends PositionedSound {
    /** The volume slider the sound plays under (music and records have their own); null for the master volume. */
    public SoundCategory mcc$category;

    public PositionedSoundRecord(ResourceLocation location, float volume, float pitch, float x, float y, float z) {
        this(location, volume, pitch, false, 0, ISound.AttenuationType.LINEAR, x, y, z);
    }

    protected PositionedSoundRecord(ResourceLocation location, float volume, float pitch, boolean repeat, int repeatDelay, ISound.AttenuationType attenuation,
            float x, float y, float z) {
        super(location);
        this.volume = volume;
        this.field_147663_c = pitch;
        this.repeat = repeat;
        this.field_147665_h = repeatDelay;
        this.field_147666_i = attenuation;
        this.xPosF = x;
        this.yPosF = y;
        this.zPosF = z;
    }

    /** A UI sound (button clicks): full volume at the listener, no attenuation. */
    public static PositionedSoundRecord createPositionedSoundRecord(ResourceLocation location, float pitch) {
        return new PositionedSoundRecord(location, 0.25F, pitch, false, 0, ISound.AttenuationType.NONE, 0.0F, 0.0F, 0.0F);
    }

    public static PositionedSoundRecord createPositionedSoundRecord(ResourceLocation location) {
        return new PositionedSoundRecord(location, 1.0F, 1.0F, false, 0, ISound.AttenuationType.NONE, 0.0F, 0.0F, 0.0F);
    }

    public static PositionedSoundRecord createPositionedSoundRecord(ResourceLocation location, float volume, float pitch, float x, float y, float z) {
        return new PositionedSoundRecord(location, volume, pitch, false, 0, ISound.AttenuationType.LINEAR, x, y, z);
    }

    /** Background music: full volume, everywhere, under the music slider. */
    public static PositionedSoundRecord func_147673_a(ResourceLocation location) {
        PositionedSoundRecord r = new PositionedSoundRecord(location, 1.0F, 1.0F, false, 0, ISound.AttenuationType.NONE, 0.0F, 0.0F, 0.0F);
        r.mcc$category = SoundCategory.MUSIC;
        return r;
    }

    /** A record playing in a jukebox at the position (volume 4: heard 64 blocks away), under the records slider. */
    public static PositionedSoundRecord createRecordSoundAtPosition(ResourceLocation location, float x, float y, float z) {
        PositionedSoundRecord r = new PositionedSoundRecord(location, 4.0F, 1.0F, false, 0, ISound.AttenuationType.LINEAR, x, y, z);
        r.mcc$category = SoundCategory.RECORDS;
        return r;
    }
}
