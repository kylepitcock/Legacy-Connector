package legacyconnector.v1_20_1.net.minecraftforge.resource;

import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.PackSource;

/** Forge 1.20.1's {@code PathPackResources}: a pack read from a directory or file system root (1.21.1 describes a pack with a {@code PackLocationInfo}). */
public class PathPackResources extends net.minecraft.server.packs.PathPackResources {
    public PathPackResources(String packId, Path source) {
        super(new PackLocationInfo(packId, Component.literal(packId), PackSource.BUILT_IN, Optional.empty()), source);
    }

    public PathPackResources(String packId, boolean isBuiltin, Path source) {
        this(packId, source);
    }

    public Path getSource() {
        return null;
    }
}
