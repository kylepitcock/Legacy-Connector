package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.cpw.mods.fml.common.discovery.ModCandidate;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLEvent;

/**
 * FML's container of an {@code @Mod} class. Every legacy mod's container is one (mods dispatch events to other mods
 * through {@link #handleModStateEvent}: BuildCraft hands its blueprints' missing blocks to each mod's missing-mappings
 * handler), and mods subclass it for their own annotations (CoFH's child mods).
 */
public class FMLModContainer implements ModContainer {
    private final String className;
    private final ModCandidate source;
    private final Map<String, Object> descriptor;
    private ModMetadata modMetadata;
    protected Object mcc$modInstance;
    private boolean enabled = true;

    public FMLModContainer(String className, ModCandidate container, Map<String, Object> modDescriptor) {
        this.className = className;
        this.source = container;
        this.descriptor = modDescriptor == null ? new HashMap<>() : modDescriptor;
    }

    private String mcc$string(String key, String fallback) {
        Object v = descriptor.get(key);
        return v == null ? fallback : v.toString();
    }

    @Override
    public String getModId() {
        return mcc$string("modid", className);
    }

    @Override
    public String getName() {
        return mcc$string("name", getModId());
    }

    @Override
    public String getVersion() {
        return mcc$string("version", "");
    }

    @Override
    public File getSource() {
        return source == null ? null : source.getModContainer();
    }

    @Override
    public ModMetadata getMetadata() {
        if (modMetadata == null) {
            modMetadata = new ModMetadata();
            modMetadata.modId = getModId();
            modMetadata.name = getName();
            modMetadata.version = getVersion();
        }
        return modMetadata;
    }

    /** FML gave the container its mcmod.info entry. */
    public void bindMetadata(MetadataCollection mc) {
        if (mc == null) return;
        ModMetadata md = mc.getMetadataForId(getModId(), descriptor);
        if (md != null) modMetadata = md;
    }

    public void setEnabledState(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean mcc$enabled() {
        return enabled;
    }

    @Override
    public Object getMod() {
        return mcc$modInstance;
    }

    @Override
    public String getDisplayVersion() {
        return getVersion();
    }

    @Override
    public Map<String, String> getCustomModProperties() {
        return new HashMap<>();
    }

    public Map<String, Object> getSharedModDescriptor() {
        return descriptor;
    }

    /** The mod's {@code @Mod} class name. */
    public String mcc$className() {
        return className;
    }

    /** Hands a lifecycle event to every {@code @Mod.EventHandler} method of the mod that accepts it. */
    public void handleModStateEvent(FMLEvent event) {
        Object mod = getMod();
        if (mod == null || event == null) return;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Class<?> c = mod.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.isAnnotationPresent(Mod.EventHandler.class) || m.getParameterCount() != 1) continue;
                if (!seen.add(m.getName() + ":" + m.getParameterTypes()[0].getName())) continue;
                if (!m.getParameterTypes()[0].isInstance(event)) continue;
                try {
                    m.setAccessible(true);
                    m.invoke(Modifier.isStatic(m.getModifiers()) ? null : mod, event);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException re) throw re;
                    if (cause instanceof Error err) throw err;
                    throw new LoaderException(cause);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    throw new LoaderException(e);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "FMLMod:" + getModId() + "{" + getVersion() + "}";
    }
}
