package org.kylepitcock.legacyconnector.loader;

import org.kylepitcock.legacyconnector.loader.mapping.BetaMappings;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Beta 1.7.3 ModLoader / Forge 1.0.x jars: detection, and the mod-set wide index the deobfuscator names classes with.
 *
 * <p>Such a jar has no metadata. It is recognised by its {@code mod_*} classes (ModLoader's entry points, default package)
 * or its references to the ModLoader/Forge API, and confirmed by how many of its references to the obfuscated game resolve
 * in the b1.7.3 client or server names (ModLoader mods of 1.0-1.2.5 look the same but name other members). A jar in
 * {@code mods/b1.7.3/} is taken as b1.7.3 without that check.</p>
 *
 * <p>Classes in the default package (all of a typical ModLoader mod) cannot live in a JPMS module, and mods also put classes in
 * {@code net/minecraft/src/}, which belongs to the shim. Both are moved to {@code legacyconnector_b173/<mod>/}; addons name the
 * classes of the mod they extend ({@code mod_BuildCraftCore}), so the index of moved names covers every b1.7.3 jar of the set.
 * Translation reads the index only after every jar has been inspected.</p>
 */
public final class BetaJars {
    /** The ModLoader / ModLoaderMP API, default package, provided by the shim as {@code net/minecraft/src/<Name>}. */
    public static final Set<String> MODLOADER_API = Set.of("BaseMod", "ModLoader", "MLProp", "ModTextureStatic", "ModTextureAnimation",
            "EntityRendererProxy", "BaseModMp", "ModLoaderMp", "ISpawnable", "Packet230ModLoader", "NetClientHandlerEntity", "EntityTrackerEntry2");
    /** Where the ModLoader API and Forge's {@code forge/} package are named (MCP's package for both). */
    public static final String API_PACKAGE = "net/minecraft/src/";
    /** Root of the packages default-package mod classes move to. */
    public static final String MOD_ROOT = "legacyconnector_b173/";
    /** Folder of {@code mods/} for b1.7.3 jars and zips (zips are only read from here). */
    public static final String FOLDER = "b1.7.3";

    /** Below this share of resolving game references a ModLoader jar is taken to be for another Minecraft version. */
    static final double MIN_RESOLVED = 0.6;

    /** Supertypes of a b1.7.3 mod class, in original (obfuscated, unmoved) names. */
    public record ClassMeta(String superName, String[] interfaces) {}

    /** Original name -> moved name, every b1.7.3 jar (first jar wins on duplicates). */
    private static final Map<String, String> MOVED = new ConcurrentHashMap<>();
    /** Original name -> supertypes, every class of every b1.7.3 jar (first jar wins on duplicates). */
    private static final Map<String, ClassMeta> HIERARCHY = new ConcurrentHashMap<>();
    /** Jar -> its package under {@link #MOD_ROOT}. */
    private static final Map<Path, String> PACKAGES = new ConcurrentHashMap<>();
    /** Jar -> side whose obfuscated names it uses. */
    private static final Map<Path, BetaMappings.Side> SIDES = new ConcurrentHashMap<>();
    /** Jar -> its classes (original names) with supertypes, in jar order, to rebuild the index from the jars that load. */
    private static final Map<Path, Map<String, ClassMeta>> JAR_CLASSES = new ConcurrentHashMap<>();

    private BetaJars() {}

    /** The API name a default-package / forge class of a b1.7.3 jar refers to, or null. */
    public static String apiName(String original) {
        if (original.indexOf('/') < 0 && MODLOADER_API.contains(original)) return API_PACKAGE + original;
        if (original.startsWith("forge/")) return API_PACKAGE + original;
        return null;
    }

    /** Where a mod class of some b1.7.3 jar of the set was moved, or null if it keeps its name. */
    public static String moved(String original) {
        return MOVED.get(original);
    }

    /** Supertypes of a class of some b1.7.3 jar of the set (original names), or null. */
    public static ClassMeta meta(String original) {
        return HIERARCHY.get(original);
    }

    public static BetaMappings.Side side(LegacyJar jar) {
        return SIDES.getOrDefault(jar.path, BetaMappings.Side.CLIENT);
    }

    /** The jar's package under {@link #MOD_ROOT}. */
    public static String packageOf(LegacyJar jar) {
        return PACKAGES.get(jar.path);
    }

    /** Clears the index (tests; a launch starts empty). */
    public static void reset() {
        MOVED.clear();
        HIERARCHY.clear();
        PACKAGES.clear();
        SIDES.clear();
        JAR_CLASSES.clear();
    }

    /**
     * Mods were released as a client build and a server build (ModLoaderMP), both with the same {@code mod_X} class: two mods
     * with one id stop the game. Of each such group the build for the running side is kept (the first jar when none or several
     * are). Returns the jars left out, with the reason, and rebuilds the class index from the kept jars, so references to
     * {@code mod_X} reach the copy that loads.
     */
    public static synchronized Map<LegacyJar, String> selectBuilds(List<LegacyJar> jars, String runningSide) {
        BetaMappings.Side running = "SERVER".equals(runningSide) ? BetaMappings.Side.SERVER : BetaMappings.Side.CLIENT;
        Map<String, List<LegacyJar>> byEntry = new LinkedHashMap<>();
        for (LegacyJar jar : jars) {
            if (jar.version != LegacyVersion.VB1_7_3) continue;
            for (LegacyModDecl mod : jar.mods) byEntry.computeIfAbsent(mod.legacyId, k -> new ArrayList<>()).add(jar);
        }
        Map<LegacyJar, String> dropped = new LinkedHashMap<>();
        for (var e : byEntry.entrySet()) {
            List<LegacyJar> group = new ArrayList<>(e.getValue());
            group.removeIf(dropped::containsKey);
            if (group.size() < 2) continue;
            LegacyJar keep = group.stream().filter(j -> side(j) == running).findFirst().orElse(group.get(0));
            for (LegacyJar j : group) {
                if (j != keep) dropped.put(j, "another build of mod_" + e.getKey() + " (" + keep.fileName() + ", " + side(keep).name().toLowerCase(Locale.ROOT)
                        + ") is loaded on this " + running.name().toLowerCase(Locale.ROOT));
            }
        }
        // the index again, from the jars that load (the first jar of the folder may have been a dropped build)
        MOVED.clear();
        HIERARCHY.clear();
        for (LegacyJar jar : jars) {
            if (jar.version != LegacyVersion.VB1_7_3 || dropped.containsKey(jar)) continue;
            index(jar.path, JAR_CLASSES.getOrDefault(jar.path, Map.of()));
        }
        return dropped;
    }

    private static void index(Path path, Map<String, ClassMeta> classes) {
        BetaMappings.Names names = BetaMappings.get().side(SIDES.getOrDefault(path, BetaMappings.Side.CLIENT));
        String pkg = PACKAGES.get(path);
        for (var c : classes.entrySet()) {
            String n = c.getKey();
            HIERARCHY.putIfAbsent(n, c.getValue());
            if (names.isVanilla(n) || apiName(n) != null) continue; // base class edit / bundled API copy: dropped, never moved
            if (n.indexOf('/') < 0 || n.startsWith("net/minecraft/")) MOVED.putIfAbsent(n, MOD_ROOT + pkg + "/" + n);
        }
    }

    /**
     * Obfuscated b1.7.3 class names are one or two letters in the default package (683 client classes); the two classes
     * that kept their names are the main classes.
     */
    static boolean looksLikeGameClass(String name) {
        if (name.equals("net/minecraft/client/Minecraft") || name.equals("net/minecraft/server/MinecraftServer")) return true;
        if (name.isEmpty() || name.length() > 2) return false;
        for (int i = 0; i < name.length(); i++) if (!Character.isLetter(name.charAt(i))) return false;
        return true;
    }

    private static final class Scan {
        final Map<String, ClassMeta> classes = new LinkedHashMap<>();
        final List<String> modClasses = new ArrayList<>();
        int apiRefs;
        /** "F|M owner name desc" for members the jar uses on game classes */
        final Set<String> gameRefs = new HashSet<>();
    }

    /**
     * Returns the jar as a b1.7.3 mod (version, mod declarations, side, index entries) or null. Called for jars the FML
     * inspection did not claim.
     */
    static LegacyJar inspect(ZipFile zip, Path path, LegacyJar jar) throws IOException {
        Scan scan = new Scan();
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            String name = e.getName();
            if (e.isDirectory() || !name.endsWith(".class") || name.startsWith("META-INF/")) continue;
            byte[] bytes;
            try (InputStream in = zip.getInputStream(e)) {
                bytes = in.readAllBytes();
            }
            try {
                read(bytes, scan);
            } catch (RuntimeException ex) {
                jar.notes.add("Unreadable class file " + name);
            }
        }
        if (scan.classes.isEmpty()) return null;
        Path parent = path.getParent();
        boolean inFolder = parent != null && parent.getFileName() != null && FOLDER.equals(parent.getFileName().toString());
        if (!inFolder && scan.modClasses.isEmpty() && scan.apiRefs == 0) return null;

        BetaMappings mappings = BetaMappings.get();
        if (mappings.isEmpty()) return null; // built without the table: nothing could be named
        int[] hits = new int[BetaMappings.Side.values().length];
        int total = 0;
        for (String ref : scan.gameRefs) {
            String[] p = ref.split(" ", 4);
            if (scan.classes.containsKey(p[1])) continue; // a base class edit the jar ships itself
            total++;
            for (BetaMappings.Side side : BetaMappings.Side.values()) {
                BetaMappings.Names names = mappings.side(side);
                String hit = p[0].equals("F") ? names.resolveField(p[1], p[2], p[3]) : names.resolveMethod(p[1], p[2], p[3]);
                if (hit != null) hits[side.ordinal()]++;
            }
        }
        BetaMappings.Side side = hits[BetaMappings.Side.SERVER.ordinal()] > hits[BetaMappings.Side.CLIENT.ordinal()]
                ? BetaMappings.Side.SERVER : BetaMappings.Side.CLIENT;
        double share = total == 0 ? 1.0 : hits[side.ordinal()] / (double) total;
        // The shared ModLoader API alone does not identify a Minecraft release. Keep the existing fallback unless game members identify Beta.
        if (!inFolder && (total == 0 || share < MIN_RESOLVED)) return null;

        jar.version = LegacyVersion.VB1_7_3;
        jar.versionEvidence = (scan.modClasses.isEmpty() ? "ModLoader/Forge API references" : "ModLoader entry point " + scan.modClasses.get(0))
                + ", " + (total == 0 ? "no game references" : Math.round(share * 100) + "% of " + total + " game references resolve in b1.7.3 "
                + side.name().toLowerCase(Locale.ROOT)) + (inFolder ? ", in mods/" + FOLDER : "");

        String pkg = uniquePackage(path, scan.modClasses.isEmpty() ? stem(path) : scan.modClasses.get(0).substring(4));
        SIDES.put(path, side);
        JAR_CLASSES.put(path, scan.classes);
        index(path, scan.classes);
        for (String modClass : scan.modClasses) {
            LegacyModDecl decl = new LegacyModDecl();
            decl.legacyId = modClass.substring(4);
            decl.name = displayName(path, decl.legacyId);
            decl.version = "0.0.0-legacy";
            decl.mainClass = MOD_ROOT + pkg + "/" + modClass;
            decl.description = "ModLoader mod for Minecraft Beta 1.7.3 (" + modClass + ")";
            jar.mods.add(decl);
        }
        return jar;
    }

    /**
     * The name shown in mod lists: the archive's own name when it carries the mod's name (NetherCraftv1.9b.zip for mod_Nether: version and
     * "b173" suffixes cut), otherwise the ModLoader class name without "mod_". A GUID-named file tells nothing, so it falls back to the class name.
     */
    static String displayName(Path path, String legacyId) {
        String s = stem(path);
        if (s == null || s.matches("[0-9a-fA-F-]{32,40}")) return legacyId;
        String version = "(?i)[ _.-]*v?\\d+(\\.\\d+)*[a-z]?$";
        String cleaned = s;
        for (int i = 0; i < 3; i++) {
            cleaned = cleaned.replaceAll("(?i)[ _.-]*(b1[._]?7[._]?3|beta)$", "").replaceAll(version, "");
        }
        cleaned = cleaned.replace('_', ' ').trim();
        String norm = cleaned.toLowerCase(Locale.ROOT).replace(" ", "");
        return !cleaned.isEmpty() && norm.contains(legacyId.toLowerCase(Locale.ROOT)) ? cleaned : legacyId;
    }

    private static String stem(Path path) {
        String f = path.getFileName().toString();
        int dot = f.lastIndexOf('.');
        return dot > 0 ? f.substring(0, dot) : f;
    }

    private static synchronized String uniquePackage(Path path, String base) {
        String existing = PACKAGES.get(path);
        if (existing != null) return existing;
        StringBuilder sb = new StringBuilder();
        for (char c : base.toLowerCase(Locale.ROOT).toCharArray()) sb.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ? c : '_');
        if (sb.isEmpty() || !Character.isLetter(sb.charAt(0))) sb.insert(0, 'm');
        Set<String> used = new HashSet<>(PACKAGES.values());
        String pkg = sb.toString();
        for (int i = 2; used.contains(pkg); i++) pkg = sb + "_" + i;
        PACKAGES.put(path, pkg);
        return pkg;
    }

    private static void read(byte[] bytes, Scan scan) {
        ClassReader reader = new ClassReader(bytes);
        String name = reader.getClassName();
        scan.classes.put(name, new ClassMeta(reader.getSuperName(), reader.getInterfaces()));
        if (name.indexOf('/') < 0 && name.startsWith("mod_") && name.indexOf('$') < 0) scan.modClasses.add(name);
        if (reader.getSuperName() != null && apiName(reader.getSuperName()) != null) scan.apiRefs++;
        for (String i : reader.getInterfaces()) if (apiName(i) != null) scan.apiRefs++;
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String n, String d, String s, String[] ex) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitFieldInsn(int op, String owner, String fn, String fd) {
                        if (looksLikeGameClass(owner)) scan.gameRefs.add("F " + owner + " " + fn + " " + fd);
                    }

                    @Override
                    public void visitMethodInsn(int op, String owner, String mn, String md, boolean itf) {
                        if (apiName(owner) != null) scan.apiRefs++;
                        else if (looksLikeGameClass(owner) && !mn.equals("<init>")) scan.gameRefs.add("M " + owner + " " + mn + " " + md);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }
}
