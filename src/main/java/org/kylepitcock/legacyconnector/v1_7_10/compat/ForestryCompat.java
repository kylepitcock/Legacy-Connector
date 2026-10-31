package org.kylepitcock.legacyconnector.v1_7_10.compat;

import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod-specific compat for Forestry 4.x (keyed by mod id, easy to remove). Forestry fills {@code ForestryAPI.textureManager} in the
 * static initialiser of its own {@code TextureManager}; under real FML some preInit-time code touches that class, here nothing does
 * before addons (Binnie's Extra Trees) read the API field in their init. Initialise the class right after Forestry's preInit.
 */
public final class ForestryCompat {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/ForestryCompat");

    private ForestryCompat() {}

    public static void afterPreInit(LegacyModRecord mod) {
        if (!"Forestry".equals(mod.legacyId) || mod.mainClass == null) return;
        try {
            Class.forName("forestry.core.render.TextureManager", true, mod.mainClass.getClassLoader());
        } catch (Throwable t) {
            LOG.debug("Could not initialise Forestry's TextureManager: {}", t.toString());
        }
    }
}
