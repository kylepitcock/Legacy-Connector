package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710;

public class Loader {
    public static final String MC_VERSION = "1.7.10";
    private static final Loader INSTANCE = new Loader();
    /** Read reflectively by mods that show it (Custom Main Menu's #mcpversion#). */
    private static String mcpversion = "9.05";

    public String getFMLVersionString() {
        return "7.10.99.99";
    }

    public static Loader instance() {
        return INSTANCE;
    }

    /**
     * True for legacy mods (by legacy id), for platform ids legacy code probes for, and for modern NeoForge mods
     * whose id equals the normalized legacy id (so {@code isModLoaded("Baubles")} finds a modern "baubles").
     */
    /** Mod containers of legacy coremods (FML registered them as mods; mods check their ids). */
    private static final List<ModContainer> MCC$CORE_CONTAINERS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static volatile LoaderState mcc$state = LoaderState.LOADING;

    /** Lifecycle buses of coremod containers that registered handlers (Binnie Patcher registers ore dictionary entries this way). */
    private static final List<com.google.common.eventbus.EventBus> MCC$CORE_BUSES = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void mcc$registerCoreModBus(ModContainer container) {
        try {
            com.google.common.eventbus.EventBus bus = new com.google.common.eventbus.EventBus(container.getModId());
            if (container.registerBus(bus, new LoadController())) MCC$CORE_BUSES.add(bus);
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/CoreMods").warn("Coremod container {} could not register its event bus: {}", container.getModId(), e.toString());
        }
    }

    public static void mcc$postToCoreModBuses(Object event) {
        for (com.google.common.eventbus.EventBus bus : MCC$CORE_BUSES) {
            try {
                bus.post(event);
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/CoreMods").warn("A coremod container failed handling {}: {}", event.getClass().getSimpleName(), e.toString());
            }
        }
    }

    public static void mcc$addCoreModContainer(ModContainer container) {
        if (container != null && container.getModId() != null) MCC$CORE_CONTAINERS.add(container);
    }

    public static boolean mcc$isLoading() {
        return mcc$state.ordinal() >= LoaderState.CONSTRUCTING.ordinal() && mcc$state.ordinal() <= LoaderState.POSTINITIALIZATION.ordinal();
    }

    public static void mcc$setState(LoaderState state) {
        mcc$state = state;
    }

    public static boolean isModLoaded(String modname) {
        if (modname == null) return false;
        for (ModContainer c : MCC$CORE_CONTAINERS) if (modname.equals(c.getModId())) return true;
        String lower = modname.toLowerCase(Locale.ROOT);
        if (lower.equals("forge") || lower.equals("fml") || lower.equals("mcp") || lower.equals("minecraft")) return true;
        if (LegacyMods.byLegacyId(modname) != null) return true;
        for (String present : System.getProperty("legacyconnector.presentLegacyMods", "").split(",")) {
            if (!present.isEmpty() && present.equalsIgnoreCase(modname)) return true;
        }
        // Legacy libraries replaced by a modern mod (LibraryBridges in the loader) count as loaded.
        for (String bridged : System.getProperty("legacyconnector.bridgedMods", "").split(",")) {
            if (!bridged.isEmpty() && bridged.equalsIgnoreCase(modname)) return true;
        }
        ModList list = ModList.get();
        return list != null && list.isLoaded(org.kylepitcock.legacyconnector.core.Ids.modernModId(modname));
    }

    public File getConfigDir() {
        return FMLPaths.CONFIGDIR.get().toFile();
    }

    public List<ModContainer> getModList() {
        return new ArrayList<>(getIndexedModList().values());
    }

    public List<ModContainer> getActiveModList() {
        return getModList();
    }

    private static final Map<String, ModContainer> PSEUDO = new java.util.concurrent.ConcurrentHashMap<>();

    private static ModContainer pseudo(String id) {
        return PSEUDO.computeIfAbsent(id, k -> {
            ModMetadata md = new ModMetadata();
            md.modId = k;
            md.name = k;
            md.version = switch (k) {
                case "Forge" -> "10.13.4.1614";
                case "FML" -> "7.10.99.99";
                case "mcp" -> "9.05";
                case "Minecraft" -> "1.7.10";
                default -> "1.0.0";
            };
            DummyModContainer c = new DummyModContainer(md);
            c.mcc$source = new java.io.File(k.equals("Minecraft") ? "minecraft.jar" : "minecraftforge-universal.jar");
            return c;
        });
    }

    /** Mod container -> the mod's instance (FML's registry of {@code @Mod} objects). */
    public com.google.common.collect.BiMap<ModContainer, Object> getModObjectList() {
        com.google.common.collect.BiMap<ModContainer, Object> map = com.google.common.collect.HashBiMap.create();
        for (ModContainer c : getModList()) {
            Object mod = c.getMod();
            if (mod != null && !map.containsValue(mod)) map.put(c, mod);
        }
        return map;
    }

    public Map<String, ModContainer> getIndexedModList() {
        Map<String, ModContainer> out = new LinkedHashMap<>();
        for (LegacyModRecord r : LegacyMods.ordered()) {
            if (r.platformData instanceof LegacyModContainer1710 c) out.put(r.legacyId, c);
        }
        for (ModContainer c : MCC$CORE_CONTAINERS) out.putIfAbsent(c.getModId(), c);
        // Forge itself and the libraries bridged to modern mods are "mods" too (mods read their versions).
        for (String id : new String[] {"Forge", "FML", "mcp", "Minecraft"}) out.computeIfAbsent(id, Loader::pseudo);
        for (String bridged : System.getProperty("legacyconnector.bridgedMods", "").split(",")) {
            if (!bridged.isEmpty()) out.computeIfAbsent(bridged, Loader::pseudo);
        }
        return out;
    }

    public ModContainer activeModContainer() {
        return LegacyModContainer1710.active();
    }

    public String getMCVersionString() {
        return "Minecraft " + MC_VERSION;
    }

    public String getMCPVersionString() {
        return "MCP 9.05";
    }

    public boolean hasReachedState(LoaderState state) {
        return mcc$state.ordinal() >= state.ordinal() && mcc$state != LoaderState.ERRORED;
    }

    public boolean isInState(LoaderState state) {
        return mcc$state == state;
    }

    public LoaderState getLoaderState() {
        return mcc$state;
    }

}
