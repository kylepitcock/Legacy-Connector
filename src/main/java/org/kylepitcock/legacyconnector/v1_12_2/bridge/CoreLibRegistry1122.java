package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.lang.reflect.Method;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MOD-SPECIFIC (keyed by mod id {@code supermartijn642corelib}; remove when a generic way to run mods' Mixins on Forge internals exists).
 * SuperMartijn642's CoreLib registers the items, blocks, ... of all the mods built on it (Packed Up, Rechiseled, Simple Magnets, ...)
 * from a Mixin into {@code GameData.fireRegistryEvents} that runs after each registry event was posted. Mixins cannot apply here, so
 * after every {@code RegistryEvent.Register} the two calls of that Mixin are made by hand.
 */
public final class CoreLibRegistry1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/1.12.2");
    private static final String MOD = "supermartijn642corelib";
    private static Method handler;
    private static Method acceptor;
    private static boolean resolved;
    private static boolean failed;

    private CoreLibRegistry1122() {}

    public static void afterRegistryEvent(Object event) {
        if (failed) return;
        try {
            if (!resolved) resolve();
            if (handler == null) return;
            handler.invoke(null, event);
            if (acceptor != null) acceptor.invoke(null, event);
        } catch (java.lang.reflect.InvocationTargetException e) {
            LOG.error("CoreLib's registration after {} failed", event, e.getCause());
        } catch (ReflectiveOperationException | LinkageError e) {
            failed = true;
            LOG.error("CoreLib's registration hook is unusable: {}", e.toString());
        }
    }

    private static void resolve() throws ReflectiveOperationException {
        resolved = true;
        var mod = LegacyMods.byLegacyId(MOD);
        if (mod == null || mod.instance == null || !"1.12.2".equals(mod.version)) return;
        ClassLoader loader = mod.instance.getClass().getClassLoader();
        Class<?> event = legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent.Register.class;
        handler = Class.forName("com.supermartijn642.core.registry.RegistrationHandler", true, loader).getMethod("handleRegistryEvent", event);
        try {
            acceptor = Class.forName("com.supermartijn642.core.registry.RegistryEntryAcceptor$Handler", true, loader).getMethod("onRegisterEvent", event);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            acceptor = null;
        }
    }
}
