package org.kylepitcock.legacyconnector.loader.transform;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class transformers of legacy 1.7.10 coremods, applied to legacy mod classes before translation - as
 * LaunchWrapper did when it loaded them. Coremods routinely rewrite other mods' classes: CoFH's {@code @Strippable} and
 * AE2's {@code @Integration} remove interfaces of absent mods, OpenMods and EnderCore inject interfaces, and patcher
 * mods fix API mismatches between mods. Transformers aimed at Minecraft's own classes find nothing to do (Minecraft is
 * not present here; the shim replaces it).
 *
 * <p>Coremod plugins are loaded in an isolated class loader (a stand-in {@code LaunchClassLoader} over the legacy jars,
 * with a legacy-named LaunchWrapper/FML API from {@code coremod-api.jar}) and see the production environment: SRG
 * names, {@code runtimeDeobfuscationEnabled}. A plugin or transformer that fails is skipped and logged.</p>
 */
public final class LegacyCoreModTransformers {
    private static final Logger LOG = LoggerFactory.getLogger("Legacy Connector");
    private static final String API_RESOURCE = "legacyconnector/legacy/%s/coremod-api.jar";

    private record Transformer(String name, Object instance) {}

    private final List<Transformer> transformers = new ArrayList<>();
    private final Set<String> exclusions;
    private final Method transform;
    private final Map<String, Integer> failures = new HashMap<>();

    private LegacyCoreModTransformers(List<Transformer> transformers, Set<String> exclusions, Method transform) {
        this.transformers.addAll(transformers);
        this.exclusions = exclusions;
        this.transform = transform;
    }

    private final java.util.Map<String, byte[]> results = new java.util.HashMap<>();

    public boolean isEmpty() {
        return transformers.isEmpty();
    }

    /**
     * Bundled libraries (BundledLibraries) the coremod plugins' own classes use: argo (CodeChickenCore's DepLoader), trove (CoFH Core's ASMCore builds its
     * method tables with gnu.trove maps in a static initializer; without it the whole CoFH class transformer was skipped and none of its hooks ran).
     */
    static boolean isCoremodLibrary(String fileName) {
        return fileName.startsWith("argo-") || fileName.startsWith("trove4j-");
    }

    /** The coremod API flavour a version's plugins link against: 1.7.10's, or Forge 1.6.4's (java.util.logging, getLibraryRequestClass; also 1.5.2). */
    public static LegacyVersion apiFor(LegacyVersion version) {
        return version.numericIds() ? LegacyVersion.V1_6_4 : LegacyVersion.V1_7_10;
    }

    /** The cache file name of an API flavour's nested jar. */
    public static String apiJarName(LegacyVersion api) {
        return api == LegacyVersion.V1_6_4 ? "coremod-api-1.6.4.jar" : "coremod-api.jar";
    }

    /**
     * The transformers of every coremod among {@code jars} whose version uses the {@code api} flavour (1.7.10, or 1.6.4 and 1.5.2 together); empty when
     * there are none or loading failed. Each flavour has its own isolated class loader.
     */
    public static LegacyCoreModTransformers load(List<LegacyJar> jars, Path cacheDir, Path gameDir, String side, Path runtimeJar, LegacyVersion api) {
        List<LegacyJar> legacy = jars.stream().filter(j -> j.version.family() == LegacyVersion.V1_7_10 && apiFor(j.version) == api).toList();
        List<LegacyJar> coremods = legacy.stream().filter(j -> j.coreModClass != null && !j.coreModClass.isBlank()).toList();
        if (coremods.isEmpty()) return empty();
        try {
            Path apiJar = cacheDir.resolve(apiJarName(api));
            String apiResource = API_RESOURCE.formatted(api.id());
            try (InputStream in = LegacyCoreModTransformers.class.getClassLoader().getResourceAsStream(apiResource)) {
                if (in == null) {
                    LOG.warn("Legacy Connector: {} is missing; legacy coremod transformers are not applied", apiResource);
                    return empty();
                }
                Files.copy(in, apiJar, StandardCopyOption.REPLACE_EXISTING);
            }
            System.setProperty("java.awt.headless", "true");
            if (side != null) System.setProperty("legacyconnector.side", side);
            ClassLoader parent = LegacyCoreModTransformers.class.getClassLoader();
            URLClassLoader apiLoader = new URLClassLoader("legacyconnector-coremod-api", new URL[] {apiJar.toUri().toURL()}, parent);
            Path runtimeClasses = runtimeJar != null ? runtimeJar
                    : System.getProperty("legacyconnector.runtimeClasses") != null ? Path.of(System.getProperty("legacyconnector.runtimeClasses")) : null;
            ClassLoader fml = runtimeClasses == null ? apiLoader : new ShimLoader(runtimeClasses, apiLoader, api);
            List<URL> urls = new ArrayList<>();
            for (LegacyJar j : legacy) urls.add(j.path.toUri().toURL());
            // Libraries the old launcher put on the class path that coremod plugins use (argo: CodeChickenCore's DepLoader; trove: CoFH Core's ASMCore), extracted by BundledLibraries
            try (var dirs = Files.list(cacheDir)) {
                for (Path dir : dirs.filter(d -> Files.isDirectory(d) && d.getFileName().toString().startsWith("libs-")).toList()) {
                    try (var libs = Files.list(dir)) {
                        for (Path lib : libs.filter(f -> isCoremodLibrary(f.getFileName().toString()) && f.toString().endsWith(".jar")).toList()) urls.add(lib.toUri().toURL());
                    }
                }
            }
            Class<?> lclClass = apiLoader.loadClass("net.minecraft.launchwrapper.LaunchClassLoader");
            lclClass.getField("mcc$definer").set(null, (UnaryOperator<byte[]>) LegacyCoreModTransformers::modernizeApi);
            Class<?> lclImpl = api == LegacyVersion.V1_6_4 ? apiLoader.loadClass("cpw.mods.fml.relauncher.RelaunchClassLoader") : lclClass;
            URLClassLoader lcl = (URLClassLoader) lclImpl.getConstructor(URL[].class, ClassLoader.class)
                    .newInstance(urls.toArray(URL[]::new), fml);
            openJavaLang(lcl);
            Class<?> launch = apiLoader.loadClass("net.minecraft.launchwrapper.Launch");
            launch.getField("classLoader").set(null, lcl);
            apiLoader.loadClass("cpw.mods.fml.relauncher.FMLLaunchHandler").getMethod("mcc$setClassLoader", Object.class).invoke(null, lcl);
            apiLoader.loadClass("cpw.mods.fml.relauncher.FMLInjectionData").getField("mcVersion").set(null, coremods.get(0).version.id());
            launch.getField("minecraftHome").set(null, gameDir.toFile());
            @SuppressWarnings("unchecked")
            Map<String, Object> blackboard = (Map<String, Object>) launch.getField("blackboard").get(null);
            blackboard.put("fml.deobfuscatedEnvironment", Boolean.FALSE);

            Class<?> pluginType = apiLoader.loadClass("cpw.mods.fml.relauncher.IFMLLoadingPlugin");
            Class<? extends java.lang.annotation.Annotation> sortingIndex = annotation(apiLoader, "SortingIndex");
            Class<? extends java.lang.annotation.Annotation> exclusionsAnn = annotation(apiLoader, "TransformerExclusions");
            @SuppressWarnings("unchecked")
            Set<String> exclusions = (Set<String>) lclClass.getMethod("mcc$transformerExclusions").invoke(lcl);
            Method addExclusion = lclClass.getMethod("addTransformerExclusion", String.class);

            List<String> coremodNames = coremods.stream().map(j -> j.coreModClass.trim()).toList();
            Class<?> transformerType = apiLoader.loadClass("net.minecraft.launchwrapper.IClassTransformer");
            Method transform = transformerType.getMethod("transform", String.class, String.class, byte[].class);
            // As FML: every plugin is constructed first (in discovery order); then, by sorting index, each one's
            // transformers are created and its injectData runs (plugins check each other's state there).
            record Plugin(LegacyJar jar, int index, Object instance) {}
            List<Plugin> plugins = new ArrayList<>();
            for (LegacyJar jar : coremods) {
                String pluginClass = jar.coreModClass.trim();
                Plugin p = timed(pluginClass, jar, lcl, () -> {
                    addExclusion.invoke(lcl, pluginClass);
                    Class<?> cls = Class.forName(pluginClass, true, lcl);
                    if (!pluginType.isAssignableFrom(cls)) return null;
                    var ex = cls.getAnnotation(exclusionsAnn);
                    if (ex != null) for (String e : (String[]) exclusionsAnn.getMethod("value").invoke(ex)) if (!e.isEmpty()) addExclusion.invoke(lcl, e);
                    var si = cls.getAnnotation(sortingIndex);
                    int index = si == null ? 0 : (Integer) sortingIndex.getMethod("value").invoke(si);
                    return new Plugin(jar, index, cls.getDeclaredConstructor().newInstance());
                });
                if (p != null) plugins.add(p);
            }
            plugins.sort(Comparator.comparingInt(Plugin::index));
            record Loaded(String jar, List<Transformer> transformers) {}
            List<Loaded> loaded = new ArrayList<>();
            for (Plugin p : plugins) {
                LegacyJar jar = p.jar();
                Loaded l = timed(jar.coreModClass.trim(), jar, lcl, () -> {
                    String[] names = (String[]) pluginType.getMethod("getASMTransformerClass").invoke(p.instance());
                    List<Transformer> created = new ArrayList<>();
                    if (names != null) for (String name : names) {
                        if (name == null || name.isBlank()) continue;
                        try {
                            Object instance = Class.forName(name.trim(), true, lcl).getDeclaredConstructor().newInstance();
                            if (transformerType.isInstance(instance)) created.add(new Transformer(name.trim(), instance));
                        } catch (Throwable e) {
                            LOG.warn("Legacy Connector: class transformer {} of {} could not be created and is skipped: {}", name, jar.fileName(), rootChain(e));
                        }
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("mcLocation", gameDir.toFile());
                    data.put("coremodList", coremodNames);
                    data.put("runtimeDeobfuscationEnabled", Boolean.TRUE);
                    data.put("coremodLocation", jar.path.toFile());
                    data.put("classLoader", lcl);   // FML 1.5.2 handed the RelaunchClassLoader to the plugin (CodeChickenCore keeps it)
                    pluginType.getMethod("injectData", Map.class).invoke(p.instance(), data);
                    // FML constructed the plugin's mod container before any class was transformed; transformers rely on
                    // what its constructor sets up (loggers, shared state).
                    try {
                        String containerClass = (String) pluginType.getMethod("getModContainerClass").invoke(p.instance());
                        if (containerClass != null && !containerClass.isBlank()) {
                            Class.forName(containerClass.trim(), true, lcl).getDeclaredConstructor().newInstance();
                        }
                    } catch (Throwable e) {
                        LOG.warn("Legacy Connector: mod container of {} could not be created: {}", jar.fileName(), cause(e).toString());
                    }
                    return new Loaded(jar.fileName(), created);
                });
                if (l != null) loaded.add(l);
            }
            Ae2CoremodConfig.prepare(lcl, gameDir);
            List<Transformer> out = new ArrayList<>();
            Map<String, String> owners = new LinkedHashMap<>();
            for (Loaded l : loaded) {
                for (Transformer tr : l.transformers()) {
                    out.add(tr);
                    owners.put(tr.name(), l.jar());
                }
            }
            // Transformers a mod registers itself ({@code Launch.classLoader.registerTransformer(X.class.getName())} in a static initialiser, which FML 1.5
            // ran while loading the mod's classes: Immibis Core's trait injection). The initialiser itself touches the game (it cannot run here), so
            // its constant parts are replayed: the registered transformer classes, and the constant pairs it puts into static maps.
            try {
                for (LegacyJar jar : coremods) {
                    for (StaticRegistration reg : staticTransformerRegistrations(jar.path)) {
                        timed(reg.holder(), jar, lcl, () -> {
                            for (String[] put : reg.puts()) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    Map<Object, Object> map = (Map<Object, Object>) Class.forName(put[0], true, lcl).getField(put[1]).get(null);
                                    map.put(Class.forName(put[2], true, lcl), Class.forName(put[3], true, lcl));
                                } catch (Throwable e) {
                                    LOG.warn("Legacy Connector: {} of {}: a registration of the static initialiser could not be replayed: {}", reg.holder(), jar.fileName(), rootChain(e));
                                }
                            }
                            for (String name : reg.transformers()) {
                                try {
                                    Object instance = Class.forName(name, true, lcl).getDeclaredConstructor().newInstance();
                                    if (transformerType.isInstance(instance) && out.stream().noneMatch(o -> o.name().equals(name))) {
                                        out.add(new Transformer(name, instance));
                                        owners.put(name, jar.fileName());
                                    }
                                } catch (Throwable e) {
                                    LOG.warn("Legacy Connector: class transformer {} registered by {} could not be created and is skipped: {}", name, reg.holder(), rootChain(e));
                                }
                            }
                            return null;
                        });
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Legacy Connector: transformers registered by mod classes could not be collected: {}", rootChain(e));
            }
            if (!out.isEmpty()) LOG.info("Legacy Connector: applying {} legacy coremod class transformer(s): {}", out.size(), owners);
            return new LegacyCoreModTransformers(out, exclusions, transform);
        } catch (Throwable e) {
            LOG.warn("Legacy Connector: legacy coremod transformers are not applied", e);
            return empty();
        }
    }

    /** What a static initialiser registers: transformer classes and constant (map field, key class, value class) puts. */
    private record StaticRegistration(String holder, List<String> transformers, List<String[]> puts) {}

    /** The classes of a jar whose static initialiser calls {@code registerTransformer} on a class loader (mods that install their own transformers). */
    private static List<StaticRegistration> staticTransformerRegistrations(Path jar) {
        List<StaticRegistration> found = new ArrayList<>();
        try (var zip = new java.util.zip.ZipFile(jar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var e = entries.nextElement();
                if (!e.getName().endsWith(".class") || e.getSize() > 400_000) continue;
                byte[] bytes;
                try (InputStream in = zip.getInputStream(e)) {
                    bytes = in.readAllBytes();
                }
                if (indexOf(bytes, "registerTransformer".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)) < 0) continue;
                var node = new org.objectweb.asm.tree.ClassNode();
                new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                for (var m : node.methods) {
                    if (!m.name.equals("<clinit>")) continue;
                    List<String> transformers = new ArrayList<>();
                    List<String[]> puts = new ArrayList<>();
                    var insns = m.instructions.toArray();
                    for (int i = 0; i < insns.length; i++) {
                        if (!(insns[i] instanceof org.objectweb.asm.tree.MethodInsnNode mi)) continue;
                        if (mi.name.equals("registerTransformer") && mi.owner.endsWith("ClassLoader")) {
                            // ... ldc X.class; invokevirtual Class.getName; invokevirtual registerTransformer  (or ldc "x.Y")
                            for (int k = i - 1; k >= Math.max(0, i - 4); k--) {
                                if (insns[k] instanceof org.objectweb.asm.tree.LdcInsnNode l) {
                                    if (l.cst instanceof org.objectweb.asm.Type t) transformers.add(t.getClassName());
                                    else if (l.cst instanceof String str) transformers.add(str);
                                    break;
                                }
                            }
                        } else if (mi.name.equals("put") && mi.owner.equals("java/util/Map") && i >= 3
                                && insns[i - 3] instanceof org.objectweb.asm.tree.FieldInsnNode f && f.getOpcode() == org.objectweb.asm.Opcodes.GETSTATIC
                                && insns[i - 2] instanceof org.objectweb.asm.tree.LdcInsnNode k1 && k1.cst instanceof org.objectweb.asm.Type kt
                                && insns[i - 1] instanceof org.objectweb.asm.tree.LdcInsnNode v1 && v1.cst instanceof org.objectweb.asm.Type vt) {
                            puts.add(new String[] {f.owner.replace('/', '.'), f.name, kt.getClassName(), vt.getClassName()});
                        }
                    }
                    if (!transformers.isEmpty()) found.add(new StaticRegistration(node.name.replace('/', '.'), transformers, puts));
                }
            }
        } catch (java.io.IOException | RuntimeException ex) {
            LOG.debug("Legacy Connector: {} could not be scanned for transformer registrations: {}", jar.getFileName(), ex.toString());
        }
        return found;
    }

    private static int indexOf(byte[] data, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= data.length; i++) {
            for (int j = 0; j < needle.length; j++) if (data[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }

    /** Coremod code of the 1.5 era makes ClassLoader.defineClass accessible (CodeChickenCore's DelegatedTransformer); Java 8 let it. */
    private static void openJavaLang(ClassLoader lcl) {
        try {
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
            java.lang.reflect.Field implField = java.lang.invoke.MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            var impl = (java.lang.invoke.MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(implField), unsafe.staticFieldOffset(implField));
            var addOpens = impl.findVirtual(Module.class, "implAddOpens", java.lang.invoke.MethodType.methodType(void.class, String.class, Module.class));
            addOpens.invoke(Object.class.getModule(), "java.lang", lcl.getUnnamedModule());
        } catch (Throwable t) {
            LOG.debug("Legacy Connector: could not open java.lang to the coremod class loader: {}", t.toString());
        }
    }

    private interface Step<T> {
        T run() throws Throwable;
    }

    /** Runs one step of a plugin on its own thread (plugins may block on network or UI), with a time limit. */
    private static <T> T timed(String plugin, LegacyJar jar, ClassLoader context, Step<T> step) throws InterruptedException {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                result.set(step.run());
            } catch (Throwable e) {
                error.set(cause(e));
            }
        }, "Legacy Connector coremod " + plugin);
        t.setDaemon(true);
        t.setContextClassLoader(context);
        t.start();
        t.join(30_000);
        if (t.isAlive()) LOG.warn("Legacy Connector: coremod plugin {} of {} did not finish in 30 s; its transformers are not applied", plugin, jar.fileName());
        else if (error.get() != null) LOG.warn("Legacy Connector: coremod plugin {} of {} failed; its transformers are not applied: {}", plugin, jar.fileName(), error.get().toString());
        return t.isAlive() ? null : result.get();
    }

    /** The exception with its cause chain, one line (a class that failed in a static initialiser hides the reason in a cause). */
    private static String rootChain(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = cause(e); t != null && sb.length() < 600; t = t.getCause() == t ? null : t.getCause()) {
            if (sb.length() > 0) sb.append(" <- ");
            sb.append(t);
            StackTraceElement[] st = t.getStackTrace();
            if (st.length > 0) sb.append(" at ").append(st[0]);
        }
        return sb.toString();
    }

    private static Throwable cause(Throwable e) {
        return e instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null ? ite.getCause() : e;
    }

    private static LegacyCoreModTransformers empty() {
        return new LegacyCoreModTransformers(List.of(), Set.of(), null);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends java.lang.annotation.Annotation> annotation(ClassLoader loader, String name) throws ClassNotFoundException {
        return (Class<? extends java.lang.annotation.Annotation>) loader.loadClass("cpw.mods.fml.relauncher.IFMLLoadingPlugin$" + name);
    }

    /** Runs every transformer over one legacy class (internal name), in plugin order; failures leave the input. */
    public synchronized byte[] apply(String internalName, byte[] bytes) {
        if (transformers.isEmpty()) return bytes;
        String name = internalName.replace('/', '.');
        for (String e : exclusions) if (name.startsWith(e)) return bytes;
        // A jar is read more than once (a scan for the hierarchy, then the translation) and FML ran each transformer once
        // per class: some remember what they patched (Binnie Patcher) and leave the second copy untouched. The first result
        // is therefore reused for the same input.
        String key = name + ':' + bytes.length + ':' + java.util.Arrays.hashCode(bytes);
        byte[] remembered = results.get(key);
        if (remembered != null) return remembered;
        byte[] current = bytes;
        for (Transformer t : transformers) {
            try {
                byte[] next = (byte[]) transform.invoke(t.instance, name, name, current);
                if (next != null && next != current) current = recomputeMaxs(next);
            } catch (Throwable e) {
                Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null ? ite.getCause() : e;
                int n = failures.merge(t.name, 1, Integer::sum);
                if (n <= 3) LOG.warn("Legacy Connector: class transformer {} failed on {} (class left as it was): {}", t.name, name, cause.toString());
                if (n == 1) LOG.debug("Legacy Connector: class transformer {} failure detail", t.name, cause);
            }
        }
        if (current != bytes) results.put(key, current);
        return current;
    }

    /**
     * Legacy FML/Forge classes beyond the launch API, served from Legacy Connector's own shim with the relocation undone
     * ({@code legacyconnector/v1_7_10/cpw/mods/fml/common/DummyModContainer} as {@code cpw/mods/fml/common/DummyModContainer}).
     * Coremod plugins extend and call these (mod containers, versioning, events). Parts that reach into the running game
     * cannot link here and fail only if used.
     */
    private static final class ShimLoader extends ClassLoader {
        private static final String PREFIX = LegacyVersion.V1_7_10.shimPrefix();
        /** Shim roots tried in order: the 1.6.4 API (ITickHandler, TickType...) lives in its own root over the 1.7.10 one. */
        private final List<String> prefixes;
        private final java.util.zip.ZipFile jar;
        private final Path dir;

        /** @param runtime the runtime jar, or (development) its classes directory */
        ShimLoader(Path runtime, ClassLoader parent, LegacyVersion api) throws java.io.IOException {
            super("legacyconnector-coremod-shim", parent);
            this.prefixes = api == LegacyVersion.V1_6_4 ? List.of(LegacyVersion.V1_6_4.shimPrefix(), PREFIX) : List.of(PREFIX);
            this.dir = Files.isDirectory(runtime) ? runtime : null;
            this.jar = dir == null ? new java.util.zip.ZipFile(runtime.toFile()) : null;
        }

        private byte[] read(String entry) throws java.io.IOException {
            if (dir != null) {
                Path p = dir.resolve(entry);
                return Files.exists(p) ? Files.readAllBytes(p) : null;
            }
            var e = jar.getEntry(entry);
            if (e == null) return null;
            try (InputStream in = jar.getInputStream(e)) {
                return in.readAllBytes();
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String internal = name.replace('.', '/');
            byte[] bytes = null;
            try {
                if (LegacyVersion.isRelocatedRoot(internal)) {
                    for (String prefix : prefixes) {
                        bytes = read(prefix + internal + ".class");
                        if (bytes != null) break;
                    }
                } else {
                    bytes = read(internal + ".class");
                }
            } catch (java.io.IOException ex) {
                throw new ClassNotFoundException(name, ex);
            }
            if (bytes == null) {
                // A vanilla class the shim has no hand-written version of: coremods still name it (ClassWriter's
                // frame computation asks for common superclasses), so serve an empty class with its real supertypes.
                var entryData = org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData.forVersion(LegacyVersion.V1_7_10).get(internal);
                if (entryData == null) {
                    // A Minecraft class of the mod's own version (1.5.2 names such as net.minecraft.network.packet.Packet are not in the 1.7.10
                    // data): mod classes whose static initialisers install transformers (Immibis Core) only name it in signatures, and the verifier
                    // accepts any value for an interface type, so an empty interface lets them link.
                    if (!internal.startsWith("net/minecraft/")) throw new ClassNotFoundException(name);
                    ClassWriter stub = new ClassWriter(0);
                    stub.visit(org.objectweb.asm.Opcodes.V17, org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_INTERFACE
                            | org.objectweb.asm.Opcodes.ACC_ABSTRACT, internal, null, "java/lang/Object", null);
                    stub.visitEnd();
                    byte[] bytesOut = stub.toByteArray();
                    return defineClass(name, bytesOut, 0, bytesOut.length);
                }
                ClassWriter bare = new ClassWriter(0);
                boolean iface = (entryData.access() & org.objectweb.asm.Opcodes.ACC_INTERFACE) != 0;
                bare.visit(org.objectweb.asm.Opcodes.V17, entryData.access() | org.objectweb.asm.Opcodes.ACC_PUBLIC, internal, null,
                        iface || entryData.superName() == null ? "java/lang/Object" : entryData.superName(),
                        entryData.interfaces().toArray(new String[0]));
                bare.visitEnd();
                byte[] out = bare.toByteArray();
                return defineClass(name, out, 0, out.length);
            }
            ClassWriter writer = new ClassWriter(0);
            new ClassReader(bytes).accept(new ClassRemapper(writer, new Remapper() {
                @Override
                public String map(String n) {
                    for (String prefix : prefixes) if (n.startsWith(prefix)) return n.substring(prefix.length());
                    return n;
                }
            }), 0);
            byte[] out = writer.toByteArray();
            return defineClass(name, out, 0, out.length);
        }
    }

    /**
     * Coremod classes run before (and outside) the jar translation: calls to Guava methods that newer Guava moved
     * (Objects.firstNonNull, used by OpenMods' transformer helpers) are redirected the way translated mods' are.
     */
    private static byte[] guavaCalls(byte[] bytes) {
        try {
            String text = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
            // also the (URLClassLoader) casts of dependency loaders (Galacticraft's plugin adds its libraries to the class path)
            if (!text.contains("com/google/common/base/Objects") && !text.contains("java/net/URLClassLoader")) return bytes;
            org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
            new ClassReader(bytes).accept(node, 0);
            if (JavaCompatRedirects.apply(node) == 0) return bytes;
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            return writer.toByteArray();
        } catch (RuntimeException e) {
            return bytes;
        }
    }

    /** Patched methods may need a deeper operand stack than they declare; FML's writers computed it, so do the same. */
    private static byte[] recomputeMaxs(byte[] bytes) {
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            reader.accept(writer, 0);
            return writer.toByteArray();
        } catch (RuntimeException e) {
            return bytes;
        }
    }

    /** Classes of coremods compiled against ASM 5 use names ASM 9 replaced (see {@link LegacyRemapper#JDK_RELOCATIONS}). */
    private static byte[] modernizeApi(byte[] bytes) {
        bytes = CoremodPatches.apply(bytes);
        bytes = guavaCalls(bytes);
        Map<String, String> renames = new HashMap<>();
        for (var e : LegacyRemapper.JDK_RELOCATIONS.entrySet()) {
            if (e.getKey().startsWith("org/objectweb/asm/")) renames.put(e.getKey(), e.getValue());
        }
        renames.put("org/objectweb/asm/ClassWriter", "org/kylepitcock/legacyconnector/loader/transform/CompatClassWriter");
        boolean mentions = false;
        String text = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        for (String k : renames.keySet()) if (text.contains(k)) mentions = true;
        if (!mentions) return bytes;
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassRemapper(writer, new Remapper() {
            @Override
            public String map(String internalName) {
                return renames.getOrDefault(internalName, internalName);
            }
        }), 0);
        return writer.toByteArray();
    }
}
