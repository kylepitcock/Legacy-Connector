package org.kylepitcock.mccompat.core.pack;

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
 * and code-registered translations. Always enabled and placed on top.
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

    private GeneratedPack(PackLocationInfo location, PackType type) {
        this.location = location;
        this.type = type;
        if (type == PackType.CLIENT_RESOURCES) GeneratedTranslations.contribute(files);
        for (Contributor c : CONTRIBUTORS) {
            try {
                c.contribute(type, files);
            } catch (RuntimeException | LinkageError e) {
                LoggerFactory.getLogger("MCCompat/Pack").error("Generated pack contributor {} failed", c, e);
            }
        }
        LoggerFactory.getLogger("MCCompat/Pack").info("Generated {} {} file(s) for legacy mods", files.size(), type == PackType.SERVER_DATA ? "data" : "asset");
    }

    public static void addContributor(Contributor c) {
        CONTRIBUTORS.add(c);
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        PackType type = event.getPackType();
        String id = type == PackType.SERVER_DATA ? "mccompat_legacy_data" : "mccompat_legacy_assets";
        PackLocationInfo info = new PackLocationInfo(id, Component.literal("MC Compatibility: legacy mod content"), PackSource.BUILT_IN, Optional.empty());
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo location) {
                return new GeneratedPack(location, type);
            }

            @Override
            public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                return new GeneratedPack(location, type);
            }
        };
        Pack pack = Pack.readMetaAndCreate(info, supplier, type, new PackSelectionConfig(true, Pack.Position.TOP, false));
        if (pack != null) event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    private static IoSupplier<InputStream> stream(Supplier<byte[]> data) {
        return () -> new ByteArrayInputStream(data.get());
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length == 1 && elements[0].equals(PACK_META)) {
            int format = SharedConstants.getCurrentVersion().getPackVersion(type);
            byte[] meta = ("{\"pack\":{\"pack_format\":" + format + ",\"description\":\"MC Compatibility legacy content\"}}").getBytes(StandardCharsets.UTF_8);
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
            return (T) new PackMetadataSection(Component.literal("MC Compatibility legacy content"), SharedConstants.getCurrentVersion().getPackVersion(type));
        }
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return location;
    }

    @Override
    public void close() {
    }
}
