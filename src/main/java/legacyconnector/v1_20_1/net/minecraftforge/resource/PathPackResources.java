package legacyconnector.v1_20_1.net.minecraftforge.resource;

import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.PackSource;

/**
 * Forge 1.20.1's {@code PathPackResources}, backed by the NeoForge 1.21.1 secure-jar root.
 *
 * <p>Palladium 4.5.10 wraps this type and reads both {@link #getSource()} and
 * {@link #isBuiltin()} from its parent, so both values must be preserved by this facade.
 */
public class PathPackResources extends net.minecraft.server.packs.PathPackResources {
    private final Path source;
    private final boolean builtin;

    public PathPackResources(String packId, Path source) {
        this(packId, true, source);
    }

    public PathPackResources(String packId, boolean isBuiltin, Path source) {
        super(new PackLocationInfo(
                packId,
                Component.literal(packId),
                isBuiltin ? PackSource.BUILT_IN : PackSource.DEFAULT,
                Optional.empty()),
                java.util.Objects.requireNonNull(source, "source"));
        this.source = source;
        this.builtin = isBuiltin;
    }

    /** Forge's accessor used by legacy mods; returns the actual root containing assets and data. */
    public Path getSource() {
        return source;
    }

    /**
     * Forge 1.20.1's path resolver. Palladium 4.5.10 uses a Mixin invoker for this
     * protected method when wrapping a mod pack. The 1.21.1 vanilla superclass no
     * longer declares it, so the compatibility facade must keep the legacy method
     * on this concrete class for that invoker to bind.
     *
     * <p>Each argument is a path component (and may itself contain slash-separated
     * directories), matching Forge 1.20.1's implementation.
     */
    protected Path resolve(String... paths) {
        Path resolved = getSource();
        for (String path : paths) {
            resolved = resolved.resolve(path);
        }
        return resolved;
    }

    /** Retains Forge 1.20.1's explicit builtin flag for wrappers such as Palladium's ModResourcePack. */
    public boolean isBuiltin() {
        return builtin;
    }
}
