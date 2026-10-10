package org.kylepitcock.mccompat.loader.transform;

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
import org.kylepitcock.mccompat.loader.LegacyJar;
import org.kylepitcock.mccompat.loader.LegacyModDecl;
import org.kylepitcock.mccompat.loader.LegacyVersion;
import org.kylepitcock.mccompat.loader.mapping.McpMappings;
import org.kylepitcock.mccompat.loader.meta.ModMetadataGenerator;
import org.kylepitcock.mccompat.loader.resource.LegacyResourceConverter;
import org.kylepitcock.mccompat.loader.shim.ShimIndex;
import org.kylepitcock.mccompat.loader.shim.ClassInfo;
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
    public static final int TRANSFORMER_VERSION = 1;

    private final ShimIndex shim;
    private final ClassLoader libraries;

    public LegacyJarTransformer(ShimIndex shim, ClassLoader libraries) {
        this.shim = shim;
        this.libraries = libraries;
    }

    public static final class Result {
        public final LinkReport links = new LinkReport();
        public final List<String> notes = new ArrayList<>();
        public int classes;
        public int compatRedirects;
        public int virtualFieldRedirects;
        public int droppedShimDuplicates;
    }

    public Result transform(LegacyJar jar, Path output) throws IOException {
        Result result = new Result();
        LegacyVersion version = jar.version;
        LegacyRemapper remapper = new LegacyRemapper(version, McpMappings.forVersion(version));

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
                if (name.equals(JarFile.MANIFEST_NAME)) {
                    manifest = new Manifest(new ByteArrayInputStream(data));
                } else if (name.endsWith(".class")) {
                    classes.put(name.substring(0, name.length() - 6), data);
                } else {
                    other.put(name, data);
                }
            }
        }

        // Classes a legacy jar ships inside Minecraft/Forge packages: either a copy of something the shim already
        // provides (drop it - one canonical copy must exist) or a mod-owned addition (move it to a private package,
        // otherwise it would split a package with the runtime module and JPMS refuses to boot).
        String privateRoot = "mccompat_extra/" + (jar.hasMods() ? jar.mods.get(0).modernId() : LegacyModDecl.toModernId(stripJar(jar.fileName()))) + "/";
        for (String name : List.copyOf(classes.keySet())) {
            if (!LegacyVersion.isRelocatedRoot(name)) continue;
            if (shim.contains(version.shimPrefix() + name)) {
                classes.remove(name);
                result.droppedShimDuplicates++;
            } else {
                remapper.override(name, privateRoot + name);
            }
        }
        if (result.droppedShimDuplicates > 0) {
            result.notes.add("Dropped " + result.droppedShimDuplicates + " bundled copies of Minecraft/Forge classes (the shim provides them)");
        }

        // Pass 1: relocate + rename, build the jar-local half of the class hierarchy.
        Hierarchy hierarchy = new Hierarchy(shim, libraries);
        Map<String, ClassNode> nodes = new LinkedHashMap<>();
        for (var e : classes.entrySet()) {
            ClassNode node = new ClassNode();
            try {
                new ClassReader(e.getValue()).accept(new ClassRemapper(node, remapper), 0);
            } catch (RuntimeException ex) {
                result.notes.add("Skipped unreadable class " + e.getKey() + ": " + ex);
                continue;
            }
            nodes.put(node.name, node);
            hierarchy.addLocal(ClassInfo.of(node));
        }

        // Pass 2: call-site fixes and link checking, then serialize.
        Map<String, byte[]> outClasses = new LinkedHashMap<>();
        for (ClassNode node : nodes.values()) {
            result.compatRedirects += JavaCompatRedirects.apply(node);
            result.virtualFieldRedirects += ShimLinker.apply(node, hierarchy, result.links);
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

        if (jar.hasMods()) {
            outResources.put("META-INF/neoforge.mods.toml", ModMetadataGenerator.modsToml(jar));
            outResources.put(ModMetadataGenerator.LEGACY_JSON, ModMetadataGenerator.legacyJson(jar));
            for (LegacyModDecl mod : jar.mods) {
                outClasses.put(ModMetadataGenerator.entrypointName(mod) + ".class", ModMetadataGenerator.entrypointClass(jar, mod));
            }
        }

        Manifest outManifest = cleanManifest(manifest, jar);
        write(output, outManifest, outClasses, outResources);
        return result;
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
        attrs.putValue("MCCompat-Legacy-Version", jar.version.id());
        attrs.putValue("MCCompat-Source", jar.fileName());
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
