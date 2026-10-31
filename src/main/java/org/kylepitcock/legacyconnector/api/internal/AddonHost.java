package org.kylepitcock.legacyconnector.api.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.IModBusEvent;
import org.kylepitcock.legacyconnector.api.LegacyAddon;
import org.kylepitcock.legacyconnector.api.LegacyApi;
import org.kylepitcock.legacyconnector.api.LegacyCapabilities;
import org.kylepitcock.legacyconnector.api.LegacyMod;
import org.kylepitcock.legacyconnector.api.LegacyPhase;
import org.kylepitcock.legacyconnector.api.event.LegacyLoadCompleteEvent;
import org.kylepitcock.legacyconnector.api.event.LegacyModsDiscoveredEvent;
import org.kylepitcock.legacyconnector.api.event.LegacyRegistrationFinishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the addons, drives their callbacks and posts the lifecycle events. Called from {@code LegacyLifecycle} at the three phase
 * boundaries and from the capability registration.
 */
public final class AddonHost {
    private static final Logger LOG = LoggerFactory.getLogger("Legacy Connector/Addons");
    /** The mod property that names an addon's entry class (or classes). */
    public static final String PROPERTY = "legacyconnector-addon";

    private record Loaded(LegacyAddon addon, String className, String declaredBy) {}

    private static volatile LegacyPhase phase = LegacyPhase.NOT_STARTED;
    private static List<Loaded> addons;
    private static final List<Map.Entry<LegacyPhase, Runnable>> WAITING = new ArrayList<>();

    private AddonHost() {}

    public static LegacyPhase phase() {
        return phase;
    }

    public static void runWhen(LegacyPhase wanted, Runnable action) {
        synchronized (WAITING) {
            if (phase.compareTo(wanted) < 0) {
                WAITING.add(Map.entry(wanted, action));
                return;
            }
        }
        run(action, "runWhen");
    }

    /** Enters {@code next}: addon callbacks first, then the event on the mod buses, then the {@code runWhen} actions. */
    public static void enter(LegacyPhase next) {
        phase = next;
        for (Loaded l : active()) {
            run(() -> {
                switch (next) {
                    case DISCOVERED -> l.addon.onLegacyModsDiscovered();
                    case REGISTERED -> l.addon.onLegacyRegistered();
                    case LOAD_COMPLETE -> l.addon.onLegacyLoadComplete();
                    default -> {}
                }
            }, l.className);
        }
        switch (next) {
            case DISCOVERED -> post(new LegacyModsDiscoveredEvent(LegacyApi.mods()));
            case REGISTERED -> post(new LegacyRegistrationFinishedEvent());
            case LOAD_COMPLETE -> post(new LegacyLoadCompleteEvent());
            default -> {}
        }
        List<Runnable> due = new ArrayList<>();
        synchronized (WAITING) {
            WAITING.removeIf(w -> {
                if (w.getKey().compareTo(next) > 0) return false;
                due.add(w.getValue());
                return true;
            });
        }
        for (Runnable r : due) run(r, "runWhen");
    }

    /** Lets the active addons declare their capability adapters (called while NeoForge fires RegisterCapabilitiesEvent). */
    public static void registerCapabilities(LegacyCapabilities capabilities) {
        for (Loaded l : active()) run(() -> l.addon.registerCapabilities(capabilities), l.className);
    }

    // ------------------------------------------------------------------ discovery

    private static synchronized List<Loaded> discover() {
        if (addons != null) return addons;
        Map<String, String> declared = new LinkedHashMap<>(); // class name -> declaring mod
        for (var info : ModList.get().getMods()) {
            Object property = info.getModProperties().get(PROPERTY);
            for (String name : AddonOrder.classNames(property)) declared.putIfAbsent(name, info.getModId());
        }
        List<Loaded> found = new ArrayList<>();
        for (var e : declared.entrySet()) {
            LegacyAddon addon = instantiate(e.getKey(), e.getValue());
            if (addon != null) found.add(new Loaded(addon, e.getKey(), e.getValue()));
        }
        try {
            var services = ServiceLoader.load(LegacyAddon.class, LegacyApi.class.getClassLoader()).iterator();
            while (true) {
                try {
                    if (!services.hasNext()) break;
                    LegacyAddon a = services.next();
                    if (found.stream().noneMatch(l -> l.className.equals(a.getClass().getName()))) {
                        found.add(new Loaded(a, a.getClass().getName(), "service"));
                    }
                } catch (ServiceConfigurationError e) {
                    LOG.warn("A LegacyAddon service entry could not be loaded: {}", e.toString());
                }
            }
        } catch (RuntimeException | ServiceConfigurationError e) {
            LOG.warn("Could not scan LegacyAddon services: {}", e.toString());
        }
        List<AddonOrder.Entry> order = new ArrayList<>();
        for (Loaded l : found) order.add(new AddonOrder.Entry(l.className, l.declaredBy, priorityOf(l.addon)));
        List<Loaded> sorted = new ArrayList<>();
        for (AddonOrder.Entry entry : AddonOrder.ordered(order)) {
            for (Loaded l : found) if (l.className.equals(entry.className())) sorted.add(l);
        }
        for (Loaded l : sorted) LOG.info("Legacy addon {} (from {})", l.className, l.declaredBy);
        addons = sorted;
        return addons;
    }

    private static int priorityOf(LegacyAddon addon) {
        try {
            return addon.priority();
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    private static LegacyAddon instantiate(String className, String declaredBy) {
        ClassLoader[] loaders = {LegacyApi.class.getClassLoader(), Thread.currentThread().getContextClassLoader()};
        Throwable last = null;
        for (ClassLoader loader : loaders) {
            if (loader == null) continue;
            try {
                Class<?> cls = Class.forName(className, true, loader);
                return (LegacyAddon) cls.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                last = e;
            }
        }
        LOG.error("Mod {} names the legacy addon {} but it could not be created", declaredBy, className, last);
        return null;
    }

    /** The addons whose required legacy mods are all loaded right now. */
    private static List<Loaded> active() {
        List<Loaded> out = new ArrayList<>();
        for (Loaded l : discover()) {
            List<String> required;
            try {
                required = l.addon.requiredLegacyMods();
            } catch (RuntimeException | LinkageError e) {
                required = List.of();
            }
            boolean ok = true;
            for (String id : required) {
                if (!LegacyApi.isLoaded(id)) {
                    ok = false;
                    break;
                }
            }
            if (ok) out.add(l);
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private static <E extends Event & IModBusEvent> void post(E event) {
        ModList.get().forEachModContainer((id, container) -> {
            try {
                ModContainer c = container;
                c.getEventBus().post(event);
            } catch (RuntimeException | LinkageError e) {
                LOG.error("Mod {} failed handling {}", id, event.getClass().getSimpleName(), e);
            }
        });
    }

    private static void run(Runnable action, String what) {
        try {
            action.run();
        } catch (Throwable t) {
            LOG.error("Legacy addon {} failed", what, t);
        }
    }

    /** All legacy mod views (for the discovered event). */
    static List<LegacyMod> mods() {
        return LegacyApi.mods();
    }
}
