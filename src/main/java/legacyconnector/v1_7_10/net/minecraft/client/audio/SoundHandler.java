package legacyconnector.v1_7_10.net.minecraft.client.audio;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.SoundBridge;

/** The client's sound engine: legacy code hands it sounds by their 1.7.10 name. */
public class SoundHandler {
    public static final SoundHandler INSTANCE = new SoundHandler();

    public void playSound(ISound sound) {
        if (sound == null || sound.getPositionedSoundLocation() == null) return;
        String name = sound.getPositionedSoundLocation().getResourceDomain().equals("minecraft") ? sound.getPositionedSoundLocation().getResourcePath()
                : sound.getPositionedSoundLocation().getResourceDomain() + ":" + sound.getPositionedSoundLocation().getResourcePath();
        SoundEvent event = SoundBridge.sound(name);
        if (event == null) return;
        var manager = net.minecraft.client.Minecraft.getInstance().getSoundManager();
        if (sound instanceof ITickableSound tickable) {
            manager.play(new Tickable(event, tickable));
            return;
        }
        if (sound.getAttenuationType() == ISound.AttenuationType.NONE) {
            manager.play(SimpleSoundInstance.forUI(event, sound.getPitch(), sound.getVolume()));
        } else {
            manager.play(new SimpleSoundInstance(event, net.minecraft.sounds.SoundSource.MASTER, sound.getVolume(), sound.getPitch(), RandomSource.create(),
                    sound.getXPosF(), sound.getYPosF(), sound.getZPosF()));
        }
    }

    public void playDelayedSound(ISound sound, int delayTicks) {
        playSound(sound);
    }

    public void stopSound(ISound sound) {
    }

    public void pauseSounds() {
    }

    public void resumeSounds() {
    }

    public void stopSounds() {
    }

    /** A legacy sound updated every tick: its position, volume and pitch are copied each time, and it ends when the legacy sound says so. */
    private static final class Tickable extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
        private final ITickableSound legacy;

        Tickable(SoundEvent event, ITickableSound legacy) {
            super(event, net.minecraft.sounds.SoundSource.MASTER, RandomSource.create());
            this.legacy = legacy;
            this.looping = legacy.canRepeat();
            this.delay = legacy.getRepeatDelay();
            this.relative = legacy.getAttenuationType() == ISound.AttenuationType.NONE;
            // A sound without attenuation plays at the listener whatever position the legacy code gives it (the rocket's engine
            // note sets its own coordinates): relative to the listener that means the origin, with no fall-off.
            if (this.relative) this.attenuation = net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE;
            copy();
        }

        private void copy() {
            this.volume = legacy.getVolume();
            this.pitch = legacy.getPitch();
            if (this.relative) return;
            this.x = legacy.getXPosF();
            this.y = legacy.getYPosF();
            this.z = legacy.getZPosF();
        }

        @Override
        public void tick() {
            legacy.update();
            copy();
            if (legacy.isDonePlaying()) stop();
        }
    }

    public boolean isSoundPlaying(ISound sound) {
        return false;
    }
}
