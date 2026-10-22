package org.kylepitcock.legacyconnector.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.registries.GameData;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps the legacy FML lifecycle onto NeoForge's.
 *
 * <pre>
 * legacy (1.7.10 / 1.12.2)                 NeoForge 1.21.1
 * ---------------------------------------  ---------------------------------------------------------------
 * construction (sequential)                last RegisterEvent - registries are still unfrozen, so legacy code
 * preInit                                  may construct and register blocks/items/entities imperatively,
 * [1.12] RegistryEvent.Register&lt;T&gt;         exactly like it did on the old FML
 * init
 * postInit
 * loadComplete / IMC                       FMLCommonSetupEvent (main thread via enqueueWork)
 * </pre>
 */
public final class LegacyLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger("Legacy Connector");
    private static volatile boolean registrationOpen;
    private static boolean ran;
    private static final Map<LegacyModRecord, Throwable> FAILED = new LinkedHashMap<>();

    private LegacyLifecycle() {}

    public static boolean isRegistrationOpen() {
        return registrationOpen;
    }

    public static Map<LegacyModRecord, Throwable> failures() {
        return FAILED;
    }

    /** True if the legacy mod with this modern id (or registry namespace) failed to load and is disabled. */
    public static boolean isFailed(String modernId) {
        if (modernId == null) return false;
        for (LegacyModRecord m : FAILED.keySet()) if (m.modernId.equals(modernId)) return true;
        return false;
    }

    public static void onRegister(RegisterEvent event) {
        ResourceLocation last = null;
        for (ResourceLocation rl : GameData.getRegistrationOrder()) last = rl;
        if (ran || !event.getRegistryKey().location().equals(last)) return;
        ran = true;
        registrationOpen = true;
        try {
            runLoading();
        } finally {
            registrationOpen = false;
            int orphans = org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar.discardOrphans();
            if (orphans > 0) LOG.warn("Discarded {} legacy registry object(s) that were created but never registered (failed or duplicate registrations)", orphans);
        }
    }

    private static void runLoading() {
        List<LegacyModRecord> mods = LegacyMods.ordered();
        if (mods.isEmpty()) return;
        LOG.info("Loading {} legacy mod(s): {}", mods.size(), mods);
        long start = System.currentTimeMillis();

        phase(mods, "construction", m -> LegacyMods.platform(m.version).construct(m));
        for (LegacyPlatform p : LegacyMods.platforms()) p.beforePreInit();
        phase(mods, "preInit", m -> LegacyMods.platform(m.version).preInit(m));
        for (LegacyPlatform p : LegacyMods.platforms()) {
            try {
                p.registryEvents();
            } catch (Throwable t) {
                LOG.error("Legacy {} registry events failed", p.version(), t);
            }
        }
        phase(mods, "init", m -> LegacyMods.platform(m.version).init(m));
        phase(mods, "imc", m -> LegacyMods.platform(m.version).interMod(m));
        phase(mods, "postInit", m -> LegacyMods.platform(m.version).postInit(m));
        for (LegacyPlatform p : LegacyMods.platforms()) p.afterPostInit();
        LOG.info("Legacy mods loaded in {} ms ({} failed)", System.currentTimeMillis() - start, FAILED.size());
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> phase(LegacyMods.ordered(), "loadComplete", m -> LegacyMods.platform(m.version).loadComplete(m)));
    }

    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        if (!FAILED.isEmpty()) {
            LOG.error("=== {} legacy mod(s) failed to load and are disabled ===", FAILED.size());
            FAILED.forEach((m, t) -> LOG.error("  {}: {}", m.legacyId, t.getMessage()));
        }
    }

    interface Phase {
        void run(LegacyModRecord mod) throws Exception;
    }

    private static void phase(List<LegacyModRecord> mods, String name, Phase phase) {
        for (LegacyModRecord mod : new ArrayList<>(mods)) {
            if (FAILED.containsKey(mod)) continue;
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            try {
                if (mod.mainClass != null) thread.setContextClassLoader(mod.mainClass.getClassLoader());
                phase.run(mod);
            } catch (Throwable t) {
                LegacyModException wrapped = new LegacyModException(mod, name, t);
                if (Boolean.getBoolean("legacyconnector.failFast")) throw wrapped;
                LOG.error("{}", wrapped.getMessage(), t);
                FAILED.put(mod, wrapped);
            } finally {
                thread.setContextClassLoader(previous);
            }
        }
    }
}
