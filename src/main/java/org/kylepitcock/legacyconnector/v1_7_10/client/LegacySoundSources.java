package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.SoundBuffer;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The named sources of 1.7.10's paulscode {@code SoundSystem} (IC2's machine sounds) on the modern sound engine: each
 * source plays an OGG file by URL on a channel of the engine's static pool (so the engine's limits, pausing and listener
 * apply), with paulscode's linear or roll-off attenuation, looping, volume and pitch. Decoded files are cached; the
 * OpenAL buffers are rebuilt after the engine reloads.
 */
public final class LegacySoundSources {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/SoundSystem");
    private static final Map<String, Decoded> DECODED = new ConcurrentHashMap<>();
    /** OpenAL buffers by file; only touched on the sound thread. */
    private static final Map<String, SoundBuffer> BUFFERS = new ConcurrentHashMap<>();
    private static final java.util.Set<String> FAILED = ConcurrentHashMap.newKeySet();
    private static Field engineField;
    private static Field accessField;
    private static Field loadedField;
    private static boolean reflectionFailed;
    private static volatile SoundEngine lastEngine;

    private LegacySoundSources() {}

    private record Decoded(ByteBuffer pcm, AudioFormat format) {}

    /** The engine reloaded (resource reload, device change): its OpenAL context and every buffer in it are new. */
    public static void engineReloaded(SoundEngine engine) {
        lastEngine = engine;
        BUFFERS.clear();
    }

    private static SoundEngine engine() {
        if (reflectionFailed) return null;
        try {
            if (engineField == null) {
                engineField = net.minecraft.client.sounds.SoundManager.class.getDeclaredField("soundEngine");
                engineField.setAccessible(true);
                accessField = SoundEngine.class.getDeclaredField("channelAccess");
                accessField.setAccessible(true);
                loadedField = SoundEngine.class.getDeclaredField("loaded");
                loadedField.setAccessible(true);
            }
            if (lastEngine != null) return lastEngine;
            // the engine's load event fires inside its constructor, before Minecraft has a sound manager
            var manager = Minecraft.getInstance().getSoundManager();
            return manager == null ? null : (SoundEngine) engineField.get(manager);
        } catch (ReflectiveOperationException | RuntimeException e) {
            reflectionFailed = true;
            LOG.error("Cannot reach the sound engine; legacy sound sources stay silent", e);
            return null;
        }
    }

    /** Whether the modern engine has an open device (what 1.7.10's {@code SoundManager.loaded} reported). */
    public static boolean engineLoaded() {
        SoundEngine engine = engine();
        try {
            return engine != null && loadedField.getBoolean(engine);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static ChannelAccess access() {
        SoundEngine engine = engine();
        try {
            return engine != null && loadedField.getBoolean(engine) ? (ChannelAccess) accessField.get(engine) : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** The OpenAL buffer of a file (sound thread): decoded once, uploaded once per engine load. */
    private static SoundBuffer buffer(URL url, String identifier) {
        String key = url.toString();
        if (FAILED.contains(key)) return null;
        SoundBuffer cached = BUFFERS.get(key);
        if (cached != null) return cached;
        Decoded decoded = DECODED.get(key);
        if (decoded == null) {
            if (identifier != null && !identifier.toLowerCase(java.util.Locale.ROOT).endsWith(".ogg") && !key.toLowerCase(java.util.Locale.ROOT).endsWith(".ogg")) {
                FAILED.add(key);
                LOG.warn("Only OGG files can be played; {} is skipped", identifier);
                return null;
            }
            try (InputStream in = url.openStream(); JOrbisAudioStream stream = new JOrbisAudioStream(in)) {
                decoded = new Decoded(stream.readAll(), stream.getFormat());
                DECODED.put(key, decoded);
            } catch (Exception e) {
                FAILED.add(key);
                LOG.warn("Cannot decode sound {}: {}", key, e.toString());
                return null;
            }
        }
        SoundBuffer buffer = new SoundBuffer(decoded.pcm().duplicate(), decoded.format());
        BUFFERS.put(key, buffer);
        return buffer;
    }

    /** One named source. All its methods are called from game threads; channel work runs on the sound thread. */
    public static final class Source {
        final URL url;
        final String identifier;
        final boolean temporary;
        public volatile boolean loop;
        public volatile float x, y, z;
        public volatile int attenuation;
        public volatile float distOrRoll;
        public volatile float volume = 1.0F;
        public volatile float pitch = 1.0F;
        private CompletableFuture<ChannelAccess.ChannelHandle> handle;
        private boolean culled;
        private boolean resumeOnActivate;

        public Source(URL url, String identifier, boolean loop, float x, float y, float z, int attenuation, float distOrRoll, boolean temporary) {
            this.url = url;
            this.identifier = identifier;
            this.loop = loop;
            this.x = x;
            this.y = y;
            this.z = z;
            this.attenuation = attenuation;
            this.distOrRoll = distOrRoll;
            this.temporary = temporary;
        }

        private boolean live() {
            if (handle == null || !handle.isDone()) return handle != null;
            ChannelAccess.ChannelHandle h = handle.getNow(null);
            return h != null && !h.isStopped();
        }

        /** Runs on the channel if one is held (not acquiring one). */
        private void onChannel(Consumer<Channel> action) {
            if (handle == null) return;
            handle.thenAccept(h -> {
                if (h != null && !h.isStopped()) h.execute(action);
            });
        }

        /** The gain OpenAL gets: the source's volume, paulscode's master gain and the game's master volume. */
        private float gain(float master) {
            float game = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
            return Math.max(0.0F, volume * master * game);
        }

        private void apply(Channel ch, float master) {
            ch.setRelative(false);
            ch.setSelfPosition(new Vec3(x, y, z));
            ch.setLooping(loop);
            ch.setPitch(pitch);
            ch.setVolume(gain(master));
            switch (attenuation) {
                case 2 -> ch.linearAttenuation(Math.max(1.0F, distOrRoll));
                case 1 -> ch.linearAttenuation(Math.max(1.0F, Math.min(256.0F, distOrRoll > 0 ? 1.0F / distOrRoll : 16.0F)));
                default -> ch.disableAttenuation();
            }
        }

        public synchronized void play(float master) {
            if (culled) {
                if (loop) resumeOnActivate = true;
                return;
            }
            if (live()) {
                // as paulscode: a source that is already playing goes on (resumes when paused)
                onChannel(ch -> {
                    apply(ch, master);
                    if (!ch.playing()) ch.play();
                });
                return;
            }
            ChannelAccess access = access();
            if (access == null) return;
            handle = access.createHandle(Library.Pool.STATIC);
            URL u = url;
            String id = identifier;
            onChannel(ch -> {
                SoundBuffer buffer = buffer(u, id);
                if (buffer == null) {
                    ch.stop();
                    return;
                }
                ch.attachStaticBuffer(buffer);
                apply(ch, master);
                ch.play();
            });
        }

        public synchronized void pause() {
            onChannel(Channel::pause);
        }

        /** Stops and rewinds: the channel is given back, the next play starts from the beginning on a new one. */
        public synchronized void stop() {
            onChannel(Channel::stop);
            handle = null;
        }

        public synchronized void cull() {
            if (culled) return;
            resumeOnActivate = loop && live();
            stop();
            culled = true;
        }

        public synchronized void activate(float master) {
            if (!culled) return;
            culled = false;
            if (resumeOnActivate) {
                resumeOnActivate = false;
                play(master);
            }
        }

        public synchronized boolean playing() {
            return !culled && live();
        }

        public synchronized boolean finished() {
            return handle == null || handle.isDone() && !live();
        }

        /** A quick-play source that has finished (or never got a channel). */
        public synchronized boolean temporaryDone() {
            return temporary && finished();
        }

        public synchronized void update(float master) {
            onChannel(ch -> apply(ch, master));
        }
    }
}
