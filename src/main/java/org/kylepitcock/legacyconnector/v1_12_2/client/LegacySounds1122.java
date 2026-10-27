package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.client.audio.ISound;
import legacyconnector.v1_12_2.net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** 1.12.2 sounds as modern sound instances: the sound event id is the same, tickable sounds are updated by the modern sound manager. */
public final class LegacySounds1122 {
    private static final Map<ISound, SoundInstance> PLAYING = new WeakHashMap<>();

    private LegacySounds1122() {}

    private static SoundSource source(ISound sound) {
        try {
            return sound.getCategory() == null ? SoundSource.MASTER : SoundSource.valueOf(sound.getCategory().name());
        } catch (IllegalArgumentException e) {
            return SoundSource.MASTER;
        }
    }

    /** The modern instance for a legacy sound (made and remembered here); null for a sound without a name. */
    public static SoundInstance instance(ISound sound) {
        if (sound == null || sound.getSoundLocation() == null) return null;
        var id = sound.getSoundLocation().mcc$modern();
        SoundInstance instance;
        if (sound instanceof ITickableSound tickable) {
            instance = new Tickable(tickable, id, source(sound));
        } else {
            boolean relative = sound.getAttenuationType() == ISound.AttenuationType.NONE;
            instance = new SimpleSoundInstance(id, source(sound), sound.getVolume(), sound.getPitch(), RandomSource.create(), sound.canRepeat(),
                    sound.getRepeatDelay(), relative ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR,
                    sound.getXPosF(), sound.getYPosF(), sound.getZPosF(), relative);
        }
        synchronized (PLAYING) {
            PLAYING.put(sound, instance);
        }
        return instance;
    }

    public static void play(ISound sound, int delay) {
        SoundInstance instance = instance(sound);
        if (instance == null) return;
        var manager = Minecraft.getInstance().getSoundManager();
        if (delay > 0) manager.playDelayed(instance, delay);
        else manager.play(instance);
    }

    public static boolean isPlaying(ISound sound) {
        SoundInstance instance;
        synchronized (PLAYING) {
            instance = PLAYING.get(sound);
        }
        return instance != null && Minecraft.getInstance().getSoundManager().isActive(instance);
    }

    public static void stop(ISound sound) {
        SoundInstance instance;
        synchronized (PLAYING) {
            instance = PLAYING.remove(sound);
        }
        if (instance != null) Minecraft.getInstance().getSoundManager().stop(instance);
    }

    /** A tickable legacy sound: each tick runs its update and copies its position, volume and pitch. */
    private static final class Tickable extends AbstractTickableSoundInstance {
        private final ITickableSound legacy;

        Tickable(ITickableSound legacy, net.minecraft.resources.ResourceLocation id, SoundSource source) {
            super(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id), source, RandomSource.create());
            this.legacy = legacy;
            this.looping = legacy.canRepeat();
            this.delay = legacy.getRepeatDelay();
            this.relative = legacy.getAttenuationType() == ISound.AttenuationType.NONE;
            this.attenuation = relative ? Attenuation.NONE : Attenuation.LINEAR;
            copy();
        }

        private void copy() {
            this.x = legacy.getXPosF();
            this.y = legacy.getYPosF();
            this.z = legacy.getZPosF();
            this.volume = legacy.getVolume();
            this.pitch = legacy.getPitch();
        }

        @Override
        public void tick() {
            try {
                legacy.update();
                copy();
                if (legacy.isDonePlaying()) stop();
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Sound").warn("A legacy moving sound failed and was stopped: {}", e.toString());
                stop();
            }
        }
    }
}
