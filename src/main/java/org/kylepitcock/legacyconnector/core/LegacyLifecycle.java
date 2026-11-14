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
        if (CLIENT_ONLY.isEmpty()) return FAILED;
        Map<LegacyModRecord, Throwable> real = new LinkedHashMap<>(FAILED);
        real.keySet().removeAll(CLIENT_ONLY);
        return real;
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
        if (mods.isEmpty()) {
            // Forge 1.20.1 mods are ordinary NeoForge mods after the in-place remap: nothing to construct, but the addon API still
            // reports them and runs its phases (their content is registered by now)
            if (!org.kylepitcock.legacyconnector.v1_20_1.Forge1201Runtime.modIds().isEmpty()) {
                org.kylepitcock.legacyconnector.api.internal.AddonHost.enter(org.kylepitcock.legacyconnector.api.LegacyPhase.DISCOVERED);
                org.kylepitcock.legacyconnector.api.internal.AddonHost.enter(org.kylepitcock.legacyconnector.api.LegacyPhase.REGISTERED);
            }
            return;
        }
        LOG.info("Loading {} legacy mod(s): {}", mods.size(), mods);
        org.kylepitcock.legacyconnector.compat.jdk.JdkInternals.exportToLegacyMods();
        long start = System.currentTimeMillis();

        org.kylepitcock.legacyconnector.api.internal.AddonHost.enter(org.kylepitcock.legacyconnector.api.LegacyPhase.DISCOVERED);
        phase(mods, "construction", m -> LegacyMods.platform(m.version).construct(m));
        for (LegacyPlatform p : LegacyMods.platforms()) p.beforePreInit();
        for (LegacyPlatform p : LegacyMods.platforms()) p.coreModPhase("preInit");
        phase(mods, "preInit", m -> LegacyMods.platform(m.version).preInit(m));
        for (LegacyPlatform p : LegacyMods.platforms()) {
            try {
                p.registryEvents();
            } catch (Throwable t) {
                LOG.error("Legacy {} registry events failed", p.version(), t);
            }
        }
        phase(mods, "init", m -> LegacyMods.platform(m.version).init(m));
        for (LegacyPlatform p : LegacyMods.platforms()) p.coreModPhase("init");
        phase(mods, "imc", m -> LegacyMods.platform(m.version).interMod(m));
        for (LegacyPlatform p : LegacyMods.platforms()) p.coreModPhase("postInit");
        phase(mods, "postInit", m -> LegacyMods.platform(m.version).postInit(m));
        for (LegacyPlatform p : LegacyMods.platforms()) p.afterPostInit();
        org.kylepitcock.legacyconnector.api.internal.AddonHost.enter(org.kylepitcock.legacyconnector.api.LegacyPhase.REGISTERED);
        org.kylepitcock.legacyconnector.core.flatten.VanillaFlattening.saveFixedItems();
        LOG.info("Legacy mods loaded in {} ms ({} failed)", System.currentTimeMillis() - start, FAILED.size() - CLIENT_ONLY.size());
    }

    /**
     * The required dependencies of this legacy mod that the loader found not installed ("X requires Y (range), which is not installed"), empty
     * when none. The mod still loads; if it fails, {@link LegacyModException} starts its message with these.
     */
    public static List<String> missingDependencies(String legacyId) {
        String lines = System.getProperty("legacyconnector.missingDependencies", "");
        List<String> out = new ArrayList<>();
        if (lines.isBlank() || legacyId == null) return out;
        for (String line : lines.split("\n")) {
            int tab = line.indexOf('\t');
            if (tab > 0 && line.substring(0, tab).equalsIgnoreCase(legacyId)) out.add(line.substring(tab + 1));
        }
        return out;
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            phase(LegacyMods.ordered(), "loadComplete", m -> LegacyMods.platform(m.version).loadComplete(m));
            if (!LegacyMods.isEmpty() || !org.kylepitcock.legacyconnector.v1_20_1.Forge1201Runtime.modIds().isEmpty()) org.kylepitcock.legacyconnector.api.internal.AddonHost.enter(org.kylepitcock.legacyconnector.api.LegacyPhase.LOAD_COMPLETE);
        });
    }

    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        int real = FAILED.size() - CLIENT_ONLY.size();
        if (real > 0) {
            LOG.error("=== {} legacy mod(s) failed to load and are disabled ===", real);
            FAILED.forEach((m, t) -> { if (!CLIENT_ONLY.contains(m)) LOG.error("  {}: {}", m.legacyId, t.getMessage()); });
        }
    }

    private static final java.util.Set<LegacyModRecord> CLIENT_ONLY = new java.util.HashSet<>();

    /** True if the failure is the dist cleaner refusing a client class on a dedicated server. */
    static boolean touchedClientOnServer(Throwable t) {
        for (int i = 0; t != null && i < 12; i++, t = t.getCause()) {
            String m = t.getMessage();
            if (m != null && m.contains("for invalid dist DEDICATED_SERVER")) return true;
        }
        return false;
    }

    public static boolean isClientOnlySkip(LegacyModRecord mod) {
        return CLIENT_ONLY.contains(mod);
    }

    interface Phase {
        void run(LegacyModRecord mod) throws Exception;
    }

    private static void phase(List<LegacyModRecord> mods, String name, Phase phase) {
        long phaseStart = System.nanoTime();
        StringBuilder perMod = new StringBuilder();
        for (LegacyModRecord mod : new ArrayList<>(mods)) {
            if (FAILED.containsKey(mod)) continue;
            long modStart = System.nanoTime();
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            boolean numericIds = mod.version.equals("1.6.4") || mod.version.equals("1.5.2") || mod.version.equals("1.4.7");
            String previousIds = numericIds ? legacyconnector.v1_6_4.net.minecraft.util.Ids.enter(mod.version, mod.mainClass == null ? null : mod.mainClass.getClassLoader()) : null;
            try {
                if (mod.mainClass != null) thread.setContextClassLoader(mod.mainClass.getClassLoader());
                phase.run(mod);
            } catch (Throwable t) {
                LegacyModException wrapped = new LegacyModException(mod, name, t);
                if (Boolean.getBoolean("legacyconnector.failFast")) throw wrapped;
                if (touchedClientOnServer(t)) {
                    // A mod that needs the game client on a dedicated server (no side annotation to tell us beforehand): FML would have dropped it there.
                    LOG.info("Skipping the client-only legacy mod {} on a dedicated server (it touched client classes during {})", mod.legacyId, name);
                    CLIENT_ONLY.add(mod);
                    FAILED.put(mod, wrapped);
                    continue;
                }
                LOG.error("{}", wrapped.getMessage(), t);
                FAILED.put(mod, wrapped);
                org.kylepitcock.legacyconnector.api.internal.AddonHost.modFailed(mod, name);
                if ("b1.7.3".equals(mod.version)) org.kylepitcock.legacyconnector.vb1_7_3.dimension.DimensionRegistrationsB173.removeOwner(mod);
            } finally {
                if (numericIds) legacyconnector.v1_6_4.net.minecraft.util.Ids.leave(previousIds);
                thread.setContextClassLoader(previous);
                long ms = (System.nanoTime() - modStart) / 1_000_000;
                if (ms >= 50) perMod.append(' ').append(mod.legacyId).append('=').append(ms);
            }
        }
        LOG.info("[LegacyConnector/Perf] phase {} took {} ms:{}", name, (System.nanoTime() - phaseStart) / 1_000_000, perMod.length() == 0 ? " (no mod over 50 ms)" : perMod);
    }
}
