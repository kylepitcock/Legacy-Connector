package org.kylepitcock.legacyconnector.mixinplugin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

/**
 * Keeps unrelated mixins away from classes that come from translated legacy jars. A legacy mod and its modern port can
 * share binary class names, so an unrelated modern mixin must not be applied to a translated class. Mixins belonging to
 * another translated mod are also isolated unless that mod explicitly declares the target mod as a dependency.
 */
public final class LegacyMixinPlugin implements IMixinConfigPlugin {
    private static final String LEGACY_PREFIX = "legacyconnector.legacy.";
    private static final String PROVENANCE = "META-INF/legacyconnector/provenance.json";
    private static final String NEOFORGE_MODS = "META-INF/neoforge.mods.toml";
    private static final Pattern PROVENANCE_MODS = Pattern.compile("\"mods\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern PROVENANCE_MOD_ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DEPENDENCY_TABLE = Pattern.compile(
            "(?ms)^\\s*\\[\\[dependencies\\.([^\\]]+)\\]\\]\\s*(.*?)(?=^\\s*\\[|\\z)");
    private static final Pattern DEPENDENCY_ID = Pattern.compile("(?m)^\\s*modId\\s*=\\s*\"([^\"]+)\"");
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

    /**
     * Parses dependency tables from generated NeoForge metadata. Only owners in this translated jar's provenance are
     * accepted; this avoids guessing the owner of a placeholder table in a multi-mod legacy jar.
     */
    static Map<String, Set<String>> parseDeclaredDependencies(String toml, Set<String> ownedModIds) {
        Map<String, Set<String>> result = new HashMap<>();
        Matcher tables = DEPENDENCY_TABLE.matcher(toml);
        while (tables.find()) {
            String owner = tables.group(1).trim().toLowerCase(java.util.Locale.ROOT);
            if (!ownedModIds.contains(owner)) continue;
            Matcher dependency = DEPENDENCY_ID.matcher(tables.group(2));
            if (dependency.find()) {
                result.computeIfAbsent(owner, ignored -> new HashSet<>())
                        .add(dependency.group(1).trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
        Map<String, Set<String>> immutable = new HashMap<>();
        result.forEach((owner, dependencies) -> immutable.put(owner, Set.copyOf(dependencies)));
        return Map.copyOf(immutable);
    }

    /** True only when at least one mod owning the mixin config declares a dependency on a mod owning the target class. */
    static boolean declaresDependency(
            Set<String> mixinModIds,
            Set<String> targetModIds,
            Map<String, Set<String>> dependenciesByOwner) {
        for (String mixinModId : mixinModIds) {
            Set<String> dependencies = dependenciesByOwner.get(mixinModId);
            if (dependencies == null) continue;
            for (String targetModId : targetModIds) {
                if (dependencies.contains(targetModId)) return true;
            }
        }
        return false;
    }

    private static Set<String> provenanceModIds(String json) {
        Matcher mods = PROVENANCE_MODS.matcher(json);
        if (!mods.find()) return Set.of();
        Matcher ids = PROVENANCE_MOD_ID.matcher(mods.group(1));
        Set<String> result = new HashSet<>();
        while (ids.find()) result.add(ids.group(1).trim().toLowerCase(java.util.Locale.ROOT));
        return Set.copyOf(result);
    }

    private static String read(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String root(String url, String resource) {
        return url.endsWith(resource) ? url.substring(0, url.length() - resource.length()) : url;
    }

    private static ClassLoader contextClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl == null ? LegacyMixinPlugin.class.getClassLoader() : cl;
    }

    private static final class Extension implements IExtension {
        private final ConcurrentHashMap<String, Boolean> legacyRoots = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Set<String>> modIdsByRoot = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Map<String, Set<String>>> dependenciesByRoot = new ConcurrentHashMap<>();
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
                boolean legacy = isLegacy(name);
                if (mixinsField == null) {
                    // TargetClassContext keeps the mixins to apply in its mixins set (no getter in this Mixin version)
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
                int[] dependencyMixinsAllowed = {0};
                // Native mixins must not leak onto translated classes. A translated mod's own mixins are also isolated,
                // except when its generated NeoForge metadata explicitly declares the target mod as a dependency.
                mixins.removeIf(m -> {
                    String config = ((IMixinInfo) m).getConfig().getName();
                    if (config.startsWith(LEGACY_PREFIX)) return !legacy;
                    if (legacy && !config.startsWith("legacyconnector") && !sameJar(name, config)) {
                        if (declaredDependency(name, config)) {
                            dependencyMixinsAllowed[0]++;
                            return false;
                        }
                        return true;
                    }
                    return false;
                });
                if (mixins.size() != before) {
                    System.out.println("[LegacyConnector] skipped " + (before - mixins.size()) + " unrelated mixin(s) on "
                            + (legacy ? "legacy" : "non-legacy") + " class " + name.replace('/', '.'));
                }
                if (dependencyMixinsAllowed[0] > 0) {
                    System.out.println("[LegacyConnector] allowed " + dependencyMixinsAllowed[0]
                            + " cross-mod mixin(s) on " + name.replace('/', '.') + " through declared mod dependencies");
                }
            } catch (Throwable t) {
                System.err.println("[LegacyConnector] legacy-class mixin guard failed: " + t);
            }
        }

        /**
         * A mod's mixin config targeting a class of its own jar is allowed. Otherwise, a cross-mod mixin is allowed
         * only when the config-owning legacy mod declares the class-owning legacy mod as a dependency.
         */
        private boolean declaredDependency(String internalName, String config) {
            try {
                ClassLoader cl = contextClassLoader();
                URL classUrl = cl.getResource(internalName + ".class");
                URL configUrl = cl.getResource(config);
                if (classUrl == null || configUrl == null) return false;
                String targetRoot = root(classUrl.toString(), internalName + ".class");
                String mixinRoot = root(configUrl.toString(), config);
                Set<String> targetIds = modIdsByRoot.get(targetRoot);
                Set<String> mixinIds = modIdsByRoot.get(mixinRoot);
                Map<String, Set<String>> dependencies = dependenciesByRoot.get(mixinRoot);
                return targetIds != null && mixinIds != null && dependencies != null
                        && LegacyMixinPlugin.declaresDependency(mixinIds, targetIds, dependencies);
            } catch (RuntimeException e) {
                return false;
            }
        }

        private boolean sameJar(String internalName, String config) {
            try {
                ClassLoader cl = contextClassLoader();
                URL classUrl = cl.getResource(internalName + ".class");
                URL configUrl = cl.getResource(config);
                if (classUrl == null || configUrl == null) return false;
                return root(classUrl.toString(), internalName + ".class").equals(root(configUrl.toString(), config));
            } catch (RuntimeException e) {
                return false;
            }
        }

        private boolean isLegacy(String internalName) throws Exception {
            ClassLoader cl = contextClassLoader();
            URL url = cl.getResource(internalName + ".class");
            if (url == null) return false;
            String u = url.toString();
            if (!rootsScanned) scan(cl);
            for (String root : legacyRoots.keySet()) if (u.startsWith(root)) return true;
            return false;
        }

        private synchronized void scan(ClassLoader cl) throws Exception {
            if (rootsScanned) return;
            Enumeration<URL> provenance = cl.getResources(PROVENANCE);
            while (provenance.hasMoreElements()) {
                URL url = provenance.nextElement();
                String value = url.toString();
                if (!value.endsWith(PROVENANCE)) continue;
                String jarRoot = root(value, PROVENANCE);
                legacyRoots.put(jarRoot, Boolean.TRUE);
                try {
                    Set<String> ids = provenanceModIds(read(url));
                    if (!ids.isEmpty()) modIdsByRoot.put(jarRoot, ids);
                } catch (IOException ignored) {
                    // A missing provenance payload still marks the jar as legacy; it simply cannot authorize cross-mod mixins.
                }
            }

            Enumeration<URL> metadata = cl.getResources(NEOFORGE_MODS);
            while (metadata.hasMoreElements()) {
                URL url = metadata.nextElement();
                String value = url.toString();
                if (!value.endsWith(NEOFORGE_MODS)) continue;
                String jarRoot = root(value, NEOFORGE_MODS);
                Set<String> ids = modIdsByRoot.get(jarRoot);
                if (ids == null || ids.isEmpty()) continue;
                try {
                    dependenciesByRoot.put(jarRoot, parseDeclaredDependencies(read(url), ids));
                } catch (IOException ignored) {
                    // Keep the isolation rule when metadata cannot be read.
                }
            }
            rootsScanned = true;
        }

        @Override
        public void postApply(ITargetClassContext context) {}

        @Override
        public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {}
    }
}
