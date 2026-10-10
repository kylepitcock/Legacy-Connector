package org.kylepitcock.mccompat.loader;

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

    /**
     * Returns null if the jar is not a legacy Forge mod (e.g. it is a modern NeoForge/Forge/Fabric mod).
     */
    public static LegacyJar inspect(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
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
            }

            if (found.isEmpty() && refs17 == 0 && refs112 == 0 && infoById.isEmpty() && jar.coreModClass == null) {
                return null;
            }

            for (LegacyModDecl decl : found) {
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
    }

    private static String str(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && !e.getAsString().isBlank() ? e.getAsString() : fallback;
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
