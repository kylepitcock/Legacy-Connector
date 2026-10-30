package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/**
 * 1.12's paulscode sound system as mods drive it by source name ({@code SoundManager.sndSystem}): a named source made from a URL plays as a
 * streamed modern sound whose position and volume follow the source. Only Ogg Vorbis data decodes (the game's decoder); a source the
 * game cannot decode stays silent.
 */
public final class LegacySoundSystem1122 {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Sound");
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_source");
    private static final Map<String, Source> SOURCES = new ConcurrentHashMap<>();

    private LegacySoundSystem1122() {}

    /** A paulscode source: where its data is and how it plays. */
    private static final class Source {
        final URL url;
        final boolean loop;
        final boolean linear;
        final float distance;
        volatile float x, y, z;
        volatile float volume = 1.0F;
        volatile float pitch = 1.0F;
        volatile Instance playing;

        Source(URL url, boolean loop, float x, float y, float z, int attModel, float distOrRoll) {
            this.url = url;
            this.loop = loop;
            this.x = x;
            this.y = y;
            this.z = z;
            this.linear = attModel == 2;
            this.distance = attModel == 2 && distOrRoll > 0 ? distOrRoll : 16.0F;
        }
    }

    /** The modern sound playing a source: streams the URL, follows the source's position and volume every tick. */
    private static final class Instance extends AbstractTickableSoundInstance {
        private final Source src;

        Instance(Source src) {
            super(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ID), SoundSource.RECORDS, RandomSource.create());
            this.src = src;
            this.looping = src.loop;
            this.attenuation = src.linear ? SoundInstance.Attenuation.LINEAR : SoundInstance.Attenuation.NONE;
            this.relative = false;
            sync();
        }

        private void sync() {
            this.x = src.x;
            this.y = src.y;
            this.z = src.z;
            this.volume = src.volume;
            this.pitch = src.pitch;
        }

        @Override
        public void tick() {
            if (src.playing != this) {
                stop();
                return;
            }
            sync();
        }

        @Override
        public WeighedSoundEvents resolve(net.minecraft.client.sounds.SoundManager manager) {
            this.sound = new Sound(ID, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, (int) src.distance);
            return new WeighedSoundEvents(ID, null);
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    InputStream in = src.url.openStream();
                    return looping ? new LoopingAudioStream(JOrbisAudioStream::new, in) : new JOrbisAudioStream(in);
                } catch (Exception e) {
                    LOGGER.warn("A legacy sound source could not be played from {}: {}", src.url, e.toString());
                    throw new java.util.concurrent.CompletionException(e);
                }
            }, net.minecraft.Util.backgroundExecutor());
        }
    }

    public static void newSource(boolean priority, String name, URL url, String identifier, boolean loop, float x, float y, float z, int attModel, float distOrRoll) {
        if (name == null || url == null) return;
        removeSource(name);
        SOURCES.put(name, new Source(url, loop, x, y, z, attModel, distOrRoll));
    }

    public static void play(String name) {
        Source src = name == null ? null : SOURCES.get(name);
        if (src == null) return;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            Instance old = src.playing;
            if (old != null && mc.getSoundManager().isActive(old)) return;
            Instance inst = new Instance(src);
            src.playing = inst;
            mc.getSoundManager().play(inst);
        });
    }

    public static boolean playing(String name) {
        Source src = name == null ? null : SOURCES.get(name);
        Instance inst = src == null ? null : src.playing;
        return inst != null && Minecraft.getInstance().getSoundManager().isActive(inst);
    }

    public static void stop(String name) {
        Source src = name == null ? null : SOURCES.get(name);
        if (src == null) return;
        Instance inst = src.playing;
        src.playing = null;
        if (inst != null) Minecraft.getInstance().execute(() -> Minecraft.getInstance().getSoundManager().stop(inst));
    }

    public static void removeSource(String name) {
        if (name == null) return;
        stop(name);
        SOURCES.remove(name);
    }

    public static void setPosition(String name, float x, float y, float z) {
        Source src = name == null ? null : SOURCES.get(name);
        if (src == null) return;
        src.x = x;
        src.y = y;
        src.z = z;
    }

    public static void setVolume(String name, float volume) {
        Source src = name == null ? null : SOURCES.get(name);
        if (src != null) src.volume = Math.max(0.0F, volume);
    }

    public static void setPitch(String name, float pitch) {
        Source src = name == null ? null : SOURCES.get(name);
        if (src != null) src.pitch = pitch;
    }
}
