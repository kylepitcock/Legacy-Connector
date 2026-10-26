package legacyconnector.v1_7_10.net.minecraftforge.client.event.sound;

import legacyconnector.v1_7_10.net.minecraft.client.audio.ISound;
import legacyconnector.v1_7_10.net.minecraft.client.audio.SoundCategory;
import legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager;

/** Fired before the client plays a sound: handlers set {@link #result} to another sound, or to null to silence it. */
public class PlaySoundEvent17 extends SoundEvent {
    public final String name;
    public final ISound sound;
    public final SoundCategory category;
    public ISound result;

    public PlaySoundEvent17(SoundManager manager, ISound sound, SoundCategory category) {
        super(manager);
        this.sound = sound;
        this.category = category;
        this.name = sound.getPositionedSoundLocation().getResourcePath();
        this.result = sound;
    }

    public PlaySoundEvent17(String name, ISound sound) {
        this(name, sound, SoundCategory.MASTER);
    }

    public PlaySoundEvent17(String name, ISound sound, SoundCategory category) {
        super(SoundManager.INSTANCE);
        this.name = name;
        this.sound = sound;
        this.category = category;
        this.result = sound;
    }
}
