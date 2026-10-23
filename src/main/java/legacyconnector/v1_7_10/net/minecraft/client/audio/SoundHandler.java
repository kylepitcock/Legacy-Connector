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
}
