package org.kylepitcock.legacyconnector.loader.transform;

import java.io.File;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MOD-SPECIFIC (Applied Energistics 2 rv3, 1.7.10), isolated and easy to remove. AE2's coremod
 * ({@code appeng.transformer.asm.ASMIntegration}) strips the interfaces and methods of integrations that are "disabled",
 * asking {@code IntegrationRegistry.isEnabled}; that reads {@code AEConfig.instance}, which AE2 sets in its preInit. On
 * the legacy launcher classes were transformed lazily as they loaded (after preInit), but here every class is translated
 * up front, so {@code AEConfig.instance} is null, the check throws, every integration counts as failed and AE2 loses its
 * IC2/RF/Mekanism/... interfaces even when those mods are present (an ME Controller then cast to a missing
 * {@code IEnergyTile} every tick). Creating the config the way preInit would makes the coremod decide as it did.
 */
final class Ae2CoremodConfig {
    private static final Logger LOG = LoggerFactory.getLogger("Legacy Connector");

    private Ae2CoremodConfig() {}

    static void prepare(ClassLoader legacyLoader, Path gameDir) {
        Class<?> config;
        try {
            config = Class.forName("appeng.core.AEConfig", true, legacyLoader);
        } catch (ClassNotFoundException | LinkageError e) {
            return; // not Applied Energistics 2
        }
        try {
            var field = config.getField("instance");
            if (field.get(null) != null) return;
            File file = gameDir.resolve("config").resolve("AppliedEnergistics2").resolve("AppliedEnergistics2.cfg").toFile();
            file.getParentFile().mkdirs();
            field.set(null, config.getConstructor(File.class).newInstance(file));
        } catch (Throwable e) {
            Throwable c = e instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null ? ite.getCause() : e;
            LOG.warn("Legacy Connector: could not create AE2's config for its coremod ({}); its integrations may be stripped wrongly", c.toString());
        }
    }
}
