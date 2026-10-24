package legacyconnector.v1_7_10.net.minecraftforge.client.event.sound;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.client.audio.ISound;

/** Fired before the client plays a sound: handlers set {@link #result} to another sound, or to null to silence it. */
public class PlaySoundEvent17 extends Event {
    public final String name;
    public final ISound sound;
    public ISound result;

    public PlaySoundEvent17(String name, ISound sound) {
        this.name = name;
        this.sound = sound;
        this.result = sound;
    }
}
