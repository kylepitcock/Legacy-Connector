package legacyconnector.v1_20_1.net.minecraftforge.fml.config;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;

/**
 * What a mod's own subclass of Forge 1.20.1's {@code ModConfig} extends (the class is final in NeoForge 1.21.1, so the loader moves the superclass here;
 * see {@code ModConfigSubclass1201}). It holds the arguments of the Forge constructor; {@link #addTo} (the loader's replacement for
 * {@code ModContainer.addConfig}) registers them with FML, which creates the real {@link ModConfig}. {@link #wrapperOf} maps that real config (what
 * {@code ModConfigEvent.getConfig()} returns) back to the mod's object, so {@code instanceof MyModConfig} and its methods keep working.
 * <p>
 * Not carried over: {@code getHandler()} overrides (Mekanism, Ars Nouveau moved their {@code serverconfig} files into the global config folder through
 * one); FML 4 always keeps server configs in the world's {@code serverconfig} folder.
 */
public class LegacyModConfig {
    private static final Map<ModConfig, LegacyModConfig> WRAPPERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ConfigFileTypeHandler HANDLER = new ConfigFileTypeHandler();

    private final ModConfig.Type type;
    private final IConfigSpec spec;
    private final ModContainer container;
    private final String fileName;
    private volatile ModConfig real;

    public LegacyModConfig(ModConfig.Type type, IConfigSpec spec, ModContainer container, String fileName) {
        this.type = type;
        this.spec = spec;
        this.container = container;
        this.fileName = fileName;
    }

    public ModConfig.Type getType() {
        return type;
    }

    public String getFileName() {
        return fileName;
    }

    public IConfigSpec getSpec() {
        return spec;
    }

    public String getModId() {
        return container.getModId();
    }

    public ConfigFileTypeHandler getHandler() {
        return HANDLER;
    }

    public java.nio.file.Path getFullPath() {
        ModConfig r = real;
        return r == null ? null : r.getFullPath();
    }

    public IConfigSpec.ILoadedConfig getLoadedConfig() {
        ModConfig r = real;
        return r == null ? null : r.getLoadedConfig();
    }

    /** The FML config behind this object once it was added to its container, else null. */
    public ModConfig real() {
        return real;
    }

    /** Replaces {@code ModContainer.addConfig(config)}. */
    public static void addTo(ModContainer container, Object config) {
        if (config instanceof LegacyModConfig l) {
            if (l.real != null) return;
            ModConfig created = ConfigTracker.INSTANCE.registerConfig(l.type, l.spec, container, l.fileName);
            l.real = created;
            WRAPPERS.put(created, l);
        }
        // a real ModConfig is tracked by FML when it is made: nothing to add
    }

    /** The mod's object for a config FML hands out; anything else comes back unchanged. */
    public static Object wrapperOf(Object o) {
        if (o instanceof ModConfig c) {
            LegacyModConfig w = WRAPPERS.get(c);
            if (w != null) return w;
        }
        return o;
    }
}
