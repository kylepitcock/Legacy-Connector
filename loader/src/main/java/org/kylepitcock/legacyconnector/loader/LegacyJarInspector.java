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
    /** True when a zip in mods/ holds class files (a mod distributed as a zip, as most 1.5.2 ones were) rather than only resources. */
    public static boolean zipHasClasses(Path path) {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                if (entries.nextElement().getName().endsWith(".class")) return true;
            }
        } catch (IOException | RuntimeException e) {
            // not a readable zip: leave it to whoever else looks at it
        }
        return false;
    }

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
            // A 1.12.2 library jar built from a 1.15 template keeps a stray mods.toml next to its FML coremod plugin (Obfuscate 0.4.2 for 1.12.2).
            boolean strayModsToml = zip.getEntry("META-INF/neoforge.mods.toml") == null && zip.getEntry("fabric.mod.json") == null
                    && zip.getEntry("META-INF/MANIFEST.MF") != null && legacyCorePlugin(zip);
            // Anything that already speaks a modern metadata format is not ours to touch.
            if (zip.getEntry("META-INF/neoforge.mods.toml") != null
                    || (zip.getEntry("META-INF/mods.toml") != null && !strayModsToml)
                    || zip.getEntry("fabric.mod.json") != null
                    || zip.getEntry("quilt.mod.json") != null) {
                return null;
            }

            LegacyJar jar = new LegacyJar(path);
            readManifest(zip, jar);

            Map<String, JsonObject> infoById = readMcModInfo(zip, jar);
            int refs17 = 0, refs112 = 0;
            Markers markers = new Markers();
            List<LegacyModDecl> found = new ArrayList<>();
            List<String> modLoaderMods = new ArrayList<>();

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
                markers.scan(pool, entry.getName());
                // ModLoader (1.4.x): a mod is a class extending BaseMod (net.minecraft.src.BaseMod, or the bare name in a reobfuscated jar)
                String superName = reader.getSuperName();
                // (ModLoader only loads classes named mod_*; the others of a jar, such as MCreator's per-element mcreator_*, are helpers the mod_ class constructs)
                if (("BaseMod".equals(superName) || "net/minecraft/src/BaseMod".equals(superName))
                        && reader.getClassName().substring(reader.getClassName().lastIndexOf('/') + 1).startsWith("mod_")) {
                    modLoaderMods.add(reader.getClassName());
                }

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
                // no FML at all: a Beta 1.7.3 ModLoader / Forge 1.0 mod (in mods/b1.7.3/, or linking to b1.7.3's names), else a 1.4.7
                // ModLoader mod (BaseMod subclasses: decided below) or not a legacy mod; mods/1.4.7/ never holds b1.7.3 jars
                Path folder = path.getParent() == null ? null : path.getParent().getFileName();
                if (folder == null || !folder.toString().equals("1.4.7")) {
                    LegacyJar beta = BetaJars.inspect(zip, path, jar);
                    if (beta != null) return beta;
                }
                if (modLoaderMods.isEmpty()) return null;
            }
            // ModLoader-only jars have no @Mod: each BaseMod subclass is a mod (id = its class name, as ModLoader showed it)
            if (found.isEmpty()) {
                for (String cls : modLoaderMods) {
                    LegacyModDecl decl = new LegacyModDecl();
                    decl.mainClass = cls;
                    decl.legacyId = cls.substring(cls.lastIndexOf('/') + 1);
                    decl.modLoader = true;
                    found.add(decl);
                }
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

            if (unique.isEmpty() && jar.coreModClass != null && !jar.coreModClass.isBlank()) jar.containerModIds.addAll(infoById.keySet());
            if (jar.coreModClass != null && !jar.coreModClass.isBlank()) addCoreModContainerIds(zip, jar);
            jar.version = chooseVersion(zip, jar, infoById, refs17, refs112, markers, !modLoaderMods.isEmpty());
            return jar.version == null ? null : jar;
        }
    }

    /**
     * A coremod jar may ship a {@code DummyModContainer} that FML registers as a mod of its own (OpenModsLib's {@code OpenModsCore}); other
     * mods then depend on it with {@code required-after}. Its id is the string stored into {@code ModMetadata.modId} by the container's
     * constructor, and counts as installed since the container lives in this jar.
     */
    private static void addCoreModContainerIds(ZipFile zip, LegacyJar jar) {
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (!e.getName().endsWith(".class") || e.getName().indexOf('$') >= 0) continue;
                byte[] bytes;
                try (InputStream in = zip.getInputStream(e)) {
                    bytes = in.readAllBytes();
                }
                ClassReader cr = new ClassReader(bytes);
                String sup = cr.getSuperName();
                if (sup == null || !sup.endsWith("/DummyModContainer")) continue;
                cr.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                        if (!name.equals("<init>")) return null;
                        return new org.objectweb.asm.MethodVisitor(Opcodes.ASM9) {
                            String lastString;

                            @Override
                            public void visitLdcInsn(Object value) {
                                lastString = value instanceof String str ? str : null;
                            }

                            @Override
                            public void visitFieldInsn(int opcode, String owner, String fname, String fdesc) {
                                if (opcode == Opcodes.PUTFIELD && fname.equals("modId") && lastString != null && !jar.containerModIds.contains(lastString)) {
                                    jar.containerModIds.add(lastString);
                                }
                                lastString = null;
                            }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        } catch (IOException | RuntimeException ignored) {
            // best effort: the dependency check only loses an id
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

    /**
     * What tells Forge 1.6.4 code from 1.7.x code, both of which link against {@code cpw.mods.fml}: names that exist in only one of the two
     * (counted per class, with the first example kept for the report).
     */
    static final class Markers {
        // 1.6.4 only: Icon (1.7: IIcon), the IconRegister, the 1.6 packet and event APIs, the Forge classes 1.7 moved, the SRG ids of Block.blocksList / Item.itemsList
        private static final String[] V164 = {"net/minecraft/util/Icon", "net/minecraft/client/renderer/texture/IconRegister",
                "net/minecraft/network/packet/Packet250CustomPayload", "net/minecraft/network/packet/Packet",
                "cpw/mods/fml/common/Mod$PreInit", "cpw/mods/fml/common/Mod$Init", "cpw/mods/fml/common/Mod$PostInit",
                "cpw/mods/fml/common/Mod$ServerStarting", "cpw/mods/fml/common/Mod$ServerStopping",
                "cpw/mods/fml/common/network/NetworkMod", "cpw/mods/fml/common/network/IPacketHandler",
                "cpw/mods/fml/common/network/PacketDispatcher", "cpw/mods/fml/common/registry/TickRegistry", "cpw/mods/fml/common/ITickHandler",
                "net/minecraftforge/event/ForgeSubscribe", "net/minecraftforge/event/Event", "net/minecraftforge/common/Configuration",
                "net/minecraftforge/common/ForgeDirection", "net/minecraftforge/liquids/", "net/minecraft/item/EnumToolMaterial",
                "net/minecraft/block/StepSound", "net/minecraft/util/EnumMovingObjectType", "net/minecraft/world/EnumGameType",
                "field_71973_m", "field_77698_e"};
        // 1.7.x only: IIcon, Blocks/Items, the 1.7 event, packet (SimpleNetworkWrapper) and tick APIs
        private static final String[] V17 = {"net/minecraft/util/IIcon", "net/minecraft/client/renderer/texture/IIconRegister",
                "net/minecraft/init/Blocks", "net/minecraft/init/Items", "cpw/mods/fml/common/eventhandler/",
                "cpw/mods/fml/common/network/simpleimpl/", "cpw/mods/fml/common/gameevent/", "net/minecraftforge/common/util/",
                "net/minecraftforge/common/config/", "net/minecraft/util/ChatComponentText", "net/minecraft/block/Block$SoundType"};

        // 1.5.2 only (absent from 1.6.4; legacy/*/hierarchy.txt diff): the string-path texture API, the Forge classes 1.6 moved
        private static final String[] V152 = {"net/minecraft/client/renderer/RenderEngine", "net/minecraft/client/renderer/texture/TextureStitched",
                "net/minecraft/client/renderer/texture/Texture", "net/minecraft/client/texturepacks/", "net/minecraftforge/common/ISidedInventory",
                "net/minecraftforge/common/IThrowableEntity", "net/minecraftforge/common/IArmorTextureProvider", "net/minecraft/block/BlockCloth",
                "net/minecraftforge/client/event/TextureLoadEvent", "cpw/mods/fml/client/ITextureFX"};
        // 1.6 and later only (absent from 1.5.2): EntityLivingBase, attributes, the resource system, @EventHandler, the 1.6 blocks
        private static final String[] V16 = {"net/minecraft/entity/EntityLivingBase", "net/minecraft/entity/ai/attributes/", "net/minecraft/util/ResourceLocation",
                "net/minecraft/client/resources/", "cpw/mods/fml/common/Mod$EventHandler", "net/minecraft/client/renderer/texture/TextureManager",
                "net/minecraft/client/renderer/texture/TextureAtlasSprite", "net/minecraft/client/renderer/entity/RendererLivingEntity",
                "net/minecraft/client/entity/AbstractClientPlayer", "net/minecraft/entity/passive/EntityHorse", "net/minecraft/block/BlockColored",
                "net/minecraft/block/BlockRotatedPillar", "cpw/mods/fml/common/Optional"};

        // 1.4.7 only (absent from 1.5.2; legacy/*/hierarchy.txt diff): FML's texture-FX helpers, the dispenser hooks, the 1.4 minecart registry and special-spawn event
        private static final String[] V147 = {"cpw/mods/fml/client/FMLTextureFX", "cpw/mods/fml/client/SpriteHelper", "cpw/mods/fml/common/IDispenseHandler",
                "cpw/mods/fml/common/IDispenserHandler", "net/minecraftforge/common/MinecartRegistry",
                "net/minecraftforge/event/entity/living/LivingSpecialSpawnEvent"};
        // 1.5.2 and later only (absent from 1.4.7): Forge classes added in 1.5. (Packaged vanilla names do not tell: the last 1.4.7 builds of Forge, 6.6.2.5xx, already
        // compiled mods against MCP 7 package names, so 1.4.7 jars mix notch names with a few net/minecraft/world/World.)
        private static final String[] V152_ONLY = {"net/minecraftforge/common/BiomeDictionary", "net/minecraftforge/common/IExtendedEntityProperties",
                "net/minecraftforge/common/FakePlayer", "net/minecraftforge/client/event/TextureStitchEvent", "net/minecraftforge/client/event/RenderGameOverlayEvent",
                "net/minecraftforge/client/event/RenderLivingEvent", "net/minecraftforge/client/event/RenderPlayerEvent", "net/minecraftforge/client/model/",
                "net/minecraftforge/common/network/", "cpw/mods/fml/relauncher/IClassNameTransformer"};
        /** Classes the ModLoader emulation keeps in net.minecraft.src (default package in a reobfuscated jar); every other net/minecraft/src name is flat vanilla. */
        private static final java.util.Set<String> ML_NAMES = java.util.Set.of("BaseMod", "ModLoader", "MLProp", "ModTextureStatic", "ModTextureAnimation",
                "TradeEntry", "EntityRendererProxy", "FMLRenderAccessLibrary");

        int v164, v17, v152, v16, v147, v152only, flat;
        String example164, example17, example152, example16, example147, example152only, exampleFlat;

        /** True when {@code pool} references a vanilla class by its flat MCP 6.x name {@code net/minecraft/src/<Name>} (1.4.7). */
        private static String flatVanilla(String pool) {
            int from = 0;
            for (int i; (i = pool.indexOf("net/minecraft/src/", from)) >= 0; from = i + 1) {
                int start = i + "net/minecraft/src/".length();
                int end = start;
                while (end < pool.length() && Character.isJavaIdentifierPart(pool.charAt(end))) end++;
                String name = pool.substring(start, end);
                if (!name.isEmpty() && !ML_NAMES.contains(name)) return name;
            }
            return null;
        }

        /** True when {@code pool} (one name per line) holds {@code name} as a whole class or field name (or a package prefix when it ends with '/'). */
        private static boolean has(String pool, String name) {
            int from = 0;
            for (int i; (i = pool.indexOf(name, from)) >= 0; from = i + 1) {
                if (name.endsWith("/")) return true;
                int end = i + name.length();
                // class names sit inside descriptors ("(Lnet/minecraft/util/Icon;)V"); a bare member name needs a boundary before it too
                boolean before = name.indexOf('/') >= 0 || i == 0 || !Character.isJavaIdentifierPart(pool.charAt(i - 1));
                boolean after = end >= pool.length() || !Character.isJavaIdentifierPart(pool.charAt(end));
                if (before && after) return true;
            }
            return false;
        }

        void scan(String pool, String entry) {
            if (entry.startsWith("META-INF/")) return;
            for (String m : V164) {
                if (has(pool, m)) {
                    v164++;
                    if (example164 == null) example164 = m.replaceAll("^.*/", "");
                    break;
                }
            }
            for (String m : V152) {
                if (has(pool, m)) {
                    v152++;
                    if (example152 == null) example152 = m.replaceAll("/$", "").replaceAll("^.*/", "");
                    break;
                }
            }
            for (String m : V16) {
                if (has(pool, m)) {
                    v16++;
                    if (example16 == null) example16 = m.replaceAll("/$", "").replaceAll("^.*/", "");
                    break;
                }
            }
            for (String m : V17) {
                if (has(pool, m)) {
                    v17++;
                    if (example17 == null) example17 = m.replaceAll("^.*/", "");
                    break;
                }
            }
            for (String m : V147) {
                if (has(pool, m)) {
                    v147++;
                    if (example147 == null) example147 = m.replaceAll("^.*/", "");
                    break;
                }
            }
            for (String m : V152_ONLY) {
                if (has(pool, m)) {
                    v152only++;
                    if (example152only == null) example152only = m.replaceAll("/$", "").replaceAll("^.*/", "");
                    break;
                }
            }
            String flatName = flatVanilla(pool);
            if (flatName != null) {
                flat++;
                if (exampleFlat == null) exampleFlat = flatName;
            }
        }
    }

    /** True when a mod of the jar declares {@code acceptedMinecraftVersions} (or mcmod.info {@code mcversion}) of {@code version}. */
    private static boolean declaresVersion(LegacyJar jar, Map<String, JsonObject> infos, LegacyVersion version) {
        for (LegacyModDecl decl : jar.mods) {
            if (LegacyVersion.fromMinecraftVersion(decl.acceptedMinecraftVersions) == version) return true;
        }
        for (JsonObject info : infos.values()) {
            if (info.has("mcversion") && LegacyVersion.fromMinecraftVersion(info.get("mcversion").getAsString()) == version) return true;
        }
        return false;
    }

    /** The family of a jar that links against {@code cpw.mods.fml}: 1.5.2, 1.6.4 or 1.7.x (1.7.10 shim). */
    private static LegacyVersion chooseCpw(ZipFile zip, LegacyJar jar, Map<String, JsonObject> infos, int refs17, Markers markers) {
        if (markers.v164 > 0 && markers.v17 == 0 && markers.v152 > 0 && markers.v16 == 0) {
            jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes) and uses 1.5.2-only API (" + markers.v152 + " classes, e.g. " + markers.example152 + ")";
            return LegacyVersion.V1_5_2;
        }
        if (markers.v164 > 0 && markers.v17 == 0) {
            // the 1.6.4 markers (Icon, @PreInit, ForgeSubscribe...) exist in 1.5.2 as well: with neither side's own API present, the declared version decides
            // (a jar that also carries 1.6 API, like Immibis Core's texture switch, still is a 1.5.2 jar when it declares 1.5.2 and uses 1.5.2-only API)
            if ((markers.v16 == 0 || markers.v152 > 0) && declaresVersion(jar, infos, LegacyVersion.V1_5_2)) {
                jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes), 1.5.2 declared";
                return LegacyVersion.V1_5_2;
            }
            // nothing declared: a file name naming 1.5.2 (and no 1.6-only API) is the last hint
            if (markers.v16 == 0 && jar.fileName().matches("(?i).*(1\\.5\\.2|mc15x).*")) {
                jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes), file name says 1.5.2";
                return LegacyVersion.V1_5_2;
            }
            jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes) and uses 1.6.4-only API (" + markers.v164 + " classes, e.g. " + markers.example164 + ")";
            return LegacyVersion.V1_6_4;
        }
        if (markers.v164 == 0 && markers.v17 == 0) {
            // nothing in the code tells them apart: the declared Minecraft version does
            if (declaresVersion(jar, infos, LegacyVersion.V1_5_2)) {
                jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes), 1.5.2 declared";
                return LegacyVersion.V1_5_2;
            }
            if (markers.v16 == 0 && jar.fileName().matches("(?i).*(1\\.5\\.2|mc15x).*")) {
                jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes), file name says 1.5.2";
                return LegacyVersion.V1_5_2;
            }
            for (LegacyModDecl decl : jar.mods) {
                if (LegacyVersion.fromMinecraftVersion(decl.acceptedMinecraftVersions) == LegacyVersion.V1_6_4) {
                    jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes), @Mod(acceptedMinecraftVersions=" + decl.acceptedMinecraftVersions + ")";
                    return LegacyVersion.V1_6_4;
                }
            }
            for (JsonObject info : infos.values()) {
                if (info.has("mcversion") && LegacyVersion.fromMinecraftVersion(info.get("mcversion").getAsString()) == LegacyVersion.V1_6_4) {
                    jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes), mcmod.info mcversion=" + info.get("mcversion").getAsString();
                    return LegacyVersion.V1_6_4;
                }
            }
            // a jar that names Minecraft by obfuscated (notch) names cannot be 1.7.x: the obfuscation that fits is the version (Advanced Survival declares nothing)
            double[] fp = NotchFingerprint.scores(zip);
            if (fp != null && fp[2] >= 0.6 && fp[2] > fp[1] + 0.15) {
                jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes), obfuscated names match Minecraft 1.6.4 (" + Math.round(fp[2] * 100) + "% of the member references)";
                return LegacyVersion.V1_6_4;
            }
        }
        jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes)"
                + (markers.v17 > 0 ? ", 1.7-only API (" + markers.v17 + " classes, e.g. " + markers.example17 + ")" : "");
        return LegacyVersion.V1_7_10;
    }

    /**
     * Forge 1.4.7: told from 1.5.2 and later by what it does not have. 1.4.x mods name Minecraft by obfuscated (notch) names, or by the flat
     * {@code net/minecraft/src/<Name>} of MCP 6.x; none of the packaged vanilla names, 1.5 Forge classes or 1.6/1.7 markers appear. Positive evidence, in order:
     * flat names, a 1.4.7-only FML/Forge class, the notch fingerprint (member references that resolve against the 1.4.7 table but not the 1.5.2/1.6.4 ones),
     * the declared Minecraft version. Returns null when the jar is not 1.4.7 (or nothing says so), leaving the other versions' detection untouched.
     */
    private static LegacyVersion choose147(ZipFile zip, LegacyJar jar, Map<String, JsonObject> infos, int refs17, Markers markers, boolean modLoader) {
        if (markers.v17 > 0) return null;
        // the notch fingerprint is the strongest evidence, so it comes first and overrides the 1.5.2/1.6 markers (1.4.7 jars use @PreInit, ISidedInventory...)
        boolean candidate = refs17 > 0 || modLoader;
        double[] score = candidate ? NotchFingerprint.scores(zip) : null;
        if (score != null && score[0] >= 0.6 && score[0] > score[1] + 0.15 && score[0] > score[2] + 0.15) {
            jar.versionEvidence = "obfuscated names match Minecraft 1.4.7 (" + Math.round(score[0] * 100) + "% of the member references; 1.5.2 " + Math.round(score[1] * 100)
                    + "%, 1.6.4 " + Math.round(score[2] * 100) + "%)";
            return LegacyVersion.V1_4_7;
        }
        // an older or newer version's obfuscation clearly fits better (a 1.5.2 ModLoader mod can name flat net/minecraft/src classes as well)
        if (score != null && (score[1] >= 0.6 || score[2] >= 0.6) && Math.max(score[1], score[2]) > score[0] + 0.15) return null;
        if (markers.v16 > 0 || markers.v152only > 0) return null;
        if (markers.flat > 0) {
            jar.versionEvidence = "names Minecraft classes by their flat MCP 6.x names (net/minecraft/src/" + markers.exampleFlat + ")";
            return LegacyVersion.V1_4_7;
        }
        if (markers.v147 > 0) {
            jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes) and uses 1.4.7-only API (" + markers.v147 + " classes, e.g. " + markers.example147 + ")";
            return LegacyVersion.V1_4_7;
        }
        if (!candidate) return null;
        if (declaresVersion(jar, infos, LegacyVersion.V1_4_7)) {
            jar.versionEvidence = "1.4.x declared" + (modLoader ? " (ModLoader mod)" : "");
            return LegacyVersion.V1_4_7;
        }
        // tiny jars give the fingerprint too little to count: their file name (BiblioCraft-1.4.7.zip, PonySocks147.jar, ...MC1.4...) is the last hint
        String fileName = jar.path.getFileName().toString().toLowerCase();
        if (java.util.regex.Pattern.compile("(^|[^0-9.])1\\.4\\.[67]([^0-9]|$)|mc1\\.4|[a-z]147([^0-9]|$)").matcher(fileName).find()) {
            jar.versionEvidence = "links against cpw.mods.fml (" + refs17 + " classes), file name names Minecraft 1.4";
            return LegacyVersion.V1_4_7;
        }
        if (modLoader && refs17 == 0) {
            // BaseMod exists up to 1.5.2; without any other evidence a bare ModLoader mod is taken for the oldest supported version
            jar.versionEvidence = "ModLoader mod (extends BaseMod), no version evidence: taken for 1.4.7";
            return LegacyVersion.V1_4_7;
        }
        return null;
    }

    /**
     * Which version's obfuscation a notch-named jar was built against: the share of its member references (to default-package owners the jar does not define)
     * that resolve in the notch table of 1.4.7, 1.5.2 and 1.6.4 respectively ({@code legacy/<id>/notch.txt}, same owner, name and descriptor). The obfuscation
     * is re-randomized with every Minecraft release, so only the right version matches more than a few percent.
     */
    static final class NotchFingerprint {
        private NotchFingerprint() {}

        /** {1.4.7, 1.5.2, 1.6.4} match ratios, or null when the jar has too few member references into obfuscated classes to say. */
        static double[] scores(ZipFile zip) {
            java.util.Set<String> own = new java.util.HashSet<>();
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                String n = en.nextElement().getName();
                if (n.endsWith(".class")) own.add(n.substring(0, n.length() - 6));
            }
            org.kylepitcock.legacyconnector.loader.mapping.NotchNames[] tables = {
                    org.kylepitcock.legacyconnector.loader.mapping.NotchNames.get("1.4.7"),
                    org.kylepitcock.legacyconnector.loader.mapping.NotchNames.get("1.5.2"),
                    org.kylepitcock.legacyconnector.loader.mapping.NotchNames.get("1.6.4")};
            int[] hits = new int[3];
            int[] total = {0};
            en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                try (InputStream in = zip.getInputStream(entry)) {
                    new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            return new org.objectweb.asm.MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitMethodInsn(int opcode, String owner, String mname, String mdesc, boolean itf) {
                                    count(owner, mname, mdesc, false);
                                }

                                @Override
                                public void visitFieldInsn(int opcode, String owner, String fname, String fdesc) {
                                    count(owner, fname, fdesc, true);
                                }

                                private void count(String owner, String mname, String mdesc, boolean field) {
                                    // only members whose name is itself obfuscated tell the versions apart ("a", "b"... or "func_" free names of 1-3 letters)
                                    if (owner.indexOf('/') >= 0 || own.contains(owner) || mname.length() > 3 || mname.startsWith("<")) return;
                                    total[0]++;
                                    for (int v = 0; v < 3; v++) {
                                        String r = field ? tables[v].field(owner, mname, null) : tables[v].method(owner, mname, mdesc, null);
                                        if (r != null) hits[v]++;
                                    }
                                }
                            };
                        }
                    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                } catch (IOException | RuntimeException e) {
                    // an unreadable class does not count
                }
            }
            if (total[0] < 15) return null;
            return new double[] {hits[0] / (double) total[0], hits[1] / (double) total[0], hits[2] / (double) total[0]};
        }
    }

    private static LegacyVersion chooseVersion(ZipFile zip, LegacyJar jar, Map<String, JsonObject> infos, int refs17, int refs112, Markers markers, boolean modLoader) {
        // 0. Forge 1.4.7 / ModLoader: decided on what it lacks (see choose147); leaves the other versions as they were
        if (refs112 == 0) {
            LegacyVersion v147 = choose147(zip, jar, infos, refs17, markers, modLoader);
            if (v147 != null) return v147;
        }
        // 1. The package the code actually links against is the most reliable signal.
        if (refs17 > 0 && refs112 == 0) return chooseCpw(zip, jar, infos, refs17, markers);
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

    private static boolean legacyCorePlugin(ZipFile zip) throws IOException {
        try (InputStream in = zip.getInputStream(zip.getEntry(JarFile.MANIFEST_NAME))) {
            return new Manifest(in).getMainAttributes().getValue("FMLCorePlugin") != null;
        }
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
            if (list == null && root.isJsonObject()) list = root.getAsJsonObject().getAsJsonArray("modlist"); // TreeCapitator, ...
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
                    extra.append(key.equals("dependants") ? ";before:" : key.equals("requiredMods") ? ";required-after:" : ";after:").append(id.getAsString());
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

            /** The main class carries {@code @SideOnly(Side.CLIENT)}: a client mod (JourneyMap) that must not be constructed on a dedicated server. */
            boolean sideClient;

            @Override
            public void visitEnd() {
                if (sideClient && result[0] != null) result[0].clientSideOnly = true;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (descriptor.equals("Lcpw/mods/fml/relauncher/SideOnly;") || descriptor.equals("Lnet/minecraftforge/fml/relauncher/SideOnly;")) {
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitEnum(String name, String enumDescriptor, String value) {
                            if ("CLIENT".equals(value)) sideClient = true;
                        }
                    };
                }
                if (!descriptor.equals(MOD_1_7) && !descriptor.equals(MOD_1_12)) return null;
                LegacyModDecl decl = new LegacyModDecl();
                decl.mainClass = className;
                decl.clientSideOnly = sideClient;
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
