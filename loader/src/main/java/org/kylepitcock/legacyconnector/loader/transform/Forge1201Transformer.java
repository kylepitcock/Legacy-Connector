package org.kylepitcock.legacyconnector.loader.transform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.LegacyJarInspector;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.Provenance;
import org.kylepitcock.legacyconnector.loader.mapping.SrgMojangTable;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Forge 1.20.1 to NeoForge 1.21.1, in place. Unlike 1.7.10 and 1.12.2 nothing is relocated into a shim: the mod keeps calling the real
 * Minecraft classes. What changes:
 * <ul>
 *   <li>SRG ids ({@code m_46859_}, {@code f_46441_}) become Mojang names ({@link SrgMojangTable}), also in reflection strings;</li>
 *   <li>Forge classes become NeoForge's (tables in {@code legacyconnector/forward/1.20.1/*.tsv}, then the package moves); the few Forge API classes
 *       1.21.1 no longer has are served by a facade ({@code legacyconnector/v1_20_1/...} in the shim index) and take precedence;</li>
 *   <li>{@code mods.toml} becomes {@code neoforge.mods.toml}, the access transformer and mixin configs are made Mojang-named.</li>
 * </ul>
 * The result is a plain NeoForge mod that FML loads and constructs itself; no legacy lifecycle runs.
 */
public final class Forge1201Transformer {
    private static final Pattern SRG = Pattern.compile("^[mf]_\\d+_$");
    private static final String[][] PACKAGE_MOVES = {
            {"net/minecraftforge/fml/", "net/neoforged/fml/"},
            {"net/minecraftforge/eventbus/", "net/neoforged/bus/"},
            {"net/minecraftforge/api/", "net/neoforged/api/"},
            {"net/minecraftforge/forgespi/", "net/neoforged/neoforgespi/"},
            {"net/minecraftforge/", "net/neoforged/neoforge/"},
    };

    private final ShimIndex shim;
    private final Map<String, String> srg;
    private final Map<String, String> classRenames;

    public Forge1201Transformer(ShimIndex shim) {
        this.shim = shim;
        this.srg = SrgMojangTable.load();
        this.classRenames = loadRenames();
    }

    private static volatile Map<String, String> renamesCache;

    private static synchronized Map<String, String> loadRenames() {
        if (renamesCache != null) return renamesCache;
        Map<String, String> map = new HashMap<>();
        for (String f : List.of("forge-event-renames.tsv", "forge-fml-renames.tsv", "forge-curated-renames.tsv")) {
            try (InputStream in = Forge1201Transformer.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/" + f)) {
                if (in == null) continue;
                for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                    if (l.startsWith("#")) continue;
                    String[] a = l.strip().split("\t");
                    if (a.length == 2) map.put(a[0], a[1]);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return renamesCache = map;
    }

    // ---- names ----------------------------------------------------------------------------------------------------------------

    /** Maps one class name; names outside Forge are returned unchanged (Minecraft's own class names are the same in both runtimes). */
    String mapClass(String name) {
        if (!name.startsWith("net/minecraftforge/")) return name;
        String facade = LegacyVersion.V1_20_1.shimPrefix() + name;
        if (shim.contains(facade)) return facade;
        String direct = classRenames.get(name);
        if (direct != null) return direct;
        int dollar = name.indexOf('$');
        if (dollar > 0) {
            String outer = mapClass(name.substring(0, dollar));
            if (!outer.equals(name.substring(0, dollar))) return outer + name.substring(dollar);
        }
        for (String[] move : PACKAGE_MOVES) if (name.startsWith(move[0])) return move[1] + name.substring(move[0].length());
        return name;
    }

    private final class Mapper extends Remapper {
        @Override
        public String map(String internalName) {
            String r = mapClass(internalName);
            if (r.equals(internalName)) {
                String mc = Mc1201Tables.get().classes.get(internalName);
                if (mc != null) return mc;
            }
            return r;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            if (name.length() > 3 && name.charAt(1) == '_') {
                String m = srg.get(name);
                if (m != null) name = m;
            }
            return Mc1201Tables.get().renamedMember(owner, name, descriptor);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            if (name.length() > 3 && name.charAt(1) == '_') {
                String m = srg.get(name);
                if (m != null) name = m;
            }
            return Mc1201Tables.get().renamedMember(owner, name, ":" + descriptor);
        }

        @Override
        public String mapInvokeDynamicMethodName(String name, String descriptor) {
            // a lambda for a functional interface whose method was renamed (FriendlyByteBuf.Writer.accept is StreamEncoder.encode): the factory returns the interface
            String iface = Type.getReturnType(descriptor).getInternalName();
            String renamed = Mc1201Tables.get().renamedMember(iface, name, "*");
            if (!renamed.equals(name)) return renamed;
            return mapMethodName(null, name, descriptor);
        }
    }

    // ---- the jar --------------------------------------------------------------------------------------------------------------

    public LegacyJarTransformer.Result transform(LegacyJar jar, Path output) throws IOException {
        LegacyJarTransformer.Result result = new LegacyJarTransformer.Result();
        Map<String, byte[]> classes = new LinkedHashMap<>();
        Map<String, byte[]> other = new LinkedHashMap<>();
        Manifest manifest = null;
        try (ZipFile zip = new ZipFile(jar.existing().toFile())) {
            for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                byte[] data;
                try (InputStream in = zip.getInputStream(e)) {
                    data = in.readAllBytes();
                }
                String name = e.getName();
                if (name.equals("META-INF/MANIFEST.MF")) manifest = new Manifest(new ByteArrayInputStream(data));
                else if (name.endsWith(".class") && !name.startsWith("META-INF/")) classes.put(name.substring(0, name.length() - 6), data);
                else other.put(name, data);
            }
        }
        Mapper mapper = new Mapper();
        Map<String, byte[]> outClasses = new LinkedHashMap<>();
        int stringsMapped = 0;
        int rewrites = 0;
        List<ClassNode> nodes = new ArrayList<>();
        Mc1201Hierarchy hierarchy = new Mc1201Hierarchy();
        for (var e : classes.entrySet()) {
            ClassNode node = new ClassNode();
            try {
                new ClassReader(e.getValue()).accept(new ClassRemapper(node, mapper), 0);
            } catch (RuntimeException ex) {
                result.notes.add("Skipped unreadable class " + e.getKey() + ": " + ex);
                continue;
            }
            hierarchy.add(node);
            nodes.add(node);
        }
        Mc1201Migration.Counts mc = new Mc1201Migration.Counts();
        for (ClassNode node : nodes) {
            stringsMapped += mapSrgStrings(node);
            int viaCalls = CallRewrites1201.apply(node);
            viaCalls += ForgeApiRewrites1201.apply(node);
            if (ForgeApiRewrites1201.markProviders(node)) viaCalls++;
            rewrites += viaCalls;
            Mc1201Migration.Counts changed = Mc1201Migration.apply(node, hierarchy);
            mc.add(changed);
            byte[] bytes;
            if (viaCalls + changed.total() > 0) {
                // the stack at branch targets changed: frames are computed again
                try {
                    ClassWriter writer = hierarchy.frameWriter();
                    node.accept(writer);
                    bytes = writer.toByteArray();
                } catch (RuntimeException ex) {
                    result.notes.add("Frames of " + node.name + " could not be recomputed (" + ex + "); the class keeps the old ones and may fail to verify");
                    ClassWriter writer = new ClassWriter(0);
                    node.accept(writer);
                    bytes = writer.toByteArray();
                }
            } else {
                ClassWriter writer = new ClassWriter(0);
                node.accept(writer);
                bytes = writer.toByteArray();
            }
            outClasses.put(node.name + ".class", bytes);
            result.classes++;
        }
        if (mc.total() > 0) result.notes.add("Minecraft 1.20.1 to 1.21.1 migration: " + mc);
        if (rewrites > 0) result.notes.add(rewrites + " call site(s) rewritten for changed 1.21.1 signatures (ResourceLocation factories, event bus post)");
        if (stringsMapped > 0) result.notes.add(stringsMapped + " reflection string(s) with SRG names now use Mojang names");

        Map<String, byte[]> outResources = new LinkedHashMap<>();
        for (var e : other.entrySet()) {
            String name = e.getKey();
            if (LegacyJarTransformer.isSignatureFile(name)) continue;
            // multi-loader jars: Sinytra Connector would load the same jar a second time as a Fabric mod
            if (name.equals("fabric.mod.json") || name.equals("quilt.mod.json")) continue;
            byte[] data = e.getValue();
            if (name.startsWith("META-INF/jarjar/")) {
                if (name.endsWith(".jar")) {
                    byte[] nested = nestedJar(name, data, result);
                    if (nested == null) continue;
                    data = nested;
                } else if (name.equals("META-INF/jarjar/metadata.json")) {
                    data = jarJarMetadata(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                }
            }
            if (name.equals("META-INF/mods.toml")) {
                outResources.put("META-INF/neoforge.mods.toml", modsToml(new String(data, StandardCharsets.UTF_8), mixinConfigs(manifest)).getBytes(StandardCharsets.UTF_8));
                continue;
            }
            if (name.equals("META-INF/accesstransformer.cfg")) data = accessTransformer(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
            else if (name.endsWith(".json") && looksLikeMixinConfig(data)) data = stripRefmap(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
            else if (name.equals("pack.mcmeta")) data = packMcmeta(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
            outResources.put(name, data);
        }

        Manifest outManifest = LegacyJarTransformer.cleanManifest(manifest, jar);
        String originalSha = Provenance.sha256Hex(jar.existing());
        Provenance.addManifest(outManifest.getMainAttributes(), jar, originalSha);
        outResources.put(Provenance.RESOURCE, Provenance.json(jar, originalSha, LegacyJarTransformer.TRANSFORMER_VERSION));
        LegacyJarTransformer.write(output, outManifest, outClasses, outResources);
        return result;
    }

    // ---- jar-in-jar ------------------------------------------------------------------------------------------------------------

    /** Nested libraries NeoForge 1.21.1 ships itself: a second (older) copy would split the module. */
    static boolean providedByHost(String path) {
        String n = path.substring(path.lastIndexOf('/') + 1).toLowerCase();
        return n.startsWith("mixinextras");
    }

    /**
     * A jar inside {@code META-INF/jarjar}: dropped when the host provides it (MixinExtras), translated when it is a Forge 1.20.1 mod of its own (SpectreLib,
     * Registrate...), kept otherwise. Null drops it.
     */
    private byte[] nestedJar(String path, byte[] data, LegacyJarTransformer.Result result) throws IOException {
        if (providedByHost(path)) {
            result.notes.add("Dropped nested " + path.substring(path.lastIndexOf('/') + 1) + " (NeoForge provides it)");
            return null;
        }
        Path in = Files.createTempFile("lc1201-in-", ".jar");
        Path out = Files.createTempFile("lc1201-out-", ".jar");
        try {
            Files.write(in, data);
            LegacyJar nested = LegacyJarInspector.inspect(in);
            if (nested == null || nested.version != LegacyVersion.V1_20_1) return data;
            new Forge1201Transformer(shim).transform(nested, out);
            result.notes.add("Translated nested " + path.substring(path.lastIndexOf('/') + 1));
            return Files.readAllBytes(out);
        } finally {
            Files.deleteIfExists(in);
            Files.deleteIfExists(out);
        }
    }

    /** {@code META-INF/jarjar/metadata.json}: the entries of dropped nested jars go. */
    static String jarJarMetadata(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray jars = root.getAsJsonArray("jars");
            if (jars == null) return json;
            JsonArray kept = new JsonArray();
            for (JsonElement el : jars) {
                String path = el.getAsJsonObject().has("path") ? el.getAsJsonObject().get("path").getAsString() : "";
                if (!providedByHost(path)) kept.add(el);
            }
            root.add("jars", kept);
            return root.toString();
        } catch (RuntimeException e) {
            return json;
        }
    }

    /** Reflection by SRG name ({@code ObfuscationReflectionHelper.findField(Foo.class, "f_1_")}): the runtime has Mojang names. */
    private int mapSrgStrings(ClassNode node) {
        int n = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions == null) continue;
            for (AbstractInsnNode in : m.instructions) {
                if (in instanceof LdcInsnNode l && l.cst instanceof String s && s.length() > 3 && s.charAt(1) == '_' && SRG.matcher(s).matches()) {
                    String mojang = srg.get(s);
                    if (mojang != null) {
                        l.cst = mojang;
                        n++;
                    }
                }
            }
        }
        return n;
    }

    // ---- metadata and data -------------------------------------------------------------------------------------------------

    private static final Pattern KEY_LINE = Pattern.compile("^(\\s*)([A-Za-z_]+)(\\s*=\\s*)(.*)$");

    /** mods.toml to neoforge.mods.toml: same format; the loader, Minecraft and "forge" dependencies are retargeted, {@code mandatory} became {@code type}. */
    /** The mixin configs a Forge 1.20.1 jar names in its manifest ({@code MixinConfigs: a.mixins.json,b.mixins.json}); NeoForge wants them in the mods.toml. */
    static List<String> mixinConfigs(Manifest manifest) {
        String value = manifest == null ? null : manifest.getMainAttributes().getValue("MixinConfigs");
        List<String> out = new ArrayList<>();
        if (value != null) for (String c : value.split("[,\\s]+")) if (!c.isBlank()) out.add(c.strip());
        return out;
    }

    static String modsToml(String text) {
        return modsToml(text, List.of());
    }

    static String modsToml(String text, List<String> mixinConfigs) {
        StringBuilder out = new StringBuilder("# Generated by Legacy Connector from the Forge 1.20.1 mods.toml\n");
        List<String> block = new ArrayList<>();
        boolean inDependency = false;
        boolean multi = false; // inside a ''' or """ string: its lines are text, whatever they look like
        for (String line : text.split("\\R", -1)) {
            boolean wasMulti = multi;
            if ((count(line, "'''") + count(line, "\"\"\"")) % 2 == 1) multi = !multi;
            if (wasMulti) {
                if (inDependency) block.add(line);
                else out.append(line).append('\n');
                continue;
            }
            String t = line.strip();
            if (t.startsWith("[")) {
                flushDependency(out, block, inDependency);
                block.clear();
                inDependency = t.startsWith("[[dependencies.");
                out.append(inDependency ? "" : line).append(inDependency ? "" : "\n");
                if (inDependency) block.add(line);
                continue;
            }
            if (inDependency) {
                block.add(line);
                continue;
            }
            Matcher m = KEY_LINE.matcher(line);
            if (m.matches() && m.group(2).equals("loaderVersion")) {
                out.append(m.group(1)).append("loaderVersion").append(m.group(3)).append("\"[1,)\"\n");
                continue;
            }
            out.append(line).append('\n');
        }
        flushDependency(out, block, inDependency);
        for (String config : mixinConfigs) out.append("\n[[mixins]]\nconfig=\"").append(config).append("\"\n");
        return out.toString();
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + what.length())) n++;
        return n;
    }

    private static void flushDependency(StringBuilder out, List<String> block, boolean inDependency) {
        if (!inDependency || block.isEmpty()) return;
        String modId = null;
        for (String l : block) {
            Matcher m = KEY_LINE.matcher(l);
            if (m.matches() && m.group(2).equals("modId")) modId = m.group(4).replaceAll("^[\"']|[\"']\\s*(#.*)?$", "");
        }
        boolean wroteType = false;
        for (String l : block) {
            Matcher m = KEY_LINE.matcher(l);
            if (!m.matches()) {
                out.append(l).append('\n');
                continue;
            }
            String key = m.group(2);
            if (key.equals("modId") && "forge".equals(modId)) out.append(m.group(1)).append("modId").append(m.group(3)).append("\"neoforge\"\n");
            else if (key.equals("versionRange") && "forge".equals(modId)) out.append(m.group(1)).append("versionRange").append(m.group(3)).append("\"[21.1,)\"\n");
            else if (key.equals("versionRange") && "minecraft".equals(modId)) out.append(m.group(1)).append("versionRange").append(m.group(3)).append("\"[1.21.1,1.22)\"\n");
            else if (key.equals("mandatory")) {
                out.append(m.group(1)).append("type").append(m.group(3)).append(m.group(4).strip().startsWith("true") ? "\"required\"" : "\"optional\"").append('\n');
                wroteType = true;
            } else out.append(l).append('\n');
        }
        if (!wroteType && block.stream().noneMatch(l -> l.strip().startsWith("type"))) out.append("type=\"required\"\n");
    }

    private static final Pattern AT_LINE = Pattern.compile("^(\\s*(?:public|protected|private|default)(?:[-+]f)?\\s+\\S+\\s+)([mf]_\\d+_)(.*)$");

    /** Forge access transformer: SRG member names to Mojang names (class names and descriptors are the same). */
    String accessTransformer(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\\R", -1)) {
            Matcher m = AT_LINE.matcher(line);
            if (m.matches() && srg.containsKey(m.group(2))) out.append(m.group(1)).append(srg.get(m.group(2))).append(m.group(3));
            else out.append(line);
            out.append('\n');
        }
        return out.toString();
    }

    private static boolean looksLikeMixinConfig(byte[] data) {
        if (data.length > 200_000) return false;
        String s = new String(data, StandardCharsets.UTF_8);
        return s.contains("\"package\"") && s.contains("\"refmap\"");
    }

    /** Mixin refmaps translate Mojang names to SRG for production; 1.21.1 runs with Mojang names, which the annotations already use. */
    static String stripRefmap(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            root.remove("refmap");
            return root.toString();
        } catch (RuntimeException e) {
            return json.replaceAll("\"refmap\"\\s*:\\s*\"[^\"]*\"\\s*,?", "");
        }
    }

    /** Resource and data pack formats of 1.21.1 (34 and 48). */
    static String packMcmeta(String json) {
        return json.replaceAll("(\"pack_format\"\\s*:\\s*)\\d+", "$134");
    }
}
