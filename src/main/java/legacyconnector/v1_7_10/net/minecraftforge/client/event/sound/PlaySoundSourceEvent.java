package legacyconnector.v1_7_10.net.minecraftforge.client.event.sound;

import legacyconnector.v1_7_10.net.minecraft.client.audio.ISound;
import legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager;

/** A (non-streamed) sound started playing in the engine (posted from NeoForge's PlaySoundSourceEvent, see SoundEvents1710). */
public class PlaySoundSourceEvent extends SoundEvent.SoundSourceEvent {
    @Deprecated
    public final SoundManager manager;
    @Deprecated
    public final String name;
    @Deprecated
    public final float x;
    @Deprecated
    public final float y;
    @Deprecated
    public final float z;

    @Deprecated
    public PlaySoundSourceEvent(SoundManager manager, String name, float x, float y, float z) {
        super(manager, null, null);
        this.manager = manager;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PlaySoundSourceEvent(SoundManager manager, ISound sound, String uuid) {
        super(manager, sound, uuid);
        this.manager = manager;
        this.name = sound.getPositionedSoundLocation().getResourcePath();
        this.x = this.y = this.z = 0.0F;
    }
}
