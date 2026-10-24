package org.kylepitcock.legacyconnector.core.pack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * 1.7.10 served {@code <game dir>/resources/<domain>/<path>} as assets of {@code domain} (Custom Main Menu keeps its images in
 * {@code resources/mainmenu}). This pack exposes that folder the same way; it only exists when the folder does.
 */
public final class GameResourcesFolderPack extends AbstractPackResources {
    private final Path root;

    private GameResourcesFolderPack(PackLocationInfo location, Path root) {
        super(location);
        this.root = root;
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        Path dir = FMLPaths.GAMEDIR.get().resolve("resources");
        if (!Files.isDirectory(dir)) return;
        PackLocationInfo info = new PackLocationInfo("legacyconnector_resources_folder", Component.literal("Legacy Connector: resources folder"), PackSource.BUILT_IN, Optional.empty());
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo location) {
                return new GameResourcesFolderPack(location, dir);
            }

            @Override
            public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                return new GameResourcesFolderPack(location, dir);
            }
        };
        Pack pack = Pack.readMetaAndCreate(info, supplier, PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.TOP, false));
        if (pack != null) event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length == 1 && elements[0].equals(PACK_META)) {
            int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
            byte[] meta = ("{\"pack\":{\"pack_format\":" + format + ",\"description\":\"1.7.10 resources folder\"}}").getBytes(StandardCharsets.UTF_8);
            return () -> new ByteArrayInputStream(meta);
        }
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES) return null;
        Path file = root.resolve(location.getNamespace()).resolve(location.getPath()).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) return null;
        return () -> Files.newInputStream(file);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) return;
        Path base = root.resolve(namespace);
        Path start = base.resolve(path);
        if (!Files.isDirectory(start)) return;
        try (Stream<Path> files = Files.walk(start)) {
            files.filter(Files::isRegularFile).forEach(f -> {
                String rel = base.relativize(f).toString().replace('\\', '/');
                ResourceLocation rl = ResourceLocation.tryBuild(namespace, rel);
                if (rl != null) output.accept(rl, () -> Files.newInputStream(f));
            });
        } catch (IOException ignored) {
            // an unreadable folder offers nothing
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        Set<String> out = new HashSet<>();
        if (type != PackType.CLIENT_RESOURCES) return out;
        try (Stream<Path> dirs = Files.list(root)) {
            dirs.filter(Files::isDirectory).forEach(d -> {
                String name = d.getFileName().toString();
                if (ResourceLocation.isValidNamespace(name)) out.add(name);
            });
        } catch (IOException ignored) {
            // nothing to serve
        }
        return out;
    }

    @Override
    public void close() {
    }
}
