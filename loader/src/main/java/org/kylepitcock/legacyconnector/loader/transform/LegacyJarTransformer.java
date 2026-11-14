package org.kylepitcock.legacyconnector.loader.transform;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.LegacyModDecl;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.mapping.McpMappings;
import org.kylepitcock.legacyconnector.loader.meta.ModMetadataGenerator;
import org.kylepitcock.legacyconnector.loader.resource.LegacyResourceConverter;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.synth.SynthDemand;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;

/**
 * Rewrites one legacy jar into a jar NeoForge can load. Pure function of (input jar, shim index, mappings):
 * the result is cached by the locator and reused on later launches.
 */
public final class LegacyJarTransformer {
    /** Bump whenever the output of this class changes, to invalidate caches. */
    public static final int TRANSFORMER_VERSION = 48;
    /** Names the legacy coremod plugin class of a translated jar. */
    public static final String COREMOD_RESOURCE = "META-INF/legacyconnector-coremod.txt";

    private final ShimIndex shim;
    private final ClassLoader libraries;

    /** Lower-cased ids of the legacy mods being loaded, for {@code @Optional} stripping. */
    private java.util.Set<String> presentMods = java.util.Set.of();

    public LegacyJarTransformer(ShimIndex shim, ClassLoader libraries) {
        this.shim = shim;
        this.libraries = libraries;
    }

    /** "CLIENT" / "SERVER", or null to keep both sides (offline checks). */
    private String side;

    public LegacyJarTransformer side(String side) {
        this.side = side;
        return this;
    }

    private CrossVersionPackages crossVersion = CrossVersionPackages.NONE;

    /** Packages that jars of several legacy versions define; each version links to its own copy. */
    public LegacyJarTransformer crossVersion(CrossVersionPackages crossVersion) {
        this.crossVersion = crossVersion;
        return this;
    }

    private java.util.Map<LegacyVersion, java.util.Set<String>> bundledByOtherVersions = java.util.Map.of();

    /**
     * Per version, the names of the classes that jars of the OTHER legacy versions hold. A 1.7.10 mod probing for an optional API (Thermal Expansion's
     * {@code Class.forName("buildcraft.api.tools.IToolWrench")}) must not have its use of it stubbed as absent when a 1.4.7 mod of the same game bundles it:
     * the probe succeeds at run time, so the code behind it has to be real.
     */
    public LegacyJarTransformer bundledByOtherVersions(java.util.Map<LegacyVersion, java.util.Set<String>> names) {
        this.bundledByOtherVersions = names;
        return this;
    }

    private LegacyRemapper remapperFor(LegacyVersion version, SynthDemand demand) {
        LegacyRemapper remapper = new LegacyRemapper(version, McpMappings.forVersion(version), shim, demand);
        remapper.crossVersion(crossVersion);
        return remapper;
    }

    /** Rewrites a legacy class before translation (legacy coremods' class transformers); null leaves classes as shipped. */
    public interface ClassPatcher {
        byte[] patch(LegacyJar jar, String internalName, byte[] bytes);
    }

    private ClassPatcher patcher;

    public LegacyJarTransformer patcher(ClassPatcher patcher) {
        this.patcher = patcher;
        return this;
    }

    private Contents readPatched(LegacyJar jar) throws IOException {
        Contents c = read(jar);
        if (patcher != null) c.classes.replaceAll((name, bytes) -> patcher.patch(jar, name, bytes));
        // Beta 1.7.3 jars are obfuscated: named first, then relocated like the other families
        if (jar.version == LegacyVersion.VB1_7_3) c.notes.addAll(BetaDeobfuscator.apply(jar, c.classes,shim));
        return c;
    }

    public LegacyJarTransformer presentMods(java.util.Collection<String> legacyIds) {
        java.util.Set<String> s = new java.util.HashSet<>();
        for (String id : legacyIds) s.add(id.toLowerCase(java.util.Locale.ROOT));
        this.presentMods = s;
        return this;
    }

    public static final class Result {
        public final LinkReport links = new LinkReport();
        /** Legacy API this jar needs synthesized (see {@code SynthJarBuilder}). */
        public final SynthDemand demand = new SynthDemand();
        public final List<String> notes = new ArrayList<>();
        public int classes;
        public int compatRedirects;
        public int virtualFieldRedirects;
        public int droppedShimDuplicates;
        /** 1.20.1: symbols of Minecraft 1.21.1 / NeoForge the mod references that do not exist and were replaced by stand-ins (SoftLink1201). */
        public int softLinked;
        /** The stand-ins, one entry per symbol: {@code class owner}, {@code method owner.name(desc)}, {@code field owner.name:desc} (internal names). */
        public final List<String> softSymbols = new ArrayList<>();
    }

    /** Raw contents of a legacy jar. */
    private record Contents(Map<String, byte[]> classes, Map<String, byte[]> other, Manifest manifest, List<String> notes) {}

    private static Contents read(LegacyJar jar) throws IOException {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        Map<String, byte[]> other = new LinkedHashMap<>();
        Manifest manifest = null;
        try (ZipFile zip = new ZipFile(jar.existing().toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory()) continue;
                byte[] data;
                try (InputStream in = zip.getInputStream(e)) {
                    data = in.readAllBytes();
                }
                String name = e.getName();
                if (name.matches("(?i)(.*/)?(lib|libs|libraries)/[^/]+\\.jar")) {
                    // A library bundled inside the mod (IC2's EJML): its classes join the mod's own, as if it had
                    // been put on the classpath by the mod's coremod.
                    try (java.util.zip.ZipInputStream nested = new java.util.zip.ZipInputStream(new ByteArrayInputStream(data))) {
                        for (ZipEntry n; (n = nested.getNextEntry()) != null; ) {
                            String nn = n.getName();
                            if (n.isDirectory() || nn.startsWith("META-INF/") && !nn.startsWith("META-INF/services/")) continue;
                            byte[] bytes = nested.readAllBytes();
                            if (nn.endsWith(".class")) classes.putIfAbsent(nn.substring(0, nn.length() - 6), bytes);
                            else other.putIfAbsent(nn, bytes);
                        }
                    }
                    continue;
                }
                if (name.equals(JarFile.MANIFEST_NAME)) {
                    manifest = new Manifest(new ByteArrayInputStream(data));
                } else if (name.endsWith(".class") && name.startsWith("java/")) {
                    // a stray class in the JDK's own package tree (JEI 1.12.2 ships java/mezz/jei/config/Tags): Java 8 never loaded it, but
                    // the module layer refuses to define such a package at all and the game does not start
                    continue;
                } else if (name.endsWith(".class")) {
                    classes.put(name.substring(0, name.length() - 6), data);
                } else {
                    other.put(name, data);
                }
            }
        }
        if (jar.version == LegacyVersion.VB1_7_3) stripInstallRoot(classes, other);
        return new Contents(classes, other, manifest, new ArrayList<>());
    }

    /**
     * Beta mod archives keep the files that were copied into minecraft.jar under one folder ({@code jar/mod_X.class}, {@code jar/moon/earth.png}).
     * The folder is the install root, read from where each class sits against its own package name; the mod asks for its resources
     * ({@code /moon/earth.png}) as if the root were the jar's root, so the files below it move there.
     */
    private static void stripInstallRoot(Map<String, byte[]> classes, Map<String, byte[]> other) {
        String root = null;
        for (var c : classes.entrySet()) {
            String internal;
            try { internal = new org.objectweb.asm.ClassReader(c.getValue()).getClassName(); }
            catch (RuntimeException e) { return; }
            String key = c.getKey();
            if (key.equals(internal)) return; // a class at its package's own path: the jar root is the install root
            if (!key.endsWith("/" + internal)) return;
            String found = key.substring(0, key.length() - internal.length());
            if (root == null) root = found; else if (!root.equals(found)) return;
        }
        if (root == null || root.isEmpty()) return;
        for (var entry : List.copyOf(other.entrySet())) {
            if (!entry.getKey().startsWith(root)) continue;
            other.remove(entry.getKey());
            other.putIfAbsent(entry.getKey().substring(root.length()), entry.getValue());
        }
    }

    /**
     * Classes a legacy jar ships inside Minecraft/Forge packages: either a copy of something the shim already
     * provides (dropped - one canonical copy must exist) or a mod-owned addition (moved to a private package,
     * otherwise it would split a package with the runtime module and JPMS refuses to boot). Returns the number dropped.
     */
    private static void preventCollisions(Map<String, byte[]> classes, LegacyRemapper remapper) {
        for (byte[] data : classes.values()) {
            try {
                ClassNode n = new ClassNode();
                // Instructions are needed to see a class calling an inherited method under its SRG name while declaring the readable one
                // (Twilight Forest's Naga: getMaximumHomeDistance() { return func_110174_bM(); } would recurse once renamed); only classes
                // that mention an SRG method name can do that.
                boolean srgCalls = new String(data, java.nio.charset.StandardCharsets.ISO_8859_1).contains("func_");
                new ClassReader(data).accept(n, (srgCalls ? 0 : ClassReader.SKIP_CODE) | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                remapper.preventCollisions(n);
            } catch (RuntimeException ignored) {
                // unreadable classes are reported later
            }
        }
    }

    private static final List<String> PROVIDED_LIBRARIES = List.of("it/unimi/dsi/fastutil/", "com/google/common/", "com/google/thirdparty/", "com/google/gson/",
            "org/slf4j/", "org/apache/logging/", "io/netty/", "org/joml/", "org/apache/commons/lang3/", "org/apache/commons/io/", "org/apache/commons/codec/",
            "com/google/errorprone/", "com/google/j2objc/", "org/checkerframework/", "org/jetbrains/annotations/");

    /**
     * A class of a library the game ships (commons-lang3, Guava, Netty...): dropped from every legacy jar and served by the game's copy, so
     * it is never relocated either (a relocated reference would point at a copy no jar has any more).
     */
    public static boolean isProvidedLibrary(String internalName) {
        return PROVIDED_LIBRARIES.stream().anyMatch(internalName::startsWith) && !isOptionalNativeClass(internalName);
    }

    /**
     * A class of Netty's optional native transports (kqueue, epoll...) the game's own Netty does not ship (it has no kqueue): a mod that
     * bundles Netty and names them in its network code keeps its copy, which is a package of its own and so splits nothing. The
     * rest of a bundled Netty is dropped for the game's. The game's Netty is recognised by its core classes being visible here.
     */
    private static boolean isOptionalNativeClass(String name) {
        if (!name.startsWith("io/netty/channel/kqueue/") && !name.startsWith("io/netty/channel/epoll/") && !name.startsWith("io/netty/channel/unix/")
                && !name.startsWith("io/netty/channel/uring/")) {
            return false;
        }
        try {
            ClassLoader cl = LegacyJarTransformer.class.getClassLoader();
            if (cl == null || cl.getResource("io/netty/channel/Channel.class") == null) return false;
            return cl.getResource(name + ".class") == null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private int relocateBundled(LegacyJar jar, Map<String, byte[]> classes, LegacyRemapper remapper) {
        String privateRoot = "legacyconnector_extra/" + (jar.hasMods() ? jar.mods.get(0).modernId() : LegacyModDecl.toModernId(stripJar(jar.fileName()))) + "/";
        int dropped = 0;
        // Forge 1.12.2 shipped typesafe config and akka; the runtime bundles them. A mod that overrides a class or two of them (OpenComputers
        // patches one in com.typesafe.config.impl) would split the package with that module: its few classes go, a full shaded copy stays.
        List<String> partial = new java.util.ArrayList<>();
        // (Any legacy version: OpenComputers 1.7.10 patches it the same way.)
        {
            for (String lib : List.of("com/typesafe/config/", "akka/")) {
                if (classes.keySet().stream().filter(n -> n.startsWith(lib)).count() < 20) partial.add(lib);
            }
        }
        for (String name : List.copyOf(classes.keySet())) {
            if (partial.stream().anyMatch(name::startsWith)) {
                classes.remove(name);
                dropped++;
                continue;
            }
            // Libraries the game already ships: a second copy would split the package between two modules.
            if (PROVIDED_LIBRARIES.stream().anyMatch(name::startsWith) && !isOptionalNativeClass(name)) {
                classes.remove(name);
                dropped++;
                continue;
            }
            if (name.indexOf('/') < 0) {
                // The default package (ModLoader's mod_* classes, reobfuscated helpers): a named module cannot hold classes outside a package.
                remapper.override(name, privateRoot + "root/" + name);
                for (LegacyModDecl mod : jar.mods) if (name.equals(mod.mainClass)) mod.mainClass = privateRoot + "root/" + name;
                continue;
            }
            if (!LegacyVersion.isRelocatedRoot(name) && !remapper.relocatedForVersion(name)) continue;
            String forwarded = org.kylepitcock.legacyconnector.loader.mapping.ForwardRenames.map(jar.version, name);
            if (shim.contains(jar.version.linkPrefix() + forwarded)
                    || (jar.version.forward() != null && shim.contains(jar.version.shimPrefix() + forwarded))) {
                classes.remove(name);
                dropped++;
            } else {
                remapper.override(name, privateRoot + name);
            }
        }
        // Classes in the default package (ModLoader-style mod_X, MCreator's mcreator_X, a mod compiled without a package): a named module cannot hold
        // them (the class loader never finds them), so they move into a private package of the jar.
        for (String name : List.copyOf(classes.keySet())) {
            if (name.indexOf('/') < 0 && !name.startsWith(privateRoot)) remapper.override(name, privateRoot + "root/" + name);
        }
        for (LegacyModDecl mod : jar.mods) {
            if (mod.mainClass != null && mod.mainClass.indexOf('/') < 0 && classes.containsKey(mod.mainClass)) mod.mainClass = privateRoot + "root/" + mod.mainClass;
        }
        // A mod whose own classes live under a relocated root (Engender: net.minecraft.AgeOfMinecraft.*) moved with them.
        for (LegacyModDecl mod : jar.mods) {
            if (mod.mainClass != null && classes.containsKey(mod.mainClass) && LegacyVersion.isRelocatedRoot(mod.mainClass)) {
                mod.mainClass = privateRoot + mod.mainClass;
            }
        }
        return dropped;
    }

    /**
     * Structural view (relocated names, members) of a jar's classes, for linking the jars that extend or call
     * into it. Cheap: method bodies are skipped.
     */
    public List<ClassInfo> scan(LegacyJar jar) throws IOException {
        if (jar.version == LegacyVersion.V1_20_1) return new Forge1201Transformer(shim).scan(jar);
        Contents c = readPatched(jar);
        LegacyRemapper remapper = remapperFor(jar.version, null);
        notchLocals(c.classes, remapper);
        relocateBundled(jar, c.classes, remapper);
        preventCollisions(c.classes, remapper);
        List<ClassInfo> out = new ArrayList<>();
        for (byte[] data : c.classes.values()) {
            try {
                ClassNode node = new ClassNode();
                ClassReader reader = new ClassReader(data);
                reader.accept(classRemapper(node, remapper), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                ClassInfo info = ClassInfo.of(node);
                String[] ifaces = reader.getInterfaces();
                info.originalName = reader.getClassName();
                info.originalSupers = new String[1 + ifaces.length];
                info.originalSupers[0] = reader.getSuperName();
                System.arraycopy(ifaces, 0, info.originalSupers, 1, ifaces.length);
                out.add(info);
            } catch (RuntimeException ignored) {
                // reported when the jar itself is translated
            }
        }
        return out;
    }

    /** The class visitor that relocates and renames a legacy class (1.5.2 jars keep the real EntityLiving where they mean the class itself). */
    private static ClassRemapper classRemapper(org.objectweb.asm.ClassVisitor next, LegacyRemapper remapper) {
        return (remapper.version() == LegacyVersion.V1_5_2 || remapper.version() == LegacyVersion.V1_4_7) ? new Living152ClassRemapper(next, remapper) : new ClassRemapper(next, remapper);
    }

    /** 1.6.4 jars that name Minecraft by its obfuscated names need the jar's own class hierarchy to find inherited members (see NotchNames). */
    private static void notchLocals(Map<String, byte[]> classes, LegacyRemapper remapper) {
        notchLocals(classes, remapper, Map.of());
    }

    /**
     * Copies of obfuscated vanilla classes a mod jar carries in its default package (Crafting Dead ships the reobfuscated ModelBiped,
     * RenderPlayer and ThreadDownloadImageData$1 of its build): Forge's class loader read the game's own jar first, so the copy was never
     * used. Dropping it lets the mod's references resolve to the vanilla class they name, as they did. Recognised by a notch name of the
     * version whose SourceFile is the vanilla class's simple name.
     */
    static int dropBundledVanilla(Map<String, byte[]> classes, LegacyVersion version) {
        org.kylepitcock.legacyconnector.loader.mapping.NotchNames notch = org.kylepitcock.legacyconnector.loader.mapping.NotchNames.get(version.id());
        if (notch == null || notch.isEmpty()) return 0;
        int dropped = 0;
        for (String name : List.copyOf(classes.keySet())) {
            if (name.indexOf('/') >= 0) continue;
            String mcp = notch.mcpClass(name);
            if (mcp == null) continue;
            try {
                ClassNode node = new ClassNode();
                new ClassReader(classes.get(name)).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
                String outer = mcp.substring(mcp.lastIndexOf('/') + 1);
                int dollar = outer.indexOf('$');
                if (dollar >= 0) outer = outer.substring(0, dollar);
                String source = node.sourceFile == null ? "" : node.sourceFile.replaceAll("INNER\\d+(?=\\.java$)", "");
                if (source.equals(outer + ".java")) {
                    classes.remove(name);
                    dropped++;
                }
            } catch (RuntimeException ignored) {
                // unreadable: kept, reported later
            }
        }
        return dropped;
    }

    private static void notchLocals(Map<String, byte[]> classes, LegacyRemapper remapper, Map<String, ClassInfo> peers) {
        if (!remapper.version().numericIds()) return;
        dropBundledVanilla(classes, remapper.version());
        Map<String, String[]> locals = new java.util.HashMap<>();
        // packaged classes only: a peer jar that carries vanilla classes under their notch names (a jar mod) must not hide them from this jar
        for (ClassInfo peer : peers.values()) if (peer.originalSupers != null && peer.name.indexOf('/') > 0) locals.put(peer.name, peer.originalSupers);
        for (byte[] data : classes.values()) {
            try {
                ClassReader r = new ClassReader(data);
                String[] ifaces = r.getInterfaces();
                String[] all = new String[1 + ifaces.length];
                all[0] = r.getSuperName();
                System.arraycopy(ifaces, 0, all, 1, ifaces.length);
                locals.put(r.getClassName(), all);
            } catch (RuntimeException ignored) {
                // reported when the class is translated
            }
        }
        remapper.notchLocals(locals);
    }

    public Result transform(LegacyJar jar, Path output) throws IOException {
        return transform(jar, output, Map.of());
    }

    /** @param peers classes of the other legacy jars of the same version (see {@link #scan}), by name */
    public Result transform(LegacyJar jar, Path output, Map<String, ClassInfo> peers) throws IOException {
        if (jar.version == LegacyVersion.V1_20_1) return new Forge1201Transformer(shim).transform(jar, output, peers);
        Result result = new Result();
        LegacyVersion version = jar.version;
        LegacyRemapper remapper = remapperFor(version, result.demand);

        Contents contents = readPatched(jar);
        Map<String, byte[]> classes = contents.classes;
        Map<String, byte[]> other = contents.other;
        Manifest manifest = contents.manifest;

        result.notes.addAll(contents.notes);
        int platformCopies = dropPlatformLibraries(classes, other);
        if (platformCopies > 0) result.notes.add("Dropped " + platformCopies + " bundled Mixin class(es) and launcher service file(s) (NeoForge provides Mixin)");
        notchLocals(classes, remapper, peers);
        result.droppedShimDuplicates = relocateBundled(jar, classes, remapper);
        // another jar's default-package classes (ModLoader mods share a base mod: mod_bspkrsCore) moved into a package of theirs: this jar names them by the old name
        for (ClassInfo peer : peers.values()) {
            if (peer.originalName != null && peer.originalName.indexOf('/') < 0 && !classes.containsKey(peer.originalName)) remapper.peerDefaultPackage(peer.originalName, peer.name);
        }
        preventCollisions(classes, remapper);
        if (result.droppedShimDuplicates > 0) {
            result.notes.add("Dropped " + result.droppedShimDuplicates + " bundled copies of Minecraft/Forge classes (the shim provides them)");
        }

        // Pass 1: relocate + rename, build the jar-local half of the class hierarchy.
        Hierarchy hierarchy = new Hierarchy(shim, libraries, peers, version);
        hierarchy.bundledElsewhere(bundledByOtherVersions.getOrDefault(version, java.util.Set.of()));
        Map<String, ClassNode> nodes = new LinkedHashMap<>();
        java.util.Set<String> serializable = SerialUids.serializable(classes);
        for (var e : classes.entrySet()) {
            ClassNode node = new ClassNode();
            try {
                new ClassReader(e.getValue()).accept(SerialUids.head(serializable, e.getValue(), classRemapper(node, remapper)), 0);
            } catch (RuntimeException ex) {
                result.notes.add("Skipped unreadable class " + e.getKey() + ": " + ex);
                continue;
            }
            ScalaSigRelocator.apply(node, name -> remapper.map(name));
            nodes.put(node.name, node);
            hierarchy.addLocal(ClassInfo.of(node));
        }

        // Pass 2: call-site fixes and link checking, then serialize.
        Map<String, byte[]> outClasses = new LinkedHashMap<>();
        if (side != null) {
            int sided = 0;
            for (ClassNode node : nodes.values()) {
                int n = SideOnlyStripper.apply(node, version.linkPrefix(), side);
                if (n > 0) hierarchy.addLocal(ClassInfo.of(node));
                sided += n;
            }
            if (sided > 0) result.notes.add("Removed " + sided + " @SideOnly member(s) for the other side");
            int guards = 0;
            for (ClassNode node : nodes.values()) guards += ClientClassGuard.apply(node, version.linkPrefix(), side);
            if (guards > 0) result.notes.add(guards + " client class probe(s) (catch NoClassDefFoundError) now see the class as absent on this dedicated server");
        }
        int optionalStripped = 0;
        for (ClassNode node : nodes.values()) {
            int n = OptionalStripper.apply(node, version.linkPrefix(), presentMods, hierarchy);
            if (n > 0) hierarchy.addLocal(ClassInfo.of(node));
            optionalStripped += n;
        }
        if (optionalStripped > 0) result.notes.add("Stripped " + optionalStripped + " @Optional interface(s)/method(s) for absent mods");
        for (ClassNode node : nodes.values()) CustomMainMenuVersionFix.apply(node);
        for (ClassNode node : nodes.values()) org.kylepitcock.legacyconnector.loader.transform.compat.UpdateCheckFix.apply(node);
        for (ClassNode node : nodes.values()) CofhWorldProxyFix.apply(node);
        for (ClassNode node : nodes.values()) org.kylepitcock.legacyconnector.loader.transform.compat.FatalErrorScreenFix.apply(node);
        for (ClassNode node : nodes.values()) Am2SpellIconFix.apply(node);
        for (ClassNode node : nodes.values()) Am2SpellEffectsFix.apply(node);
        for (ClassNode node : nodes.values()) Am2CompendiumFix.apply(node);
        for (ClassNode node : nodes.values()) Am2BuffCleanupFix.apply(node);
        for (ClassNode node : nodes.values()) Am2GravityFix.apply(node);
        for (ClassNode node : nodes.values()) Am2SpellBookFix.apply(node);
        for (ClassNode node : nodes.values()) Am2LecternFix.apply(node);
        int absent = 0;
        for (ClassNode node : nodes.values()) absent += AbsentDependencyStubber.apply(node, hierarchy);
        if (absent > 0) result.notes.add(absent + " method(s) use mods that are not installed; they throw NoClassDefFoundError if called");
        BetaCombatProvenance.apply(nodes,hierarchy,version,result.notes);
        int buildHeightChecks = 0;
        int floorClamps = 0;
        int forwardCalls = 0;
        int bridgeMethods = 0;
        int livingCasts = 0;
        for (ClassNode node : nodes.values()) {
            result.compatRedirects += JavaCompatRedirects.apply(node);
            result.compatRedirects += ForwardRedirects.apply(node, version);
            InterfaceFlags.apply(node, hierarchy);
            int bridged = BridgeMethods.apply(node, hierarchy, version);
            if (bridged > 0) {
                hierarchy.addLocal(ClassInfo.of(node));
                bridgeMethods += bridged;
            }
            forwardCalls += ForwardCalls.apply(node, hierarchy, version);
            livingCasts += LivingCasts152.apply(node, hierarchy, version);
            ClassLoaderCasts.apply(node, hierarchy, version);
            result.virtualFieldRedirects += ShimLinker.apply(node, hierarchy, result.links, shim.size() == 0 ? null : result.demand);
            buildHeightChecks += BuildHeightRewriter.apply(node, hierarchy, version);
            floorClamps += WorldFloorClampRewriter.apply(node, hierarchy, version);
            BetaActorTargets.apply(node,hierarchy,version);
            BetaMenuConstructors.apply(node,hierarchy,version);
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            outClasses.put(node.name + ".class", writer.toByteArray());
            result.classes++;
        }

        if (bridgeMethods > 0) result.notes.add(bridgeMethods + " bridge method(s)/call(s) from " + version.id() + " method signatures to their " + version.forward().id() + " shape");
        if (forwardCalls > 0) result.notes.add(forwardCalls + " call(s) of " + version.id() + " API now call their " + version.forward().id() + " equivalent");
        if (livingCasts > 0) result.notes.add(livingCasts + " use(s) of members only EntityLiving has (1.5.2 has no EntityLivingBase) now cast to it");
        if (buildHeightChecks > 0) result.notes.add(buildHeightChecks + " hard-coded 0..256 height check(s) on a y coordinate now use the world's build range");
        if (floorClamps > 0) result.notes.add(floorClamps + " tile entity height clamp(s) to 1.7.10's bedrock floor now count from the world's bottom");

        // Resources.
        LegacyResourceConverter resources = new LegacyResourceConverter();
        Map<String, byte[]> outResources = new LinkedHashMap<>();
        for (var e : other.entrySet()) {
            String name = e.getKey();
            if (isSignatureFile(name)) continue;
            // Forge 1.5.2 kept a mod's files under /mods/<id>/ (textures, lang, sounds); they are the mod's assets/<id>/ for the modern resource system.
            // The original stays too: mods also read them as class path files.
            if (version.numericIds() && name.startsWith("mods/") && name.indexOf('/', 5) > 5 && !name.endsWith("/")) {
                String rest = name.substring(5);
                // A domain with a slash (Immibis' "immibis/redlogic": /mods/immibis/redlogic/textures/...) is the resource domain "immibis/redlogic",
                // which the runtime turns into the namespace immibis_redlogic (Ids.namespace): the folder before textures/ or lang/ is the whole domain.
                int dirAt = Math.min(idxOrMax(rest, "/textures/"), idxOrMax(rest, "/lang/"));
                if (dirAt != Integer.MAX_VALUE && rest.substring(0, dirAt).indexOf('/') >= 0) {
                    rest = rest.substring(0, dirAt).replace('/', '_').replace('.', '_') + rest.substring(dirAt);
                }
                resources.accept("assets/" + rest, e.getValue());
            }
            if (resources.accept(name, e.getValue())) continue;
            outResources.put(name, e.getValue());
        }
        if (version.flatNames()) {
            // 1.4.7: sprite sheets named by the code are cut into one texture per tile, which the atlas stitches (see SheetCutter)
            org.kylepitcock.legacyconnector.loader.resource.SheetCutter cutter = new org.kylepitcock.legacyconnector.loader.resource.SheetCutter();
            cutter.scan(nodes.values());
            if (cutter.found()) {
                Map<String, byte[]> tiles = cutter.cut(other);
                if (!tiles.isEmpty()) {
                    outResources.putAll(tiles);
                    resources.addsTextures();
                    result.notes.add("Cut " + cutter.sheets().size() + " sprite sheet(s) into " + tiles.size() + " texture(s)");
                }
            }
        }
        outResources.putAll(resources.finish());
        if (version == LegacyVersion.V1_12_2 && jar.hasMods()) {
            jar.mixinConfigResources.clear();
            result.notes.addAll(LegacyMixinConfigs.apply(jar, outResources, nodes, hierarchy));
        }

        if (jar.coreModClass != null) {
            // The runtime constructs the coremod's plugin (without applying its transformers), as FML did.
            // second line: the Minecraft version of the plugin's jar (plugins check FMLInjectionData.data()[4])
            outResources.put(COREMOD_RESOURCE, (jar.coreModClass.trim() + "\n" + version.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        if (jar.hasMods()) {
            outResources.put("META-INF/neoforge.mods.toml", ModMetadataGenerator.modsToml(jar));
            outResources.put(ModMetadataGenerator.LEGACY_JSON, ModMetadataGenerator.legacyJson(jar));
            for (LegacyModDecl mod : jar.mods) {
                outClasses.put(ModMetadataGenerator.entrypointName(mod) + ".class", ModMetadataGenerator.entrypointClass(jar, mod));
            }
        }

        // A synthesized class can only be generated with its legacy supertypes, which may be synthesized too.
        for (String c : List.copyOf(result.demand.classes)) closeSupertypes(c, hierarchy, result.demand);
        result.links.synthesizedClasses.addAll(result.demand.classes);

        outClasses = sanitizePackages(outClasses);
        Manifest outManifest = cleanManifest(manifest, jar);
        // Provenance watermark (docs/wiki/Cache-and-Redistribution.md): hashing the original costs a few ms per translated jar only.
        String originalSha = org.kylepitcock.legacyconnector.loader.Provenance.sha256Hex(jar.existing());
        org.kylepitcock.legacyconnector.loader.Provenance.addManifest(outManifest.getMainAttributes(), jar, originalSha);
        outResources.put(org.kylepitcock.legacyconnector.loader.Provenance.RESOURCE,
                org.kylepitcock.legacyconnector.loader.Provenance.json(jar, originalSha, TRANSFORMER_VERSION));
        write(output, outManifest, outClasses, outResources);
        return result;
    }

    /**
     * A class in a directory that is no Java identifier (a mod that shaded a library under its project name, e.g. NTM-CE)
     * makes the module system reject the jar ("Invalid package name") and the whole game fails to start. The invalid
     * segments are renamed (every other character becomes '_') across all classes; string references stay unchanged.
     */
    static Map<String, byte[]> sanitizePackages(Map<String, byte[]> classes) {
        Map<String, String> prefixes = new java.util.HashMap<>();
        for (String name : classes.keySet()) {
            int slash = name.lastIndexOf('/');
            if (slash < 0 || name.startsWith("META-INF/")) continue;
            String[] segs = name.substring(0, slash).split("/");
            StringBuilder from = new StringBuilder(), to = new StringBuilder();
            for (String seg : segs) {
                String fixed = fixSegment(seg);
                from.append(seg).append('/');
                to.append(fixed).append('/');
                if (!fixed.equals(seg)) prefixes.put(from.toString(), to.toString());
            }
        }
        if (prefixes.isEmpty()) return classes;
        org.objectweb.asm.commons.Remapper remapper = new org.objectweb.asm.commons.Remapper() {
            @Override
            public String map(String internalName) {
                int i = internalName.lastIndexOf('/');
                if (i < 0) return internalName;
                String dir = internalName.substring(0, i + 1);
                // longest invalid prefix wins: rebuild segment by segment
                StringBuilder from = new StringBuilder(), to = new StringBuilder();
                String[] segs = dir.split("/");
                boolean changed = false;
                for (String seg : segs) {
                    String fixed = fixSegment(seg);
                    changed |= !fixed.equals(seg);
                    to.append(fixed).append('/');
                }
                return changed ? to + internalName.substring(i + 1) : internalName;
            }
        };
        Map<String, byte[]> out = new LinkedHashMap<>();
        for (var e : classes.entrySet()) {
            String name = e.getKey();
            if (!name.endsWith(".class") || name.startsWith("META-INF/")) {
                out.put(name, e.getValue());
                continue;
            }
            ClassWriter cw = new ClassWriter(0);
            new ClassReader(e.getValue()).accept(new ClassRemapper(cw, remapper), 0);
            out.put(remapper.map(name.substring(0, name.length() - 6)) + ".class", cw.toByteArray());
        }
        return out;
    }

    private static String fixSegment(String seg) {
        if (seg.isEmpty()) return seg;
        StringBuilder sb = new StringBuilder(seg.length());
        for (int i = 0; i < seg.length(); i++) {
            char c = seg.charAt(i);
            boolean ok = i == 0 ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c);
            sb.append(ok ? c : '_');
        }
        return sb.toString();
    }

    /** Libraries the platform ships as modules: a bundled copy would split their packages and JPMS refuses to boot. */
    private static final List<String> PLATFORM_LIBRARIES = List.of("org/spongepowered/asm/", "org/spongepowered/tools/",
            "com/llamalad7/mixinextras/",
            // the client ships JOrbis/JOgg (sound); mods that bundle them for their own sound code (MTS) would split the package
            "com/jcraft/jorbis/", "com/jcraft/jogg/");

    /**
     * Drops bundled copies of {@link #PLATFORM_LIBRARIES} (1.12.2 mods shaded Mixin for their own mixins, which target 1.12
     * classes and cannot apply here) and the service registrations that would load them as launcher or Mixin services.
     * References to the dropped classes link to NeoForge's Mixin.
     */
    static int dropPlatformLibraries(Map<String, byte[]> classes, Map<String, byte[]> other) {
        int dropped = 0;
        for (var it = classes.keySet().iterator(); it.hasNext(); ) {
            String name = it.next();
            for (String prefix : PLATFORM_LIBRARIES) {
                if (name.startsWith(prefix)) {
                    it.remove();
                    dropped++;
                    break;
                }
            }
        }
        for (var it = other.keySet().iterator(); it.hasNext(); ) {
            String name = it.next();
            if (!name.startsWith("META-INF/services/")) continue;
            String service = name.substring("META-INF/services/".length());
            if (service.startsWith("cpw.mods.modlauncher.") || service.startsWith("org.spongepowered.") || service.equals("javax.annotation.processing.Processor")) {
                it.remove();
                dropped++;
            }
        }
        return dropped;
    }

    private static void closeSupertypes(String name, Hierarchy hierarchy, SynthDemand demand) {
        ClassInfo info = hierarchy.find(name);
        if (info == null) return;
        for (String parent : info.superName == null ? info.interfaces : concat(info.superName, info.interfaces)) {
            if (hierarchy.isSynth(parent) && demand.classes.add(parent)) closeSupertypes(parent, hierarchy, demand);
        }
    }

    private static List<String> concat(String first, List<String> rest) {
        List<String> out = new ArrayList<>(rest.size() + 1);
        out.add(first);
        out.addAll(rest);
        return out;
    }

    static Manifest cleanManifest(Manifest in, LegacyJar jar) {
        Manifest out = new Manifest();
        Attributes attrs = out.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (in != null) {
            for (var e : in.getMainAttributes().entrySet()) {
                String key = e.getKey().toString();
                // Legacy loader hooks are meaningless (and some actively harmful) on modern FML.
                if (key.startsWith("FML") || key.equals("TweakClass") || key.equals("TweakOrder") || key.equals("MixinConfigs")
                        || key.equals("Main-Class") || key.equals("Class-Path") || key.equals("ContainedDeps")
                        || key.equals("Automatic-Module-Name") || key.equals("Multi-Release")) {
                    continue;
                }
                attrs.put(e.getKey(), e.getValue());
            }
            // Per-entry attributes hold signature digests that no longer match; drop them all.
        }
        attrs.putValue("LegacyConnector-Legacy-Version", jar.version.id());
        attrs.putValue("LegacyConnector-Source", jar.fileName());
        if (!jar.hasMods()) attrs.putValue("FMLModType", "GAMELIBRARY");
        return out;
    }

    static void write(Path output, Manifest manifest, Map<String, byte[]> classes, Map<String, byte[]> resources) throws IOException {
        Files.createDirectories(output.getParent());
        Path tmp = output.resolveSibling(output.getFileName() + ".tmp");
        try (OutputStream os = Files.newOutputStream(tmp); JarOutputStream jos = new JarOutputStream(os, manifest)) {
            // directory entries too: a mod that mounts a folder of its own jar (ComputerCraft's ROM) looks it up with ZipFile.getEntry("lua/rom")
            var dirs = new java.util.TreeSet<String>();
            for (String n : resources.keySet()) addParents(dirs, n);
            for (String n : classes.keySet()) addParents(dirs, n);
            for (String d : dirs) {
                if (resources.containsKey(d) || classes.containsKey(d)) continue;
                ZipEntry entry = new ZipEntry(d);
                entry.setTime(0);
                jos.putNextEntry(entry);
                jos.closeEntry();
            }
            for (var e : resources.entrySet()) put(jos, e.getKey(), e.getValue());
            for (var e : classes.entrySet()) put(jos, e.getKey(), e.getValue());
        }
        moveIntoPlace(tmp, output);
    }

    private static void addParents(java.util.Set<String> dirs, String name) {
        if (name.startsWith("META-INF/")) return; // the manifest's folder is written by JarOutputStream
        for (int i = name.indexOf('/'); i >= 0; i = name.indexOf('/', i + 1)) dirs.add(name.substring(0, i + 1));
    }

    /**
     * Windows refuses the rename while another process holds the target or the new file (an antivirus scanning the fresh
     * jar, a second game instance writing the same cache entry): retry for a few seconds; if another writer already put
     * the same jar in place, keep theirs (the cache name is a content hash).
     */
    private static void moveIntoPlace(Path tmp, Path output) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                Files.move(tmp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return;
            } catch (java.nio.file.FileSystemException e) {
                last = e;
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (Files.isRegularFile(output) && Files.size(output) > 0) {
            Files.deleteIfExists(tmp);
            return;
        }
        throw last;
    }

    private static void put(JarOutputStream jos, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0); // reproducible output
        jos.putNextEntry(entry);
        jos.write(data);
        jos.closeEntry();
    }

    private static int idxOrMax(String s, String part) {
        int i = s.indexOf(part);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    static boolean isSignatureFile(String name) {
        if (!name.startsWith("META-INF/") || name.indexOf('/', 9) >= 0) return false;
        String upper = name.toUpperCase();
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC")
                || upper.startsWith("META-INF/SIG-");
    }

    private static String stripJar(String fileName) {
        return fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }
}
