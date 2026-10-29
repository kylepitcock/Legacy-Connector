package org.kylepitcock.legacyconnector.core.pack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reloads the data packs when legacy recipes changed after the recipe pack was generated. Legacy mods may add or remove recipes
 * once the server exists: MineTweaker runs its scripts on {@code FMLServerAboutToStartEvent} (after the modern data packs
 * loaded) and again on {@code /minetweaker reload}; without a reload those changes would never reach the recipe manager.
 */
public final class LegacyRecipeSync {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Recipes");
    private static final int CHECK_TICKS = 100;
    private static final long MIN_INTERVAL_MS = 30_000;

    private static final List<BooleanSupplier> SOURCES = new CopyOnWriteArrayList<>();
    private static volatile boolean reloading;
    private static long lastReload;
    private static int ticks;

    private LegacyRecipeSync() {}

    /** A test telling whether a legacy registry changed since the recipe pack last read it. */
    public static void watch(BooleanSupplier changedSinceGenerate) {
        SOURCES.add(changedSinceGenerate);
    }

    public static void register(IEventBus gameBus) {
        // LOWEST: after the legacy FMLServerStartedEvent handlers ran (the server-start events are all past then).
        gameBus.addListener(EventPriority.LOWEST, (ServerStartedEvent e) -> check(e.getServer(), true));
        gameBus.addListener((ServerTickEvent.Post e) -> {
            if (++ticks % CHECK_TICKS == 0) check(e.getServer(), false);
        });
    }

    private static void check(MinecraftServer server, boolean startup) {
        if (reloading || (!startup && System.currentTimeMillis() - lastReload < MIN_INTERVAL_MS)) return;
        boolean changed = false;
        for (BooleanSupplier s : SOURCES) {
            try {
                changed |= s.getAsBoolean();
            } catch (RuntimeException e) {
                LOG.debug("Legacy recipe change check failed: {}", e.toString());
            }
        }
        if (!changed) return;
        reloading = true;
        lastReload = System.currentTimeMillis();
        LOG.info("Legacy recipes changed after the data packs loaded (scripts such as MineTweaker's); reloading data packs");
        server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((v, err) -> {
            reloading = false;
            if (err != null) LOG.warn("Reloading data packs for changed legacy recipes failed", err);
        });
    }
}
