package legacyconnector.v1_20_1.net.minecraftforge.resource;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.PackSource;

/**
 * Forge 1.20.1's {@code PathPackResources}: a pack read from a directory or file-system root.
 * In 1.21.1 the path is associated with a {@code PackLocationInfo}.
 *
 * <p>Some legacy resource-pack implementations (including Palladium 4.5.10) wrap this class
 * and retain only the {@link Path} returned by {@link #getSource()}. Owned ZIP file systems
 * and extracted composite roots are therefore tracked by source, rather than by the instance
 * that first opened them, so the final wrapper can release them from {@link #close()}.
 */
public class PathPackResources extends net.minecraft.server.packs.PathPackResources {
    private static final Set<FileSystem> OWNED_FILE_SYSTEMS = ConcurrentHashMap.newKeySet();
    private static final Set<Path> OWNED_TEMPORARY_ROOTS = ConcurrentHashMap.newKeySet();

    private final Path source;

    public PathPackResources(String packId, Path source) {
        this(packId, true, source);
    }

    public PathPackResources(String packId, boolean isBuiltin, Path source) {
        super(new PackLocationInfo(
                packId,
                Component.literal(packId),
                isBuiltin ? PackSource.BUILT_IN : PackSource.DEFAULT,
                Optional.empty()),
                requireSource(source));
        this.source = source;
    }

    private static Path requireSource(Path source) {
        return java.util.Objects.requireNonNull(source, "source");
    }

    /**
     * Registers a ZIP file system opened for one legacy path-backed resource pack and returns
     * its root. The legacy wrapper constructed by a mod receives this same root and owns the
     * eventual close.
     */
    public static Path ownFileSystem(FileSystem fileSystem) {
        OWNED_FILE_SYSTEMS.add(java.util.Objects.requireNonNull(fileSystem, "fileSystem"));
        return fileSystem.getPath("/");
    }

    /** Registers a temporary directory materialized from composite JarContents. */
    public static Path ownTemporaryRoot(Path root) {
        Path normalized = requireSource(root).toAbsolutePath().normalize();
        OWNED_TEMPORARY_ROOTS.add(normalized);
        return normalized;
    }

    /** Forge's accessor used by legacy mods; this must be the actual resource root, not null. */
    public Path getSource() {
        return source;
    }

    @Override
    public void close() {
        FileSystem fileSystem = source.getFileSystem();
        super.close();

        if (OWNED_FILE_SYSTEMS.remove(fileSystem) && fileSystem.isOpen()) {
            try {
                fileSystem.close();
            } catch (IOException e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/ResourcePacks")
                        .warn("Failed to close a legacy mod resource-pack file system for {}", source, e);
            }
        }

        if (OWNED_TEMPORARY_ROOTS.remove(source.toAbsolutePath().normalize())) {
            deleteTemporaryRoot(source);
        }
    }

    private static void deleteTemporaryRoot(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/ResourcePacks")
                    .warn("Failed to remove a temporary legacy mod resource-pack root {}", root, e);
        }
    }
}
