package org.kylepitcock.legacyconnector.vb1_7_3;

import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minecraft Beta 1.7.3 (ModLoader / Forge 1.0.x). Started: the loader detects, deobfuscates and links these jars
 * ({@code BetaJars}, {@code BetaDeobfuscator}); the ModLoader runtime is not written yet. Each {@code mod_*} class is loaded
 * (which links and verifies it in the game's module layer) but not constructed: a ModLoader mod registers its content from its
 * constructor, and against the synthesized API it would only half-register. Nothing of these mods enters the game yet.
 */
public final class PlatformB173 implements LegacyPlatform {
    public static final String VERSION = "b1.7.3";
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static boolean noticeLogged;

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public void construct(LegacyModRecord mod) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        mod.mainClass = Class.forName(mod.mainClassName, false, loader);
        if (!noticeLogged) {
            noticeLogged = true;
            LOG.warn("Beta 1.7.3 ModLoader mods are translated and linked, but their runtime is not implemented yet: they are not started "
                    + "and add nothing to the game");
        }
        LOG.info("b1.7.3 mod {} ({}) loaded, not started", mod.legacyId, mod.mainClassName);
    }

    @Override
    public void preInit(LegacyModRecord mod) {}

    @Override
    public void init(LegacyModRecord mod) {}

    @Override
    public void postInit(LegacyModRecord mod) {}
}
