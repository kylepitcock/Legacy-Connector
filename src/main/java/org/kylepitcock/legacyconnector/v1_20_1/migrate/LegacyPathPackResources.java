package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;

/**
 * A class extending {@code PathPackResources} and calling the 1.20.1 constructor {@code (String name, Path root, boolean isBuiltin)} (LDLib's
 * {@code CustomResourcePack}): it extends this stand-in instead, which builds the 1.21.1 {@code PackLocationInfo} from the three values.
 */
public class LegacyPathPackResources extends PathPackResources {
    public LegacyPathPackResources(String name, Path root, boolean builtin) {
        super(new PackLocationInfo(name, Component.literal(name), builtin ? PackSource.BUILT_IN : PackSource.DEFAULT, Optional.empty()), root);
    }
}
