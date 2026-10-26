package legacyconnector.v1_7_10.net.minecraft.client.audio;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.SoundBridge;

/**
 * The client's sound engine: legacy code hands it sounds by their 1.7.10 name. Each legacy sound played is remembered with
 * the modern sound instance playing it, so {@link #isSoundPlaying}, {@link #stopSound} and delayed sounds work on it.
 */
public class SoundHandler implements legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManagerReloadListener {
    public static final SoundHandler INSTANCE = new SoundHandler();

    static {
        // 1.7.10 created its sound manager with the handler; mods find it in the handler's SoundManager field (IC2)
        java.util.Objects.requireNonNull(SoundManager.INSTANCE);
    }

    /** The sound manager behind this handler (set by the manager; mods read it by reflection). */
    SoundManager sndManager;
    private final Map<ISound, SoundInstance> playing = new WeakHashMap<>();
    /** 1.7.10's music events under their modern names. */
    private static final Map<String, String> MUSIC_RENAMES = Map.of("music.game.creative", "music.creative", "music.game.end", "music.end",
            "music.game.end.dragon", "music.dragon", "music.game.end.credits", "music.credits", "music.game.nether", "music.nether.nether_wastes",
            "minecart.base", "entity.minecart.riding", "minecart.inside", "entity.minecart.inside");

    public void playSound(ISound sound) {
        SoundInstance instance = mcc$instance(sound);
        if (instance != null) net.minecraft.client.Minecraft.getInstance().getSoundManager().play(instance);
    }

    public void playDelayedSound(ISound sound, int delayTicks) {
        SoundInstance instance = mcc$instance(sound);
        if (instance != null) net.minecraft.client.Minecraft.getInstance().getSoundManager().playDelayed(instance, delayTicks);
    }

    /** The modern instance for a legacy sound (made and remembered here); null when the sound name is unknown. */
    public SoundInstance mcc$instance(ISound sound) {
        if (sound == null || sound.getPositionedSoundLocation() == null) return null;
        String name = sound.getPositionedSoundLocation().getResourceDomain().equals("minecraft") ? sound.getPositionedSoundLocation().getResourcePath()
                : sound.getPositionedSoundLocation().getResourceDomain() + ":" + sound.getPositionedSoundLocation().getResourcePath();
        SoundEvent event = SoundBridge.sound(MUSIC_RENAMES.getOrDefault(name, name));
        if (event == null) return null;
        SoundSource source = sound instanceof PositionedSoundRecord r && r.mcc$category != null ? r.mcc$category.mcc$source() : SoundSource.MASTER;
        SoundInstance instance;
        if (sound instanceof ITickableSound tickable) {
            instance = new Tickable(event, source, tickable);
        } else if (sound.getAttenuationType() == ISound.AttenuationType.NONE) {
            instance = new SimpleSoundInstance(event.getLocation(), source, sound.getVolume(), sound.getPitch(), RandomSource.create(), sound.canRepeat(),
                    sound.getRepeatDelay(), SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
        } else {
            instance = new SimpleSoundInstance(event.getLocation(), source, sound.getVolume(), sound.getPitch(), RandomSource.create(), sound.canRepeat(),
                    sound.getRepeatDelay(), SoundInstance.Attenuation.LINEAR, sound.getXPosF(), sound.getYPosF(), sound.getZPosF(), false);
        }
        synchronized (playing) {
            playing.put(sound, instance);
        }
        return instance;
    }

    /** The legacy sound a modern instance plays, or a positioned record standing in for a modern sound. */
    public ISound mcc$legacy(SoundInstance instance) {
        synchronized (playing) {
            for (var e : playing.entrySet()) if (e.getValue() == instance) return e.getKey();
        }
        var location = instance.getLocation();
        PositionedSoundRecord record = new PositionedSoundRecord(new legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation(location.getNamespace(), location.getPath()),
                instance.getSound() == null ? 1.0F : instance.getVolume(), instance.getSound() == null ? 1.0F : instance.getPitch(),
                (float) instance.getX(), (float) instance.getY(), (float) instance.getZ());
        record.mcc$category = SoundCategory.mcc$of(instance.getSource());
        return record;
    }

    public void stopSound(ISound sound) {
        SoundInstance instance;
        synchronized (playing) {
            instance = playing.remove(sound);
        }
        if (instance != null) net.minecraft.client.Minecraft.getInstance().getSoundManager().stop(instance);
    }

    public boolean isSoundPlaying(ISound sound) {
        SoundInstance instance;
        synchronized (playing) {
            instance = playing.get(sound);
        }
        return instance != null && net.minecraft.client.Minecraft.getInstance().getSoundManager().isActive(instance);
    }

    public void pauseSounds() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().pause();
    }

    public void resumeSounds() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().resume();
    }

    public void stopSounds() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().stop();
    }

    public void setListener(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, float partialTicks) {
    }

    public void update() {
    }

    /**
     * Rebuilds the sound list from the resource packs. The modern sound list is rebuilt by the game's resource reload,
     * which mods that add sounds trigger with {@code refreshResources()}; here only the engine is restarted.
     */
    @Override
    public void onResourceManagerReload(legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager manager) {
        if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) net.minecraft.client.Minecraft.getInstance().getSoundManager().reload();
    }

    /** A legacy sound updated every tick: its position, volume and pitch are copied each time, and it ends when the legacy sound says so. */
    private static final class Tickable extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
        private final ITickableSound legacy;

        Tickable(SoundEvent event, SoundSource source, ITickableSound legacy) {
            super(event, source, RandomSource.create());
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
}
