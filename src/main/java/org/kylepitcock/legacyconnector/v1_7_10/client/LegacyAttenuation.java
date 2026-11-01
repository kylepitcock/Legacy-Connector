package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.kylepitcock.legacyconnector.core.LegacyMods;

/**
 * Legacy positioned sounds fall off with distance the way 1.7.10 (and 1.12.2) let them: in software. Their sound library
 * (paulscode) computed a linear source's gain itself ({@code 1 - distance / range}, range = 16 blocks times the volume when
 * that is above 1) and handed it to OpenAL as the source gain. The modern engine leaves the fall-off to OpenAL's distance
 * model, which only works on mono buffers: a stereo file plays at full volume wherever the listener is. Most mods of that
 * time ship stereo files (111 of ChromatiCraft's 147 sounds), so their machine hums, pylon pulses and rituals came out at full
 * volume from far away. A legacy linear sound is therefore played without OpenAL attenuation (its position still pans mono
 * files) and with the 1.7.10 gain recomputed every tick from the listener's distance.
 *
 * <p>Applies to sounds a legacy mod plays through {@code SoundHandler} and to world sounds whose event belongs to a legacy
 * mod's namespace (played through the level, they reach the client as ordinary sound packets).</p>
 */
public final class LegacyAttenuation implements TickableSoundInstance {
    private final SoundInstance inner;
    private boolean stopped;
    /** A dimension's sound reduction (Galacticraft's vacuum, see DimensionSounds1710); 1 normally. */
    private float divisor = 1.0F;

    private LegacyAttenuation(SoundInstance inner) {
        this.inner = inner;
    }

    /** The sound with 1.7.10's software fall-off when it is a positioned linear sound; anything else is returned as it is. */
    public static SoundInstance wrap(SoundInstance sound) {
        if (sound == null || sound instanceof LegacyAttenuation || sound.isRelative() || sound.getAttenuation() != SoundInstance.Attenuation.LINEAR) {
            return sound;
        }
        return new LegacyAttenuation(sound);
    }

    /** World sounds of legacy mods' namespaces get the software fall-off too (registered on the game bus, client only). */
    public static void register(IEventBus gameBus) {
        gameBus.addListener((PlaySoundEvent e) -> {
            SoundInstance sound = e.getSound();
            if (sound == null || sound instanceof LegacyAttenuation || !legacyNamespace(sound.getLocation())) return;
            e.setSound(wrap(sound));
        });
    }

    private static volatile Set<String> namespaces;

    /** Whether a sound event belongs to a legacy mod: its asset namespaces, or a sound file played for one ({@code legacyconnector:legacy_file/...}). */
    static boolean legacyNamespace(ResourceLocation location) {
        if (location == null) return false;
        String ns = location.getNamespace();
        if (ns.equals("legacyconnector")) return location.getPath().startsWith("legacy_file/");
        if (ns.equals("minecraft")) return false;
        Set<String> known = namespaces;
        if (known == null) namespaces = known = collectNamespaces();
        return known.contains(ns);
    }

    private static Set<String> collectNamespaces() {
        Set<String> out = new HashSet<>();
        for (var mod : LegacyMods.all()) {
            try {
                if (mod.modernId != null) out.add(mod.modernId);
                if (mod.legacyId != null) out.add(org.kylepitcock.legacyconnector.core.Ids.namespace(mod.legacyId));
                Path assets = mod.container == null ? null : mod.container.getModInfo().getOwningFile().getFile().findResource("assets");
                if (assets != null && Files.isDirectory(assets)) {
                    try (var dirs = Files.list(assets)) {
                        dirs.filter(Files::isDirectory).forEach(d -> out.add(d.getFileName().toString().replace("/", "")));
                    }
                }
            } catch (Exception | LinkageError e) {
                // a mod whose file cannot be listed keeps the namespaces found so far
            }
        }
        out.remove("minecraft");
        return Set.copyOf(out);
    }

    /** 1.7.10's linear gain at the listener's distance: 1 at the source, 0 at 16 blocks times the volume (when above 1). */
    private float gain() {
        Sound sound = inner.getSound();
        float base = sound == null ? 16.0F : sound.getAttenuationDistance();
        float range = Math.max(sound == null ? 1.0F : inner.getVolume(), 1.0F) * base;
        Vec3 listener;
        try {
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            if (!camera.isInitialized()) return 1.0F;
            listener = camera.getPosition();
        } catch (RuntimeException e) {
            return 1.0F;
        }
        double dx = inner.getX() - listener.x, dy = inner.getY() - listener.y, dz = inner.getZ() - listener.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 0.0D) return 1.0F;
        if (distance >= range) return 0.0F;
        return (float) (1.0D - distance / range);
    }

    /**
     * 1.7.10's source volume: the sound's volume times its category's slider, capped at 1, times the gain. The modern engine
     * multiplies what this returns by the slider again (and caps it), so the slider is divided out here.
     */
    @Override
    public float getVolume() {
        if (inner.getSound() == null) return 0.0F; // not resolved yet (the engine resolves before it asks)
        float volume = inner.getVolume();
        SoundSource source = inner.getSource();
        float slider = source == null || source == SoundSource.MASTER ? 1.0F : Minecraft.getInstance().options.getSoundSourceVolume(source);
        if (slider <= 0.0F || volume <= 0.0F) return 0.0F;
        return Math.min(volume * slider, 1.0F) * gain() / slider / divisor;
    }

    @Override
    public SoundInstance.Attenuation getAttenuation() {
        return SoundInstance.Attenuation.NONE;
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    /** A loop out of range starts silent and fades in as the listener comes near (paulscode kept such sources playing). */
    @Override
    public boolean canStartSilent() {
        return inner.isLooping() || inner.canStartSilent();
    }

    @Override
    public void tick() {
        if (inner instanceof TickableSoundInstance t) {
            t.tick();
            if (t.isStopped()) stopped = true;
        }
    }

    @Override
    public boolean isStopped() {
        return stopped || inner instanceof TickableSoundInstance t && t.isStopped();
    }

    @Override
    public boolean canPlaySound() {
        return inner.canPlaySound();
    }

    @Override
    public ResourceLocation getLocation() {
        return inner.getLocation();
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        return inner.resolve(manager);
    }

    @Override
    public Sound getSound() {
        return inner.getSound();
    }

    @Override
    public SoundSource getSource() {
        return inner.getSource();
    }

    @Override
    public boolean isLooping() {
        return inner.isLooping();
    }

    @Override
    public int getDelay() {
        return inner.getDelay();
    }

    @Override
    public float getPitch() {
        return inner.getPitch();
    }

    @Override
    public double getX() {
        return inner.getX();
    }

    @Override
    public double getY() {
        return inner.getY();
    }

    @Override
    public double getZ() {
        return inner.getZ();
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
        return inner.getStream(buffers, sound, looping);
    }

    /** Divides the volume by {@code amount} (a dimension that muffles sound). */
    public void mcc$divide(float amount) {
        if (amount > 0.0F) divisor *= amount;
    }

    /** The sound this one plays with the legacy fall-off. */
    public SoundInstance mcc$inner() {
        return inner;
    }
}
