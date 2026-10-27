package legacyconnector.v1_12_2.net.minecraft.client.audio;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;

/** A sound with its properties in fields (subclasses change them). Volume and pitch are the sound's own; sounds.json scales them further. */
public abstract class PositionedSound implements ISound {
    protected SoundCategory category;
    protected ResourceLocation positionedSoundLocation;
    protected float volume = 1.0F;
    protected float pitch = 1.0F;
    protected float xPosF;
    protected float yPosF;
    protected float zPosF;
    protected boolean repeat;
    protected int repeatDelay;
    protected ISound.AttenuationType attenuationType = ISound.AttenuationType.LINEAR;
    protected Sound sound;
    private SoundEventAccessor soundEvent;

    protected PositionedSound(SoundEvent soundIn, SoundCategory categoryIn) {
        this(soundIn.getSoundName(), categoryIn);
    }

    protected PositionedSound(ResourceLocation soundId, SoundCategory categoryIn) {
        this.positionedSoundLocation = soundId;
        this.category = categoryIn;
    }

    @Override
    public SoundEventAccessor createAccessor(SoundHandler handler) {
        soundEvent = handler.getAccessor(positionedSoundLocation);
        sound = soundEvent == null ? SoundHandler.MISSING_SOUND : soundEvent.cloneEntry();
        return soundEvent;
    }

    @Override
    public Sound getSound() {
        return sound;
    }

    @Override
    public ResourceLocation getSoundLocation() {
        return positionedSoundLocation;
    }

    @Override
    public SoundCategory getCategory() {
        return category;
    }

    @Override
    public boolean canRepeat() {
        return repeat;
    }

    @Override
    public int getRepeatDelay() {
        return repeatDelay;
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    @Override
    public float getXPosF() {
        return xPosF;
    }

    @Override
    public float getYPosF() {
        return yPosF;
    }

    @Override
    public float getZPosF() {
        return zPosF;
    }

    @Override
    public ISound.AttenuationType getAttenuationType() {
        return attenuationType;
    }
}
