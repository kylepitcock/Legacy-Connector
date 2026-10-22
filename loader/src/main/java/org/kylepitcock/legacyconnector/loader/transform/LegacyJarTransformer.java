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
    public static final int TRANSFORMER_VERSION = 5;
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
    }

    /** Raw contents of a legacy jar. */
    private record Contents(Map<String, byte[]> classes, Map<String, byte[]> other, Manifest manifest) {}

    private static Contents read(LegacyJar jar) throws IOException {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        Map<String, byte[]> other = new LinkedHashMap<>();
        Manifest manifest = null;
        try (ZipFile zip = new ZipFile(jar.path.toFile())) {
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
                } else if (name.endsWith(".class")) {
                    classes.put(name.substring(0, name.length() - 6), data);
                } else {
                    other.put(name, data);
                }
            }
        }
        return new Contents(classes, other, manifest);
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
                new ClassReader(data).accept(n, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                remapper.preventCollisions(n);
            } catch (RuntimeException ignored) {
                // unreadable classes are reported later
            }
        }
    }

    private int relocateBundled(LegacyJar jar, Map<String, byte[]> classes, LegacyRemapper remapper) {
        String privateRoot = "legacyconnector_extra/" + (jar.hasMods() ? jar.mods.get(0).modernId() : LegacyModDecl.toModernId(stripJar(jar.fileName()))) + "/";
        int dropped = 0;
        for (String name : List.copyOf(classes.keySet())) {
            if (!LegacyVersion.isRelocatedRoot(name)) continue;
            if (shim.contains(jar.version.shimPrefix() + name)) {
                classes.remove(name);
                dropped++;
            } else {
                remapper.override(name, privateRoot + name);
            }
        }
        return dropped;
    }

    /**
     * Structural view (relocated names, members) of a jar's classes, for linking the jars that extend or call
     * into it. Cheap: method bodies are skipped.
     */
    public List<ClassInfo> scan(LegacyJar jar) throws IOException {
        Contents c = readPatched(jar);
        LegacyRemapper remapper = new LegacyRemapper(jar.version, McpMappings.forVersion(jar.version), shim, null);
        relocateBundled(jar, c.classes, remapper);
        preventCollisions(c.classes, remapper);
        List<ClassInfo> out = new ArrayList<>();
        for (byte[] data : c.classes.values()) {
            try {
                ClassNode node = new ClassNode();
                new ClassReader(data).accept(new ClassRemapper(node, remapper), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                out.add(ClassInfo.of(node));
            } catch (RuntimeException ignored) {
                // reported when the jar itself is translated
            }
        }
        return out;
    }

    public Result transform(LegacyJar jar, Path output) throws IOException {
        return transform(jar, output, Map.of());
    }

    /** @param peers classes of the other legacy jars of the same version (see {@link #scan}), by name */
    public Result transform(LegacyJar jar, Path output, Map<String, ClassInfo> peers) throws IOException {
        Result result = new Result();
        LegacyVersion version = jar.version;
        LegacyRemapper remapper = new LegacyRemapper(version, McpMappings.forVersion(version), shim, result.demand);

        Contents contents = readPatched(jar);
        Map<String, byte[]> classes = contents.classes;
        Map<String, byte[]> other = contents.other;
        Manifest manifest = contents.manifest;

        result.droppedShimDuplicates = relocateBundled(jar, classes, remapper);
        preventCollisions(classes, remapper);
        if (result.droppedShimDuplicates > 0) {
            result.notes.add("Dropped " + result.droppedShimDuplicates + " bundled copies of Minecraft/Forge classes (the shim provides them)");
        }

        // Pass 1: relocate + rename, build the jar-local half of the class hierarchy.
        Hierarchy hierarchy = new Hierarchy(shim, libraries, peers, version);
        Map<String, ClassNode> nodes = new LinkedHashMap<>();
        for (var e : classes.entrySet()) {
            ClassNode node = new ClassNode();
            try {
                new ClassReader(e.getValue()).accept(new ClassRemapper(node, remapper), 0);
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
                int n = SideOnlyStripper.apply(node, version.shimPrefix(), side);
                if (n > 0) hierarchy.addLocal(ClassInfo.of(node));
                sided += n;
            }
            if (sided > 0) result.notes.add("Removed " + sided + " @SideOnly member(s) for the other side");
        }
        int optionalStripped = 0;
        for (ClassNode node : nodes.values()) {
            int n = OptionalStripper.apply(node, version.shimPrefix(), presentMods, hierarchy);
            if (n > 0) hierarchy.addLocal(ClassInfo.of(node));
            optionalStripped += n;
        }
        if (optionalStripped > 0) result.notes.add("Stripped " + optionalStripped + " @Optional interface(s)/method(s) for absent mods");
        int absent = 0;
        for (ClassNode node : nodes.values()) absent += AbsentDependencyStubber.apply(node, hierarchy);
        if (absent > 0) result.notes.add(absent + " method(s) use mods that are not installed; they throw NoClassDefFoundError if called");
        for (ClassNode node : nodes.values()) {
            result.compatRedirects += JavaCompatRedirects.apply(node);
            result.virtualFieldRedirects += ShimLinker.apply(node, hierarchy, result.links, shim.size() == 0 ? null : result.demand);
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            outClasses.put(node.name + ".class", writer.toByteArray());
            result.classes++;
        }

        // Resources.
        LegacyResourceConverter resources = new LegacyResourceConverter();
        Map<String, byte[]> outResources = new LinkedHashMap<>();
        for (var e : other.entrySet()) {
            String name = e.getKey();
            if (isSignatureFile(name)) continue;
            if (resources.accept(name, e.getValue())) continue;
            outResources.put(name, e.getValue());
        }
        outResources.putAll(resources.finish());

        if (jar.coreModClass != null) {
            // The runtime constructs the coremod's plugin (without applying its transformers), as FML did.
            outResources.put(COREMOD_RESOURCE, jar.coreModClass.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
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

        Manifest outManifest = cleanManifest(manifest, jar);
        write(output, outManifest, outClasses, outResources);
        return result;
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

    private static Manifest cleanManifest(Manifest in, LegacyJar jar) {
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

    private static void write(Path output, Manifest manifest, Map<String, byte[]> classes, Map<String, byte[]> resources) throws IOException {
        Files.createDirectories(output.getParent());
        Path tmp = output.resolveSibling(output.getFileName() + ".tmp");
        try (OutputStream os = Files.newOutputStream(tmp); JarOutputStream jos = new JarOutputStream(os, manifest)) {
            for (var e : resources.entrySet()) put(jos, e.getKey(), e.getValue());
            for (var e : classes.entrySet()) put(jos, e.getKey(), e.getValue());
        }
        Files.move(tmp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static void put(JarOutputStream jos, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0); // reproducible output
        jos.putNextEntry(entry);
        jos.write(data);
        jos.closeEntry();
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
