package org.kylepitcock.legacyconnector.v1_7_10;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import legacyconnector.v1_7_10.cpw.mods.fml.common.Loader;
import legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourcePack;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The packs legacy mods supply through {@code ModContainer.getCustomResourcePackClass()}. FML built a mod's pack from that
 * class instead of a plain one over its jar; mods use it to serve assets that are not in the jar (Fisk's Heroes serves the
 * sound files and sounds.json it downloads). The jar's own assets are already in the game, translated, so this pack serves
 * only what a custom pack has beyond them. It cannot list folders (legacy packs cannot either), so it serves files by name:
 * sounds.json, sounds and textures that are asked for by name. Packs mods add while running (see
 * {@code LegacyResourcePacks}) are served here as well, whole.
 */
public final class LegacyModPacks extends AbstractPackResources {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/ModPacks");
    /** Custom pack and the plain pack over the same jar, by container, kept across reloads. */
    private static final Map<ModContainer, IResourcePack[]> PACKS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Containers a mod added with {@code FMLCommonHandler.addModToResourcePack} (scripted mods): their whole source is served. */
    private static final List<ModContainer> ADDED = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Serves the assets of {@code container}'s source (a jar or folder), as FML's {@code addModToResourcePack} did. */
    public static void mcc$addContainer(ModContainer container) {
        if (container != null && !ADDED.contains(container)) ADDED.add(container);
    }

    private LegacyModPacks(PackLocationInfo location) {
        super(location);
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        PackLocationInfo info = new PackLocationInfo("legacyconnector_mod_packs", Component.literal("Legacy Connector: legacy mod packs"), PackSource.BUILT_IN, Optional.empty());
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo location) {
                return new LegacyModPacks(location);
            }

            @Override
            public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                return new LegacyModPacks(location);
            }
        };
        Pack pack = Pack.readMetaAndCreate(info, supplier, PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.BOTTOM, false));
        if (pack != null) event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    /** Each container's custom pack (and the plain one over its jar), made on first use. */
    private static List<IResourcePack[]> packs() {
        List<IResourcePack[]> out = new ArrayList<>();
        List<ModContainer> containers;
        try {
            containers = Loader.instance().getModList();
        } catch (RuntimeException | LinkageError e) {
            return out;
        }
        for (ModContainer container : containers) {
            Class<?> cls;
            try {
                cls = container.getCustomResourcePackClass();
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            if (cls == null || cls == legacyconnector.v1_7_10.cpw.mods.fml.client.FMLFileResourcePack.class
                    || cls == legacyconnector.v1_7_10.cpw.mods.fml.client.FMLFolderResourcePack.class) continue;
            IResourcePack[] pair = PACKS.computeIfAbsent(container, c -> {
                try {
                    IResourcePack custom = (IResourcePack) cls.getConstructor(ModContainer.class).newInstance(c);
                    IResourcePack plain = c.getSource() == null ? null : c.getSource().isDirectory()
                            ? new legacyconnector.v1_7_10.cpw.mods.fml.client.FMLFolderResourcePack(c)
                            : new legacyconnector.v1_7_10.cpw.mods.fml.client.FMLFileResourcePack(c);
                    LOG.info("Serving the custom resource pack {} of {}", cls.getName(), c.getModId());
                    return new IResourcePack[] {custom, plain};
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    LOG.warn("The custom resource pack {} of {} could not be made: {}", cls.getName(), c.getModId(), e.toString());
                    return new IResourcePack[0];
                }
            });
            if (pair.length == 2) out.add(pair);
        }
        for (ModContainer container : ADDED) {
            IResourcePack[] pair = PACKS.computeIfAbsent(container, c -> {
                if (c.getSource() == null) return new IResourcePack[0];
                IResourcePack plain = c.getSource().isDirectory() ? new legacyconnector.v1_7_10.cpw.mods.fml.client.FMLFolderResourcePack(c)
                        : new legacyconnector.v1_7_10.cpw.mods.fml.client.FMLFileResourcePack(c);
                return new IResourcePack[] {plain, null};
            });
            if (pair.length == 2) out.add(pair);
        }
        return out;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length == 1 && elements[0].equals(PACK_META)) {
            int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
            byte[] meta = ("{\"pack\":{\"pack_format\":" + format + ",\"description\":\"Legacy mods' own packs\"}}").getBytes(StandardCharsets.UTF_8);
            return () -> new ByteArrayInputStream(meta);
        }
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES) return null;
        var legacy = new legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation(location.getNamespace(), location.getPath());
        for (IResourcePack[] pair : packs()) {
            try {
                if (!pair[0].resourceExists(legacy) || pair[1] != null && pair[1].resourceExists(legacy)) continue;
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            return serve(pair[0], legacy, location);
        }
        // packs mods added at run time (reloadResourcePack, Minecraft.defaultResourcePacks), newest first
        List<IResourcePack> added = org.kylepitcock.legacyconnector.v1_7_10.client.LegacyResourcePacks.injected();
        for (int i = added.size() - 1; i >= 0; i--) {
            try {
                if (added.get(i).resourceExists(legacy)) return serve(added.get(i), legacy, location);
            } catch (RuntimeException | LinkageError ignored) {
                // a broken pack serves nothing
            }
        }
        return null;
    }

    private static IoSupplier<InputStream> serve(IResourcePack pack, legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation legacy, ResourceLocation location) {
        boolean sounds = location.getPath().equals("sounds.json");
        return () -> {
            InputStream in = pack.getInputStream(legacy);
            if (in == null) throw new IOException("No " + location);
            if (!sounds) return in;
            try (in) {
                return new ByteArrayInputStream(convertSounds(in.readAllBytes(), location.getNamespace()));
            }
        };
    }

    /**
     * As the loader converts the sounds.json in mod jars: a sound named without a domain was the file's own domain's in
     * 1.7.10 (the modern game reads it as minecraft's), and "category" is gone.
     */
    static byte[] convertSounds(byte[] data, String namespace) {
        try {
            var root = com.google.gson.JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : root.entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                var event = e.getValue().getAsJsonObject();
                event.remove("category");
                if (!(event.get("sounds") instanceof com.google.gson.JsonArray list)) continue;
                for (int i = 0; i < list.size(); i++) {
                    var sound = list.get(i);
                    if (sound.isJsonPrimitive()) {
                        list.set(i, new com.google.gson.JsonPrimitive(withDomain(sound.getAsString(), namespace)));
                    } else if (sound instanceof com.google.gson.JsonObject o && o.has("name")) {
                        o.addProperty("name", withDomain(o.get("name").getAsString(), namespace));
                    }
                }
            }
            return root.toString().getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return data;
        }
    }

    private static String withDomain(String name, String namespace) {
        return name.indexOf(':') < 0 ? namespace + ":" + name : name;
    }

    /**
     * The game only plays sound files it found by listing {@code sounds/}. A legacy pack cannot list, but its sounds.json
     * names its sound files, so those are listed (the ones only the custom pack has).
     */
    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !(path.equals("sounds") || path.startsWith("sounds/"))) return;
        IoSupplier<InputStream> json = getResource(type, ResourceLocation.fromNamespaceAndPath(namespace, "sounds.json"));
        if (json == null) return;
        com.google.gson.JsonObject root;
        try (InputStream in = json.get()) {
            root = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            return;
        }
        Set<String> listed = new HashSet<>();
        for (var e : root.entrySet()) {
            if (!(e.getValue() instanceof com.google.gson.JsonObject event) || !(event.get("sounds") instanceof com.google.gson.JsonArray list)) continue;
            for (var sound : list) {
                String name = sound.isJsonPrimitive() ? sound.getAsString()
                        : sound instanceof com.google.gson.JsonObject o && o.has("name") && !"event".equals(o.has("type") ? o.get("type").getAsString() : "") ? o.get("name").getAsString() : null;
                ResourceLocation id = name == null ? null : ResourceLocation.tryParse(name);
                if (id == null || !id.getNamespace().equals(namespace)) continue;
                ResourceLocation file = id.withPath(p -> "sounds/" + p + ".ogg");
                if (!file.getPath().startsWith(path) || !listed.add(file.getPath())) continue;
                IoSupplier<InputStream> data = getResource(type, file);
                if (data != null) output.accept(file, data);
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        Set<String> out = new HashSet<>();
        if (type != PackType.CLIENT_RESOURCES) return out;
        for (IResourcePack[] pair : packs()) {
            try {
                Set<?> domains = pair[0].getResourceDomains();
                if (domains != null) for (Object d : domains) {
                    if (d instanceof String s && ResourceLocation.isValidNamespace(s)) out.add(s);
                }
            } catch (RuntimeException | LinkageError ignored) {
                // a broken pack serves nothing
            }
        }
        for (String d : org.kylepitcock.legacyconnector.v1_7_10.client.LegacyResourcePacks.domains()) {
            if (ResourceLocation.isValidNamespace(d)) out.add(d);
        }
        return out;
    }

    @Override
    public void close() {
    }
}
