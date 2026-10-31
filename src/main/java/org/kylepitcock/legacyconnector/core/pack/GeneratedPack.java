package org.kylepitcock.legacyconnector.core.pack;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.LoggerFactory;

/**
 * An in-memory resource/data pack whose content is computed from the loaded legacy mods every time packs reload:
 * translated recipes, ore dictionary tags, mining tags, world generation hooks, and (client) block states, models
 * and code-registered translations. Always enabled and placed on top, except the translated recipes (and the item tags
 * they use): those are a second pack placed at the bottom, so world data packs and KubeJS replace or remove a legacy
 * recipe by its id like any mod's recipe.
 */
public final class GeneratedPack implements PackResources {
    /** Produces files for one pack type, keyed by full path ({@code data/ns/recipe/x.json}). */
    public interface Contributor {
        void contribute(PackType type, Map<String, Supplier<byte[]>> out);
    }

    private static final List<Contributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

    private final PackLocationInfo location;
    private final PackType type;
    private final Map<String, Supplier<byte[]>> files = new TreeMap<>();

    private GeneratedPack(PackLocationInfo location, PackType type, boolean recipes) {
        this.location = location;
        this.type = type;
        if (type == PackType.CLIENT_RESOURCES) GeneratedTranslations.contribute(files);
        List<Contributor> all = new java.util.ArrayList<>(recipes ? RECIPE_CONTRIBUTORS : CONTRIBUTORS);
        if (!recipes) all.addAll(LATE_CONTRIBUTORS);
        BUILDING.incrementAndGet();
        try {
            for (Contributor c : all) {
                try {
                    c.contribute(type, files);
                } catch (RuntimeException | LinkageError e) {
                    LoggerFactory.getLogger("LegacyConnector/Pack").error("Generated pack contributor {} failed", c, e);
                }
            }
        } finally {
            BUILDING.decrementAndGet();
        }
        LoggerFactory.getLogger("LegacyConnector/Pack").info("Generated {} {} file(s) for legacy mods", files.size(), type == PackType.SERVER_DATA ? "data" : "asset");
    }

    private static final java.util.concurrent.atomic.AtomicInteger BUILDING = new java.util.concurrent.atomic.AtomicInteger();

    /**
     * Whether a generated pack is being built right now. Legacy mods' TextureStitchEvent.Pre handlers run then (the sprites they
     * register become atlas sources), before the resources being reloaded are visible: a texture they bind or load at that
     * moment (OpenComputers preloads its model textures) could not be found (FileNotFoundException logged for files that exist).
     */
    public static boolean building() {
        return BUILDING.get() > 0;
    }

    public static void addContributor(Contributor c) {
        CONTRIBUTORS.add(c);
    }

    private static final List<Contributor> LATE_CONTRIBUTORS = new CopyOnWriteArrayList<>();

    private static final List<Contributor> RECIPE_CONTRIBUTORS = new CopyOnWriteArrayList<>();

    /** A contributor of translated recipes and the item tags they use (server data only), served by the bottom pack. */
    public static void addRecipeContributor(Contributor c) {
        RECIPE_CONTRIBUTORS.add(c);
    }

    /** A contributor that runs after all others (it reads what they generated or collected, e.g. sprites for the block atlas). */
    public static void addLateContributor(Contributor c) {
        LATE_CONTRIBUTORS.add(c);
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        PackType type = event.getPackType();
        String id = type == PackType.SERVER_DATA ? "legacyconnector_legacy_data" : "legacyconnector_legacy_assets";
        addPack(event, id, "Legacy Connector: legacy mod content", type, false, Pack.Position.TOP);
        if (type == PackType.SERVER_DATA) addPack(event, "legacyconnector_legacy_recipes", "Legacy Connector: legacy mod recipes", type, true, Pack.Position.BOTTOM);
    }

    private static void addPack(AddPackFindersEvent event, String id, String title, PackType type, boolean recipes, Pack.Position position) {
        PackLocationInfo info = new PackLocationInfo(id, Component.literal(title), PackSource.BUILT_IN, Optional.empty());
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo location) {
                return new GeneratedPack(location, type, recipes);
            }

            @Override
            public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                return new GeneratedPack(location, type, recipes);
            }
        };
        Pack pack = Pack.readMetaAndCreate(info, supplier, type, new PackSelectionConfig(true, position, false));
        if (pack != null) event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    private static IoSupplier<InputStream> stream(Supplier<byte[]> data) {
        return () -> new ByteArrayInputStream(data.get());
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length == 1 && elements[0].equals(PACK_META)) {
            int format = SharedConstants.getCurrentVersion().getPackVersion(type);
            byte[] meta = ("{\"pack\":{\"pack_format\":" + format + ",\"description\":\"Legacy Connector legacy content\"}}").getBytes(StandardCharsets.UTF_8);
            return () -> new ByteArrayInputStream(meta);
        }
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        if (packType != type) return null;
        Supplier<byte[]> data = files.get(packType.getDirectory() + "/" + location.getNamespace() + "/" + location.getPath());
        return data == null ? null : stream(data);
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput output) {
        if (packType != type) return;
        String prefix = packType.getDirectory() + "/" + namespace + "/" + path + "/";
        String root = packType.getDirectory() + "/" + namespace + "/";
        for (var e : files.entrySet()) {
            if (!e.getKey().startsWith(prefix)) continue;
            ResourceLocation rl = ResourceLocation.tryBuild(namespace, e.getKey().substring(root.length()));
            if (rl != null) output.accept(rl, stream(e.getValue()));
        }
    }

    @Override
    public Set<String> getNamespaces(PackType packType) {
        Set<String> out = new HashSet<>();
        if (packType != type) return out;
        String dir = packType.getDirectory() + "/";
        for (String k : files.keySet()) {
            int slash = k.indexOf('/', dir.length());
            if (k.startsWith(dir) && slash > 0) out.add(k.substring(dir.length(), slash));
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        if (serializer.getMetadataSectionName().equals("pack")) {
            return (T) new PackMetadataSection(Component.literal("Legacy Connector legacy content"), SharedConstants.getCurrentVersion().getPackVersion(type));
        }
        if (serializer.getMetadataSectionName().equals(net.minecraft.server.packs.resources.ResourceFilterSection.TYPE.getMetadataSectionName())) {
            return (T) blockstateFilter();
        }
        return null;
    }

    /**
     * A pack filter hiding the legacy mods' own blockstate files that this pack replaces. Modern Minecraft loads the blockstate file of
     * every pack and merges them, bottom pack first, and one file that fails to parse aborts the whole stack: a 1.12 Forge blockstate
     * ("forge_marker": 1 with an "inventory": [{}] variant that takes its model from "defaults") made the block's translated
     * {@code meta=N} file in this pack unread, so the block drew as the missing model (Scape and Run: Parasites' gore piles "goresim",
     * stairs, double slabs, bushes; #110). The translation is complete, so the legacy file below is dropped. Vanilla's own namespace is
     * never filtered.
     */
    private net.minecraft.server.packs.resources.ResourceFilterSection blockstateFilter() {
        if (type != PackType.CLIENT_RESOURCES) return null;
        Map<String, StringBuilder> names = new TreeMap<>();
        String dir = type.getDirectory() + "/";
        for (String k : files.keySet()) {
            if (!k.startsWith(dir) || !k.endsWith(".json")) continue;
            int slash = k.indexOf('/', dir.length());
            if (slash < 0 || !k.startsWith("/blockstates/", slash) || k.indexOf('/', slash + "/blockstates/".length()) >= 0) continue;
            String namespace = k.substring(dir.length(), slash);
            if (namespace.equals("minecraft")) continue;
            String name = k.substring(slash + "/blockstates/".length(), k.length() - ".json".length());
            StringBuilder sb = names.computeIfAbsent(namespace, n -> new StringBuilder());
            if (sb.length() > 0) sb.append('|');
            sb.append(java.util.regex.Pattern.quote(name));
        }
        if (names.isEmpty()) return null;
        com.google.gson.JsonArray block = new com.google.gson.JsonArray();
        for (var e : names.entrySet()) {
            com.google.gson.JsonObject pattern = new com.google.gson.JsonObject();
            pattern.addProperty("namespace", "^" + java.util.regex.Pattern.quote(e.getKey()) + "$");
            pattern.addProperty("path", "^blockstates/(?:" + e.getValue() + ")\\.json$");
            block.add(pattern);
        }
        com.google.gson.JsonObject section = new com.google.gson.JsonObject();
        section.add("block", block);
        try {
            return net.minecraft.server.packs.resources.ResourceFilterSection.TYPE.fromJson(section);
        } catch (RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Pack").warn("Could not build the blockstate filter of the generated pack", e);
            return null;
        }
    }

    @Override
    public PackLocationInfo location() {
        return location;
    }

    @Override
    public void close() {
    }
}
