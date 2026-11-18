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

    /** 1.5.2 {@code addSound(name, file)}: registers a sound file by name (the file name without digits and extension is the event); the sound plays from {@code file} (see {@code SoundPool}). */
    public static void addSound(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager self, String name, java.io.File file) {
        SoundPool.SOUNDS.addSound(name, file);
    }

    // ------------------------------------------------------------------ 1.6.4 gap round 3: sounds bound to an entity

    /** 1.6.4 {@code stopEntitySound(entity)}: stops every sound started with {@code playEntitySound} for the entity. */
    public static void stopEntitySound(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager self, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        legacyconnector.v1_6_4.forward.EntitySounds164.stop(self, entity);
    }

    /** 1.6.4 {@code updateSoundLocation(entity)}: moves the entity's sounds to where it is now. */
    public static void updateSoundLocation(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager self, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        legacyconnector.v1_6_4.forward.EntitySounds164.move(entity, entity);
    }

    /** 1.6.4 {@code updateSoundLocation(entity, at)}: moves the sounds of {@code entity} to the position of {@code at}. */
    public static void updateSoundLocation(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager self, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity,
            legacyconnector.v1_7_10.net.minecraft.entity.Entity at) {
        legacyconnector.v1_6_4.forward.EntitySounds164.move(entity, at);
    }

    /** 1.6.4 {@code isEntitySoundPlaying(entity)}: whether a sound started for the entity is still playing. */
    public static boolean isEntitySoundPlaying(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager self, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        return legacyconnector.v1_6_4.forward.EntitySounds164.playing(self, entity);
    }
}
