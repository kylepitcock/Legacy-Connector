package org.kylepitcock.legacyconnector.mixinplugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

/**
 * Keeps the mixins of OTHER mods away from classes that come from translated legacy jars. A legacy mod and its modern port often
 * share binary class names (Mekanism's {@code mekanism.client.render.entity.RenderFlame}), so a modern mod's mixin (Iris targets the
 * 1.21 RenderFlame) would be applied to the legacy class, fail to find its injection point and take the mod down. Mixin has no hook
 * for "do not apply configs of other mods to this class", so this plugin registers a Mixin extension whose {@code preApply} empties
 * the mixin set of a legacy-origin target class, except for this mod's own configs. A class is legacy-origin when its jar carries the
 * translator's provenance resource.
 */
public final class LegacyMixinPlugin implements IMixinConfigPlugin {
    private static final String PROVENANCE = "META-INF/legacyconnector/provenance.json";
    private static volatile boolean installed;

    @Override
    public void onLoad(String mixinPackage) {
        install();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        install();
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        install();
    }

    @Override
    public List<String> getMixins() {
        install();
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    private static synchronized void install() {
        if (installed) return;
        try {
            MixinEnvironment env = MixinEnvironment.getCurrentEnvironment();
            Object transformer = env.getActiveTransformer();
            if (transformer == null) return; // too early; a later plugin callback retries
            Extensions extensions = null;
            for (Class<?> c = transformer.getClass(); c != null && extensions == null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (Extensions.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        extensions = (Extensions) f.get(transformer);
                        break;
                    }
                }
            }
            installed = true;
            if (extensions == null) return;
            extensions.add(new Extension());
            extensions.select(env);
        } catch (Throwable t) {
            installed = true;
            System.err.println("[LegacyConnector] could not install the legacy-class mixin guard: " + t);
        }
    }

    private static final class Extension implements IExtension {
        private final ConcurrentHashMap<String, Boolean> legacyRoots = new ConcurrentHashMap<>();
        private volatile boolean rootsScanned;
        private java.lang.reflect.Field mixinsField;

        @Override
        public boolean checkActive(MixinEnvironment environment) {
            return true;
        }

        @Override
        public void preApply(ITargetClassContext context) {
            try {
                String name = context.getClassNode().name;
                if (!isLegacy(name)) return;
                if (mixinsField == null) {
                    // TargetClassContext keeps the mixins to apply in its `mixins` set (no getter in this Mixin version)
                    for (Class<?> c = context.getClass(); c != null && mixinsField == null; c = c.getSuperclass()) {
                        try {
                            mixinsField = c.getDeclaredField("mixins");
                        } catch (NoSuchFieldException ignored) {
                            // next superclass
                        }
                    }
                    if (mixinsField == null) throw new NoSuchFieldException("mixins in " + context.getClass().getName());
                    mixinsField.setAccessible(true);
                }
                Collection<?> mixins = (Collection<?>) mixinsField.get(context);
                int before = mixins.size();
                mixins.removeIf(m -> !((IMixinInfo) m).getConfig().getName().startsWith("legacyconnector"));
                if (mixins.size() != before) {
                    System.out.println("[LegacyConnector] skipped " + (before - mixins.size()) + " mixin(s) of other mods on legacy class " + name.replace('/', '.'));
                }
            } catch (Throwable t) {
                System.err.println("[LegacyConnector] legacy-class mixin guard failed: " + t);
            }
        }

        private boolean isLegacy(String internalName) throws Exception {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) cl = LegacyMixinPlugin.class.getClassLoader();
            URL url = cl.getResource(internalName + ".class");
            if (url == null) return false;
            String u = url.toString();
            if (!rootsScanned) scan(cl);
            for (String root : legacyRoots.keySet()) if (u.startsWith(root)) return true;
            return false;
        }

        private synchronized void scan(ClassLoader cl) throws Exception {
            // All mod jars are on the loader when the first mixin target is transformed.
            List<String> found = new ArrayList<>();
            Enumeration<URL> e = cl.getResources(PROVENANCE);
            while (e.hasMoreElements()) {
                String s = e.nextElement().toString();
                if (s.endsWith(PROVENANCE)) found.add(s.substring(0, s.length() - PROVENANCE.length()));
            }
            for (String s : found) legacyRoots.put(s, Boolean.TRUE);
            rootsScanned = true;
        }

        @Override
        public void postApply(ITargetClassContext context) {}

        @Override
        public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {}
    }
}
