package org.kylepitcock.legacyconnector.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Decides whether a jar is a legacy Forge mod and, if so, which {@link LegacyVersion} family it targets.
 *
 * <p>Detection never trusts a single signal: mcmod.info is optional and often wrong, so the inspector
 * also reads the {@code @Mod} annotation and counts which FML package the bytecode links against.</p>
 */
public final class LegacyJarInspector {
    static final String MOD_1_7 = "Lcpw/mods/fml/common/Mod;";
    static final String MOD_1_12 = "Lnet/minecraftforge/fml/common/Mod;";

    private LegacyJarInspector() {}

    private static final java.util.Set<Path> DERIVATIVES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** True once if the last {@link #inspect} of this path found a translated jar (it carries a provenance watermark). */
    static boolean takeDerivative(Path path) {
        return DERIVATIVES.remove(path);
    }

    /**
     * Returns null if the jar is not a legacy Forge mod (e.g. it is a modern NeoForge/Forge/Fabric mod).
     */
    public static LegacyJar inspect(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            // Legacy Connector itself, whatever the file is called (a second copy of the jar in mods/ is not a legacy mod:
            // translating it puts the loader's packages in two modules and the game does not start).
            if (zip.getEntry("org/kylepitcock/legacyconnector/loader/LegacyModLocator.class") != null) return null;
            // A translated jar out of someone's cache (see Provenance): reported to the locator, which refuses it.
            if (zip.getEntry(Provenance.RESOURCE) != null) {
                DERIVATIVES.add(path);
                return null;
            }
            // A Forge 1.20.1 mod speaks mods.toml: it is ours (in-place remap), unlike a NeoForge or later Forge mod.
            // (multi-loader jars also carry fabric.mod.json / quilt.mod.json: those are dropped when the jar is translated)
            if (zip.getEntry("META-INF/neoforge.mods.toml") == null && zip.getEntry("META-INF/mods.toml") != null) {
                LegacyJar forge = inspectForge1201(zip, path);
                if (forge != null) return forge;
            }
            // Anything that already speaks a modern metadata format is not ours to touch.
            if (zip.getEntry("META-INF/neoforge.mods.toml") != null
                    || zip.getEntry("META-INF/mods.toml") != null
                    || zip.getEntry("fabric.mod.json") != null
                    || zip.getEntry("quilt.mod.json") != null) {
                return null;
            }

            LegacyJar jar = new LegacyJar(path);
            readManifest(zip, jar);

            Map<String, JsonObject> infoById = readMcModInfo(zip, jar);
            int refs17 = 0, refs112 = 0;
            List<LegacyModDecl> found = new ArrayList<>();

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                byte[] bytes;
                try (InputStream in = zip.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }
                ClassReader reader;
                try {
                    reader = new ClassReader(bytes);
                } catch (RuntimeException e) {
                    jar.notes.add("Unreadable class file " + entry.getName());
                    continue;
                }
                // Cheap family evidence: which FML package names appear in the constant pool.
                String pool = constantPoolText(reader);
                if (pool.contains("cpw/mods/fml/")) refs17++;
                if (pool.contains("net/minecraftforge/fml/")) refs112++;

                if (pool.contains(MOD_1_7) || pool.contains(MOD_1_12)) {
                    LegacyModDecl decl = readModAnnotation(reader);
                    if (decl != null) found.add(decl);
                }
                if (pool.contains("cpw/mods/fml/common/SidedProxy") || pool.contains("net/minecraftforge/fml/common/SidedProxy")) {
                    collectSidedProxies(reader, jar);
                }
                if (entry.getName().endsWith("package-info.class")
                        && (pool.contains("Lcpw/mods/fml/common/API;") || pool.contains("Lnet/minecraftforge/fml/common/API;"))) {
                    collectApi(reader, jar, entry.getName());
                }
            }

            if (found.isEmpty() && refs17 == 0 && refs112 == 0 && infoById.isEmpty() && jar.coreModClass == null) {
                return null;
            }

            // Scala objects carry @Mod on both the mirror class Foo and the module class Foo$; one mod each.
            java.util.Map<String, LegacyModDecl> unique = new java.util.LinkedHashMap<>();
            for (LegacyModDecl decl : found) {
                LegacyModDecl prev = unique.get(decl.legacyId);
                if (prev == null || prev.mainClass.endsWith("$") && !decl.mainClass.endsWith("$")) unique.put(decl.legacyId, decl);
            }
            for (LegacyModDecl decl : unique.values()) {
                if (decl.mainClass.endsWith("$")) decl.mainClass = decl.mainClass.substring(0, decl.mainClass.length() - 1);
                JsonObject info = infoById.get(decl.legacyId);
                if (info != null) applyMcModInfo(decl, info);
                if (decl.name == null) decl.name = decl.legacyId;
                if (decl.version == null || decl.version.isBlank() || decl.version.contains("${")) decl.version = "0.0.0-legacy";
                jar.mods.add(decl);
            }

            jar.version = chooseVersion(jar, infoById, refs17, refs112);
            return jar.version == null ? null : jar;
        }
    }

    private static final java.util.regex.Pattern TOML_STRING = java.util.regex.Pattern.compile("^\\s*([A-Za-z]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");

    /**
     * A Forge 1.20.1 jar: {@code mods.toml} with {@code modLoader="javafml"} that depends on {@code minecraft} 1.20 / 1.20.1 and not on NeoForge.
     * Returns null for everything else (Forge mods of other versions, NeoForge 1.20.1 mods, Kotlin/Scala loaders).
     */
    static LegacyJar inspectForge1201(ZipFile zip, Path path) throws IOException {
        String text;
        try (InputStream in = zip.getInputStream(zip.getEntry("META-INF/mods.toml"))) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        // multi-line strings (descriptions) can contain anything, including lines that look like tables or keys
        text = text.replaceAll("(?s)'''.*?'''|\"\"\".*?\"\"\"", "\"\"");
        String loader = null;
        String table = "";
        List<LegacyModDecl> mods = new ArrayList<>();
        LegacyModDecl current = null;
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            if (line.startsWith("#") || line.isEmpty()) continue;
            if (line.startsWith("[")) {
                table = line;
                current = null;
                if (line.startsWith("[[mods]]")) {
                    current = new LegacyModDecl();
                    mods.add(current);
                }
                continue;
            }
            var m = TOML_STRING.matcher(line);
            if (!m.find()) continue;
            String key = m.group(1), value = m.group(2) != null ? m.group(2) : m.group(3);
            if (table.isEmpty() && key.equals("modLoader")) loader = value;
            if (current != null) {
                switch (key) {
                    case "modId" -> current.legacyId = value;
                    case "displayName" -> current.name = value;
                    case "version" -> current.version = value;
                    case "description" -> current.description = value;
                    case "displayURL" -> current.url = value;
                    case "logoFile" -> current.logoFile = value;
                    case "credits" -> current.credits = value;
                    case "authors" -> current.authors.add(value);
                    default -> { }
                }
            }
        }
        if (mods.isEmpty()) {
            // inline form: mods = [ { modId = "x", version = "1", displayName = "X" } ]
            var inline = java.util.regex.Pattern.compile("\\{[^{}]*?modId\\s*=\\s*[\"']([^\"']+)[\"']([^{}]*)\\}").matcher(text);
            while (inline.find()) {
                LegacyModDecl d = new LegacyModDecl();
                d.legacyId = inline.group(1);
                var v = java.util.regex.Pattern.compile("version\\s*=\\s*[\"']([^\"']*)[\"']").matcher(inline.group(0));
                if (v.find()) d.version = v.group(1);
                var n = java.util.regex.Pattern.compile("displayName\\s*=\\s*[\"']([^\"']*)[\"']").matcher(inline.group(0));
                if (n.find()) d.name = n.group(1);
                mods.add(d);
            }
        }
        if (loader == null || !java.util.Set.of("javafml", "lowcodefml", "kotlinforforge").contains(loader) || mods.isEmpty() || mods.stream().anyMatch(d -> d.legacyId == null)) return null;
        String evidence = forge1201Evidence(zip, text, loader);
        if (evidence == null) return null;
        LegacyJar jar = new LegacyJar(path);
        readManifest(zip, jar);
        jar.coreModClass = null; // a Forge 1.20.1 jar has no FML coremod plugin
        for (LegacyModDecl d : mods) {
            if (d.name == null) d.name = d.legacyId;
            if (d.version == null || d.version.isBlank()) d.version = "0.0.0-legacy";
            d.mainClass = "";
            jar.mods.add(d);
        }
        jar.version = LegacyVersion.V1_20_1;
        jar.versionEvidence = evidence;
        return jar;
    }

    private static final java.util.regex.Pattern RANGE = java.util.regex.Pattern.compile("([\\[(])\\s*([^,\\])\\s]*)\\s*(?:(,)\\s*([^,\\])\\s]*)\\s*)?([\\])])");

    /** Maven version range ({@code [1.20,1.21)}, {@code [1.20.1]}, {@code (,1.21)}, several joined by commas) contains {@code version}. */
    static boolean rangeContains(String spec, int[] version) {
        var m = RANGE.matcher(spec);
        boolean any = false;
        while (m.find()) {
            any = true;
            String lo = m.group(2), hi = m.group(3) == null ? lo : m.group(4);
            boolean loIncl = m.group(1).equals("["), hiIncl = m.group(5).equals("]");
            boolean ok = true;
            if (!lo.isEmpty()) {
                int c = compareVersions(version, lo);
                ok = loIncl ? c >= 0 : c > 0;
            }
            if (ok && !hi.isEmpty()) {
                int c = compareVersions(version, hi);
                ok = hiIncl ? c <= 0 : c < 0;
            }
            if (ok) return true;
        }
        // a bare version ("1.20.1") is a soft requirement in Maven's syntax: accept it when it is 1.20.x
        return !any && spec.strip().matches("1\\.20(\\.[01])?");
    }

    private static int compareVersions(int[] version, String other) {
        String[] parts = other.split("[.\\-]");
        for (int i = 0; i < Math.max(version.length, parts.length); i++) {
            int a = i < version.length ? version[i] : 0;
            int b = 0;
            if (i < parts.length) {
                try {
                    b = Integer.parseInt(parts[i].replaceAll("\\D.*$", ""));
                } catch (NumberFormatException e) {
                    b = 0;
                }
            }
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    /** The version range a {@code [[dependencies.X]]} table gives for {@code modId}, or null. */
    private static String dependencyRange(String text, String modId) {
        for (String block : text.split("(?m)^\\s*\\[\\[")) {
            if (!block.startsWith("dependencies.")) continue;
            if (!java.util.regex.Pattern.compile("modId\\s*=\\s*[\"']" + java.util.regex.Pattern.quote(modId) + "[\"']").matcher(block).find()) continue;
            var r = java.util.regex.Pattern.compile("versionRange\\s*=\\s*[\"']([^\"']*)[\"']").matcher(block);
            return r.find() ? r.group(1) : "";
        }
        return null;
    }

    private static int lowerBoundMajor(String range) {
        var m = java.util.regex.Pattern.compile("[\\[(]\\s*(\\d+)").matcher(range == null ? "" : range);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /**
     * Why this {@code mods.toml} jar is for Minecraft 1.20.1, or null if it is not. In order: the declared {@code minecraft} range; the Forge version it
     * needs (46 is 1.20, 47 is 1.20.1, 48 and up are later versions); the data pack format (15 is 1.20 and 1.20.1). Many mods declare none of the first two.
     */
    private static String forge1201Evidence(ZipFile zip, String text, String loader) throws IOException {
        String mc = dependencyRange(text, "minecraft");
        if (mc != null) return rangeContains(mc, new int[] {1, 20, 1}) ? "mods.toml depends on minecraft " + mc : null;
        int forge = lowerBoundMajor(dependencyRange(text, "forge"));
        if (forge < 0) {
            var l = java.util.regex.Pattern.compile("(?m)^\\s*loaderVersion\\s*=\\s*[\"']([^\"']*)[\"']").matcher(text);
            forge = l.find() ? lowerBoundMajor(l.group(1)) : -1;
        }
        if (forge == 46 || forge == 47) return "mods.toml needs Forge " + forge + " (Minecraft 1.20.1)";
        if (forge >= 48) return null;
        boolean hasCode = false;
        for (var en = zip.entries(); en.hasMoreElements(); ) {
            String n = en.nextElement().getName();
            if (n.endsWith(".class") && !n.startsWith("META-INF/")) {
                hasCode = true;
                break;
            }
        }
        if (!hasCode) return "data-only mod (mods.toml, no code)";
        ZipEntry pack = zip.getEntry("pack.mcmeta");
        if (pack != null) {
            try (InputStream in = zip.getInputStream(pack)) {
                var f = java.util.regex.Pattern.compile("\"pack_format\"\\s*:\\s*(\\d+)").matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                if (f.find() && f.group(1).equals("15")) return "pack.mcmeta pack_format 15 (Minecraft 1.20.1)";
            }
        }
        // Forge 41 to 45 (1.19.x) or no Forge range at all: nothing in the jar says which version it is. A mods.toml jar on NeoForge 1.21.1 cannot load
        // as it is, so it is attempted as 1.20.1 (SRG names are mostly shared with 1.19.4/1.20) and the result is reported.
        if (forge >= 0) return "mods.toml needs Forge " + forge + "+, Minecraft version not declared (assumed 1.20.1)";
        return null;
    }

    private static LegacyVersion chooseVersion(LegacyJar jar, Map<String, JsonObject> infos, int refs17, int refs112) {
        // 1. The package the code actually links against is the most reliable signal.
        if (refs17 > 0 && refs112 == 0) {
            jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes)";
            return LegacyVersion.V1_7_10;
        }
        if (refs112 > 0 && refs17 == 0) {
            jar.versionEvidence = "links against net.minecraftforge.fml (" + refs112 + " classes)";
            return LegacyVersion.V1_12_2;
        }
        // 2. Declared versions.
        for (LegacyModDecl decl : jar.mods) {
            LegacyVersion v = LegacyVersion.fromMinecraftVersion(decl.acceptedMinecraftVersions);
            if (v != null) {
                jar.versionEvidence = "@Mod(acceptedMinecraftVersions=" + decl.acceptedMinecraftVersions + ")";
                return v;
            }
        }
        for (JsonObject info : infos.values()) {
            if (info.has("mcversion")) {
                LegacyVersion v = LegacyVersion.fromMinecraftVersion(info.get("mcversion").getAsString());
                if (v != null) {
                    jar.versionEvidence = "mcmod.info mcversion=" + info.get("mcversion").getAsString();
                    return v;
                }
            }
        }
        if (refs17 > 0 || refs112 > 0) {
            jar.versionEvidence = "mixed FML references, majority wins";
            return refs17 >= refs112 ? LegacyVersion.V1_7_10 : LegacyVersion.V1_12_2;
        }
        return null;
    }

    private static void readManifest(ZipFile zip, LegacyJar jar) throws IOException {
        ZipEntry entry = zip.getEntry(JarFile.MANIFEST_NAME);
        if (entry == null) return;
        try (InputStream in = zip.getInputStream(entry)) {
            Attributes attrs = new Manifest(in).getMainAttributes();
            jar.coreModClass = attrs.getValue("FMLCorePlugin");
            jar.coreModContainsMod = "true".equalsIgnoreCase(attrs.getValue("FMLCorePluginContainsFMLMod"));
            jar.tweakClass = attrs.getValue("TweakClass");
            jar.mixinConfigs = attrs.getValue("MixinConfigs");
            jar.accessTransformer = attrs.getValue("FMLAT");
        } catch (IOException | RuntimeException e) {
            jar.notes.add("Malformed manifest: " + e.getMessage());
        }
    }

    /** mcmod.info is either a bare array (v1) or {@code {"modListVersion":2,"modList":[...]}}. */
    private static Map<String, JsonObject> readMcModInfo(ZipFile zip, LegacyJar jar) {
        Map<String, JsonObject> out = new HashMap<>();
        ZipEntry entry = zip.getEntry("mcmod.info");
        if (entry == null) return out;
        try (InputStream in = zip.getInputStream(entry)) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonReader reader = new JsonReader(new StringReader(text));
            reader.setLenient(true); // plenty of legacy files have trailing commas or comments
            JsonElement root = JsonParser.parseReader(reader);
            JsonArray list = root.isJsonArray() ? root.getAsJsonArray()
                    : root.getAsJsonObject().getAsJsonArray("modList");
            if (list == null) return out;
            for (JsonElement e : list) {
                if (e.isJsonObject() && e.getAsJsonObject().has("modid")) {
                    out.put(e.getAsJsonObject().get("modid").getAsString(), e.getAsJsonObject());
                }
            }
        } catch (Exception e) {
            jar.notes.add("Could not parse mcmod.info: " + e.getMessage());
        }
        return out;
    }

    private static void applyMcModInfo(LegacyModDecl decl, JsonObject info) {
        decl.name = str(info, "name", decl.name);
        if (decl.version == null || decl.version.isBlank() || decl.version.contains("${")) decl.version = str(info, "version", decl.version);
        decl.description = str(info, "description", decl.description);
        decl.url = str(info, "url", decl.url);
        decl.logoFile = str(info, "logoFile", decl.logoFile);
        decl.credits = str(info, "credits", decl.credits);
        JsonElement authors = info.has("authorList") ? info.get("authorList") : info.get("authors");
        if (authors != null && authors.isJsonArray()) {
            for (JsonElement a : authors.getAsJsonArray()) decl.authors.add(a.getAsString());
        }
        if (decl.acceptedMinecraftVersions == null) decl.acceptedMinecraftVersions = str(info, "mcversion", null);
        // FML orders by the mcmod.info lists when the mod asks for it (useDependencyInformation).
        if (info.has("useDependencyInformation") && info.get("useDependencyInformation").isJsonPrimitive()
                && info.get("useDependencyInformation").getAsBoolean()) {
            StringBuilder extra = new StringBuilder();
            for (String key : new String[] {"requiredMods", "dependencies", "dependants"}) {
                JsonElement list = info.get(key);
                if (list == null || !list.isJsonArray()) continue;
                for (JsonElement id : list.getAsJsonArray()) {
                    if (!id.isJsonPrimitive()) continue;
                    extra.append(key.equals("dependants") ? ";before:" : ";after:").append(id.getAsString());
                }
            }
            if (!extra.isEmpty()) decl.dependencies = (decl.dependencies == null ? "" : decl.dependencies) + extra;
        }
    }

    private static String str(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && !e.getAsString().isBlank() ? e.getAsString() : fallback;
    }

    /** Fields annotated @SidedProxy, wherever they are in the jar: the runtime injects them for the jar's mods. */
    private static void collectSidedProxies(ClassReader reader, LegacyJar jar) {
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            String className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                className = name;
            }

            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                return new org.objectweb.asm.FieldVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        if (descriptor.equals("Lcpw/mods/fml/common/SidedProxy;")
                                || descriptor.equals("Lnet/minecraftforge/fml/common/SidedProxy;")) {
                            jar.sidedProxies.add(new String[] {className.replace('/', '.'), name});
                        }
                        return null;
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static void collectApi(ClassReader reader, LegacyJar jar, String entryName) {
        String pkg = entryName.substring(0, entryName.length() - "/package-info.class".length()).replace('/', '.');
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (!descriptor.equals("Lcpw/mods/fml/common/API;") && !descriptor.equals("Lnet/minecraftforge/fml/common/API;")) return null;
                String[] found = new String[4];
                found[3] = pkg;
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String name, Object value) {
                        switch (name) {
                            case "provides" -> found[0] = String.valueOf(value);
                            case "apiVersion" -> found[1] = String.valueOf(value);
                            case "owner" -> found[2] = String.valueOf(value);
                            default -> { }
                        }
                    }

                    @Override
                    public void visitEnd() {
                        if (found[0] != null && !found[0].isBlank()) jar.apis.add(found);
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static LegacyModDecl readModAnnotation(ClassReader reader) {
        LegacyModDecl[] result = new LegacyModDecl[1];
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            String className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                className = name;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (!descriptor.equals(MOD_1_7) && !descriptor.equals(MOD_1_12)) return null;
                LegacyModDecl decl = new LegacyModDecl();
                decl.mainClass = className;
                result[0] = decl;
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String name, Object value) {
                        String s = String.valueOf(value);
                        switch (name) {
                            case "modid" -> decl.legacyId = s;
                            case "name" -> decl.name = s;
                            case "version" -> decl.version = s;
                            case "dependencies" -> decl.dependencies = s;
                            case "acceptedMinecraftVersions" -> decl.acceptedMinecraftVersions = s;
                            case "clientSideOnly" -> decl.clientSideOnly = Boolean.TRUE.equals(value);
                            case "serverSideOnly" -> decl.serverSideOnly = Boolean.TRUE.equals(value);
                            default -> { }
                        }
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        LegacyModDecl decl = result[0];
        return decl == null || decl.legacyId == null ? null : decl;
    }

    /** Concatenates all UTF8 constant pool entries; good enough for substring evidence checks. */
    static String constantPoolText(ClassReader reader) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < reader.getItemCount(); i++) {
            int offset = reader.getItem(i); // points just past the tag byte; 0 for the 2nd slot of long/double
            if (offset > 0 && reader.b[offset - 1] == 1 /* CONSTANT_Utf8 */) {
                int length = reader.readUnsignedShort(offset);
                // Modified UTF-8 only differs from UTF-8 for NUL and supplementary chars, irrelevant for names.
                sb.append(new String(reader.b, offset + 2, length, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return sb.toString();
    }
}
