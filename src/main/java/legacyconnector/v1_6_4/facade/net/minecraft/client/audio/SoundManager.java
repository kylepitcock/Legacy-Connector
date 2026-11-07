package legacyconnector.v1_6_4.facade.net.minecraft.client.audio;

import legacyconnector.v1_6_4.net.minecraft.client.audio.SoundPool;

/** Facade of the 1.7.10 shim's {@code SoundManager} for 1.6.4 mods: the three sound pools they add their files to. Never instantiated. */
public final class SoundManager {
    private SoundManager() {}

    /** SoundManager.soundPoolSounds */
    public static SoundPool mcc$get$soundPoolSounds(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager self) {
        return SoundPool.SOUNDS;
    }

    /** SoundManager.soundPoolStreaming */
    public static SoundPool mcc$get$soundPoolStreaming(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager self) {
        return SoundPool.STREAMING;
    }

    /** SoundManager.soundPoolMusic */
    public static SoundPool mcc$get$soundPoolMusic(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager self) {
        return SoundPool.MUSIC;
    }
}
