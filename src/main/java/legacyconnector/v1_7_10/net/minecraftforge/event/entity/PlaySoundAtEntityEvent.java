package legacyconnector.v1_7_10.net.minecraftforge.event.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

/**
 * An entity plays a sound. {@link #name} is the sound's legacy-style name (vanilla sounds by their modern id without the
 * {@code minecraft:} namespace); handlers may replace it or change the volume and pitch, or cancel the sound.
 */
@legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
public class PlaySoundAtEntityEvent extends EntityEvent {
    public String name;
    public final float volume;
    public final float pitch;
    public float newVolume;
    public float newPitch;

    public PlaySoundAtEntityEvent(Entity entity, String name, float volume, float pitch) {
        super(entity);
        this.name = name;
        this.volume = volume;
        this.pitch = pitch;
        this.newVolume = volume;
        this.newPitch = pitch;
    }
}
