package legacyconnector.v1_12_2.net.minecraftforge.event.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;

/** An entity plays a sound: handlers may change the sound, its category, volume and pitch, or cancel it. */
@Cancelable
public class PlaySoundAtEntityEvent extends EntityEvent {
    private SoundEvent name;
    private SoundCategory category;
    private final float volume;
    private final float pitch;
    private float newVolume;
    private float newPitch;

    public PlaySoundAtEntityEvent(Entity entity, SoundEvent name, SoundCategory category, float volume, float pitch) {
        super(entity);
        this.name = name;
        this.category = category;
        this.volume = volume;
        this.pitch = pitch;
        this.newVolume = volume;
        this.newPitch = pitch;
    }

    public SoundEvent getSound() {
        return name;
    }

    public SoundCategory getCategory() {
        return category;
    }

    public float getDefaultVolume() {
        return volume;
    }

    public float getDefaultPitch() {
        return pitch;
    }

    public float getVolume() {
        return newVolume;
    }

    public float getPitch() {
        return newPitch;
    }

    public void setSound(SoundEvent value) {
        this.name = value;
    }

    public void setCategory(SoundCategory category) {
        this.category = category;
    }

    public void setVolume(float value) {
        this.newVolume = value;
    }

    public void setPitch(float value) {
        this.newPitch = value;
    }
}
