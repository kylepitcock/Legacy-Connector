package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import legacyconnector.v1_7_10.net.minecraft.client.audio.ISound;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResource;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.valueproviders.ConstantFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy sounds that name a sound file rather than a sound event. 1.7.10's sound registry held {@code SoundPoolEntry}s
 * whose location was a file the sound system opened through the resource manager ({@code mcsounddomain:} URLs), and mods
 * registered entries of their own outside any sounds.json: DragonAPI (ChromatiCraft, RotaryCraft, ReactorCraft...) registers
 * every sound as {@code custom_path:Reika/ChromatiCraft/Sounds/gui4.ogg}, puts its {@code DirectResourceManager} into the
 * resource manager's {@code domainResourceManagers} under {@code custom_path}, and that manager reads the path from the
 * class path (case sensitive, as in the jar). The modern engine only plays registered events whose files it listed at the
 * last reload, so such a sound is found here (as 1.7.10 would have found it, then on the class path, then by name without
 * regard to case), registered as a sound file under {@code legacyconnector:legacy_file/...} in the sound manager's file
 * cache, and played as a one-sound event built for it.
 */
public final class LegacySoundFiles {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Sound");
    /** Files larger than this are streamed (music, long loops) rather than decoded whole into a buffer. */
    private static final int STREAM_BYTES = 256 * 1024;
    /** By legacy {@code domain:path}; empty when no file was found (mods ask for absent names repeatedly). */
    private static final Map<String, Optional<LegacyFile>> FILES = new ConcurrentHashMap<>();
    /** Lower-cased jar entry -> file, over every mod file, made on the first lookup that needs it. */
    private static volatile Map<String, Path> lowerCaseIndex;
    private static Field soundCacheField;
    private static boolean soundCacheMissing;

    private LegacySoundFiles() {}

    /** A sound file a legacy sound names: its modern id, the one-sound event playing it, and where its data is. */
    public static final class LegacyFile {
        private final ResourceLocation id;
        private final Sound sound;
        private final IoSupplier<InputStream> data;

        LegacyFile(ResourceLocation id, boolean stream, IoSupplier<InputStream> data) {
            this.id = id;
            this.sound = new Sound(id, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, stream, false, 16);
            this.data = data;
        }

        public ResourceLocation id() {
            return id;
        }

        public Sound sound() {
            return sound;
        }

        /**
         * The event playing this file. The file is put into the sound manager's file cache first (the engine reads sound data
         * only from there; a resource reload clears it, so it is checked on every play).
         */
        public WeighedSoundEvents resolve(SoundManager manager) {
            Map<ResourceLocation, Resource> cache = soundCache(manager);
            if (cache == null) return null;
            ResourceLocation file = sound.getPath();
            if (!cache.containsKey(file)) cache.put(file, new Resource(null, data));
            return new WeighedSoundEvents(id, null);
        }
    }

    /**
     * The file a legacy sound names, or null when its location is not a file (a sound event, played by name) or the file
     * cannot be found.
     */
    public static LegacyFile find(ISound sound, legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation location) {
        if (location == null) return null;
        String path = location.getResourcePath();
        if (path == null || !path.toLowerCase(Locale.ROOT).endsWith(".ogg")) return null;
        String domain = location.getResourceDomain();
        ClassLoader loader = sound == null ? null : sound.getClass().getClassLoader();
        return FILES.computeIfAbsent(domain + ":" + path, k -> Optional.ofNullable(locate(location, loader))).orElse(null);
    }

    private static LegacyFile locate(legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation location, ClassLoader loader) {
        String domain = location.getResourceDomain();
        String path = location.getResourcePath();
        IoSupplier<InputStream> data = null;
        // As 1.7.10: the domain's own manager (DragonAPI's DirectResourceManager for custom_path), then the game's resources.
        try {
            IResourceManager game = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().getResourceManager();
            if (game instanceof legacyconnector.v1_7_10.net.minecraft.client.resources.SimpleReloadableResourceManager s
                    && s.domainResourceManagers.get(domain) instanceof IResourceManager own && own != game) {
                data = check(() -> stream(own.getResource(location)));
            }
            if (data == null) data = check(() -> stream(game.getResource(location)));
        } catch (RuntimeException | LinkageError e) {
            // no legacy client
        }
        // The class path (DragonAPI's DirectResource reads it so), then any mod file by name without regard to case.
        String entry = path.startsWith("/") ? path.substring(1) : path;
        for (ClassLoader l : new ClassLoader[] {loader, LegacySoundFiles.class.getClassLoader(), Thread.currentThread().getContextClassLoader()}) {
            if (data != null || l == null) continue;
            URL url = l.getResource(entry);
            if (url != null) data = check(url::openStream);
        }
        if (data == null) {
            Path p = lowerCaseIndex().get(entry.toLowerCase(Locale.ROOT));
            if (p == null) p = lowerCaseIndex().get(("assets/" + domain + "/" + entry).toLowerCase(Locale.ROOT));
            final Path file = p;
            if (file != null) data = check(() -> Files.newInputStream(file));
        }
        if (data == null) {
            LOG.warn("Legacy sound file {}:{} was not found", domain, path);
            return null;
        }
        long size;
        try (InputStream in = data.get()) {
            size = in.transferTo(java.io.OutputStream.nullOutputStream());
        } catch (IOException | RuntimeException e) {
            return null;
        }
        String key = (domain + "/" + entry.substring(0, entry.length() - ".ogg".length())).toLowerCase(Locale.ROOT);
        StringBuilder id = new StringBuilder("legacy_file/");
        for (char c : key.toCharArray()) id.append(ResourceLocation.validPathChar(c) ? c : '_');
        return new LegacyFile(ResourceLocation.fromNamespaceAndPath("legacyconnector", id.toString()), size > STREAM_BYTES, data);
    }

    private static InputStream stream(IResource resource) throws IOException {
        InputStream in = resource == null ? null : resource.getInputStream();
        if (in == null) throw new java.io.FileNotFoundException();
        return in;
    }

    /** The supplier when it opens (legacy resources throw unchecked exceptions for missing files), else null. */
    private static IoSupplier<InputStream> check(IoSupplier<InputStream> data) {
        try (InputStream in = data.get()) {
            return in == null ? null : data;
        } catch (IOException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static Map<String, Path> lowerCaseIndex() {
        Map<String, Path> index = lowerCaseIndex;
        if (index != null) return index;
        synchronized (LegacySoundFiles.class) {
            if (lowerCaseIndex != null) return lowerCaseIndex;
            index = new HashMap<>();
            try {
                for (var info : net.neoforged.fml.ModList.get().getModFiles()) {
                    Path root = info.getFile().getSecureJar().getRootPath();
                    try (Stream<Path> files = Files.walk(root)) {
                        for (Path p : (Iterable<Path>) files::iterator) {
                            String name = root.relativize(p).toString().replace('\\', '/');
                            if (name.toLowerCase(Locale.ROOT).endsWith(".ogg")) index.putIfAbsent(name.toLowerCase(Locale.ROOT), p);
                        }
                    } catch (IOException | RuntimeException e) {
                        // an unreadable file has no sounds
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                // no mod list
            }
            lowerCaseIndex = index;
            return index;
        }
    }

    /** The sound manager's file cache, which its engine reads sound data from (private; null if it cannot be reached). */
    @SuppressWarnings("unchecked")
    private static synchronized Map<ResourceLocation, Resource> soundCache(SoundManager manager) {
        if (soundCacheMissing) return null;
        try {
            if (soundCacheField == null) {
                Field found = null;
                try {
                    found = SoundManager.class.getDeclaredField("soundCache");
                } catch (NoSuchFieldException e) {
                    for (Field f : SoundManager.class.getDeclaredFields()) {
                        if (Map.class.isAssignableFrom(f.getType()) && f.getGenericType().getTypeName().contains(Resource.class.getName())) found = f;
                    }
                }
                if (found == null) throw new NoSuchFieldException("soundCache");
                found.setAccessible(true);
                soundCacheField = found;
            }
            return (Map<ResourceLocation, Resource>) soundCacheField.get(manager);
        } catch (ReflectiveOperationException | RuntimeException e) {
            soundCacheMissing = true;
            LOG.warn("Legacy sound files cannot be played: the sound manager's file cache is out of reach ({})", e.toString());
            return null;
        }
    }
}
