package org.kylepitcock.legacyconnector.loader.tools;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Milestone 0 of 1.20.1 support: the gap report. Takes Forge 1.20.1 mod jars (SRG-named), renames what the layer would rename
 * (SRG ids to Mojang names, Forge packages to NeoForge) and checks every class, method and field reference against the real
 * Minecraft 1.21.1 + NeoForge jars, the JDK, the other jars of the set and the libraries on the loader's class path. What is left is the
 * work list: symbols that exist in 1.20.1 and not in 1.21.1 (moved, renamed, removed or changed in signature).
 *
 * <p>usage: Gap1201 &lt;srg-table.tsv&gt; &lt;rename-tsv,rename-tsv&gt; &lt;target.jar[,target.jar]&gt; &lt;report.txt&gt; &lt;mod.jar&gt;...
 */
public final class Gap1201 {
    /** Structure of one class, from a target jar or a mod. */
    private static final class Cls {
        String name;
        String superName;
        String[] interfaces;
        final Set<String> methods = new HashSet<>();
        final Set<String> fields = new HashSet<>();
    }

    private final Map<String, Cls> target = new HashMap<>();
    private final Map<String, String> srg = new HashMap<>();
    private final Map<String, String> classRenames = new HashMap<>();
    private final Set<String> callRewrites = new HashSet<>();

    public static void main(String[] args) throws Exception {
        Gap1201 gap = new Gap1201();
        gap.loadSrg(Path.of(args[0]));
        for (String f : args[1].split(",")) gap.loadRenames(Path.of(f));
        // "@file": a list file (comma or line separated), for target lists longer than the Windows command line
        // (build.gradle appends the shim class directories after a comma: "@file,dir,dir")
        String targets = args[2];
        if (targets.startsWith("@")) {
            int comma = targets.indexOf(',');
            String rest = comma < 0 ? "" : "," + targets.substring(comma + 1);
            targets = Files.readString(Path.of(targets.substring(1, comma < 0 ? targets.length() : comma))).replace("\r", "").replace('\n', ',') + rest;
        }
        nestedMode = 1;
        for (String j : targets.split(",")) if (!j.isBlank()) gap.index(Path.of(j.trim()), gap.target);
        // jar-in-jar libraries last: a mod carrying an old copy of a library must not shadow the library jar the pack installs
        nestedMode = 2;
        for (String j : targets.split(",")) if (!j.isBlank()) gap.index(Path.of(j.trim()), gap.target);
        nestedMode = 0;
        List<Path> mods = new ArrayList<>();
        for (int i = 4; i < args.length; i++) {
            if (args[i].startsWith("@")) { // "@file": one jar path per line (a pack of hundreds of jars does not fit the Windows command line)
                for (String l : Files.readAllLines(Path.of(args[i].substring(1)))) if (!l.isBlank()) mods.add(Path.of(l.trim()));
            } else mods.add(Path.of(args[i]));
        }
        gap.run(mods, Path.of(args[3]));
    }

    private void loadSrg(Path p) throws IOException {
        try (Stream<String> lines = Files.lines(p)) {
            lines.forEach(l -> {
                int t = l.indexOf('\t');
                if (t > 0) srg.put(l.substring(0, t), l.substring(t + 1));
            });
        }
    }

    private void loadRenames(Path p) throws IOException {
        try (Stream<String> lines = Files.lines(p)) {
            lines.filter(l -> !l.startsWith("#")).forEach(l -> {
                String[] a = l.strip().split("\t");
                if (a.length != 2) return;
                // call rewrites (owner.name(desc): helper) are redirected by the translator: not a gap
                if (a[0].indexOf('(') > 0) callRewrites.add(a[0]);
                else classRenames.put(a[0], a[1]);
            });
        }
    }

    /** The target jars are indexed twice: first without, then only their jar-in-jar libraries. */
    private static int nestedMode;   // 0 everything (mods), 1 without jar-in-jar, 2 only jar-in-jar

    private static void index(Path jar, Map<String, Cls> into) throws IOException {
        if (Files.isDirectory(jar)) {
            if (nestedMode == 2) return;
            // compiled facade classes (legacyconnector/v1_20_1/...): the Forge API the layer provides itself
            try (Stream<Path> files = Files.walk(jar)) {
                for (Path f : files.filter(x -> x.toString().endsWith(".class")).toList()) {
                    String rel = jar.relativize(f).toString().replace((char) 92, '/');
                    // the facades, and the migrate helpers the translated jars call (org/kylepitcock/legacyconnector/v1_20_1/migrate: they exist at run time)
                    if (!rel.startsWith("legacyconnector/v1_20_1/") && !rel.startsWith("org/kylepitcock/legacyconnector/v1_20_1/")) continue;
                    ClassNode n = new ClassNode();
                    new ClassReader(Files.readAllBytes(f)).accept(n, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    Cls c = new Cls();
                    c.name = n.name;
                    c.superName = n.superName;
                    c.interfaces = n.interfaces.toArray(new String[0]);
                    n.methods.forEach(m -> c.methods.add(m.name + m.desc));
                    n.fields.forEach(fl -> c.fields.add(fl.name + ":" + fl.desc));
                    into.putIfAbsent(n.name, c);
                }
            }
            return;
        }
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (var en = zip.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                // jar-in-jar libraries (Create 1.21.1 carries Registrate, Flywheel, Ponder; mods carry Mixin extras...) are part of the mod at run time
                boolean nestedJar = e.getName().startsWith("META-INF/jarjar/") && e.getName().endsWith(".jar");
                if (nestedMode == 1 && nestedJar) continue;
                if (nestedMode == 2 && !nestedJar) continue;
                if (nestedJar) {
                    byte[] nestedBytes = zip.getInputStream(e).readAllBytes();
                    // a library with a native 1.21.1 build that a mod carries as jar-in-jar (Ars Nouveau's GeckoLib 4.4.4): the game takes the native, newer one
                    if (!Boolean.getBoolean("gap.keepNativeLibs") && nestedIsNativeLibrary(nestedBytes)) continue;
                    try (java.util.zip.ZipInputStream nested = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(nestedBytes))) {
                        for (ZipEntry ne; (ne = nested.getNextEntry()) != null; ) {
                            if (!ne.getName().endsWith(".class") || ne.getName().startsWith("META-INF/versions/")) continue;
                            try {
                                ClassNode n = new ClassNode();
                                new ClassReader(nested.readAllBytes()).accept(n, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                                Cls c = new Cls();
                                c.name = n.name;
                                c.superName = n.superName;
                                c.interfaces = n.interfaces.toArray(new String[0]);
                                n.methods.forEach(m -> c.methods.add(m.name + m.desc));
                                n.fields.forEach(f -> c.fields.add(f.name + ":" + f.desc));
                                into.putIfAbsent(n.name, c);
                            } catch (RuntimeException ignored) {
                                // unreadable entry
                            }
                        }
                    } catch (IOException ignored) {
                        // unreadable nested jar
                    }
                    continue;
                }
                if (!e.getName().endsWith(".class") || e.getName().startsWith("META-INF/versions/")) continue;
                try {
                    ClassNode n = new ClassNode();
                    new ClassReader(zip.getInputStream(e).readAllBytes()).accept(n, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    Cls c = new Cls();
                    c.name = n.name;
                    c.superName = n.superName;
                    c.interfaces = n.interfaces.toArray(new String[0]);
                    n.methods.forEach(m -> c.methods.add(m.name + m.desc));
                    n.fields.forEach(f -> c.fields.add(f.name + ":" + f.desc));
                    into.putIfAbsent(n.name, c);
                } catch (RuntimeException | IOException ignored) {
                    // unreadable entry
                }
            }
        }
    }

    private Cls jdk(String name) {
        if (!(name.startsWith("java/") || name.startsWith("javax/") || name.startsWith("jdk/") || name.startsWith("sun/"))) return null;
        try {
            Class<?> k = Class.forName(name.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
            Cls c = new Cls();
            c.name = name;
            c.superName = k.getSuperclass() == null ? null : k.getSuperclass().getName().replace('.', '/');
            c.interfaces = java.util.Arrays.stream(k.getInterfaces()).map(i -> i.getName().replace('.', '/')).toArray(String[]::new);
            for (var m : k.getDeclaredMethods()) c.methods.add(m.getName() + Type.getMethodDescriptor(m));
            for (var m : k.getDeclaredConstructors()) c.methods.add("<init>" + Type.getConstructorDescriptor(m));
            for (var f : k.getDeclaredFields()) c.fields.add(f.getName() + ":" + Type.getDescriptor(f.getType()));
            return c;
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    // ---- renaming -------------------------------------------------------------------------------------------------------------

    private String cls(String name) {
        if (name == null) return null;
        if (name.startsWith("[")) return desc(name);
        if (name.startsWith("net/minecraftforge/") || name.startsWith("cpw/")) {
            String facade = "legacyconnector/v1_20_1/" + name;
            if (target.containsKey(facade)) return facade;
        }
        String r = classRenames.get(name);
        if (r != null) return r;
        int dollar = name.indexOf('$');
        if (dollar > 0) {
            String outer = classRenames.get(name.substring(0, dollar));
            if (outer != null) return outer + name.substring(dollar);
        }
        // the Forge to NeoForge package move for what the tables do not list
        for (String[] move : new String[][] {{"net/minecraftforge/fml/", "net/neoforged/fml/"}, {"net/minecraftforge/eventbus/", "net/neoforged/bus/"},
                {"net/minecraftforge/api/", "net/neoforged/api/"}, {"net/minecraftforge/forgespi/", "net/neoforged/neoforgespi/"},
                {"net/minecraftforge/", "net/neoforged/neoforge/"}}) {
            if (!name.startsWith(move[0])) continue;
            String moved = move[1] + name.substring(move[0].length());
            if (target.containsKey(moved)) return moved;
            break;
        }
        return name;
    }

    private String desc(String d) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            char ch = d.charAt(i);
            if (ch == 'L') {
                int end = d.indexOf(';', i);
                sb.append('L').append(cls(d.substring(i + 1, end))).append(';');
                i = end;
            } else sb.append(ch);
        }
        return sb.toString();
    }

    private String member(String name) {
        String m = srg.get(name);
        return m == null ? name : m;
    }

    // ---- checking -------------------------------------------------------------------------------------------------------------

    private Cls find(String name, Map<String, Cls> own) {
        Cls c = own.get(name);
        if (c == null) c = target.get(name);
        return c != null ? c : jdk(name);
    }

    private boolean hasMember(String owner, String key, boolean method, Map<String, Cls> own, Set<String> seen) {
        if (owner == null || !seen.add(owner)) return false;
        Cls c = find(owner, own);
        if (c == null) return false;
        if ((method ? c.methods : c.fields).contains(key)) return true;
        if (hasMember(c.superName, key, method, own, seen)) return true;
        if (c.interfaces != null) for (String i : c.interfaces) if (hasMember(i, key, method, own, seen)) return true;
        return method && owner.equals(c.name) && c.superName == null && hasMember("java/lang/Object", key, true, own, seen);
    }

    private long totalForge;

    /** A symbol of the Forge/NeoForge API (not of Minecraft or a third-party mod). */
    private static boolean forgeSymbol(String s) {
        return s.startsWith("net/minecraftforge/") || s.startsWith("net/neoforged/") || s.startsWith("cpw/") || s.startsWith("legacyconnector/v1_20_1/")
                || s.contains("Lnet/minecraftforge/") || s.contains("Lcpw/");
    }

    /** Offline report of a translated run: the symbols the soft link replaced by stand-ins, per original jar (still unresolved references of the mod: kind "soft" in the detail file). */
    private static final Map<String, Set<String>> SOFT = new TreeMap<>();

    private final Map<String, Set<String>> missingClasses = new TreeMap<>();
    private final Map<String, Set<String>> missingMethods = new TreeMap<>();
    private final Map<String, Set<String>> missingFields = new TreeMap<>();

    private static final java.util.regex.Pattern MOD_ID = java.util.regex.Pattern.compile("modId\\s*=\\s*\"([^\"]+)\"");

    private static boolean nestedIsNativeLibrary(byte[] jar) {
        try (java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(jar))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                if (!e.getName().equals("META-INF/mods.toml") && !e.getName().equals("META-INF/neoforge.mods.toml")) continue;
                java.util.regex.Matcher m = MOD_ID.matcher(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                boolean any = false;
                while (m.find()) {
                    String id = m.group(1);
                    if (id.equals("forge") || id.equals("minecraft") || id.equals("neoforge")) continue;
                    any = true;
                    if (!org.kylepitcock.legacyconnector.loader.NativeLibraries1201.table().containsKey(id.toLowerCase(java.util.Locale.ROOT))) return false;
                }
                return any;
            }
        } catch (IOException | RuntimeException ignored) {
            // unreadable: a normal library
        }
        return false;
    }

    private static boolean isNativeLibrary(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (String name : new String[] {"META-INF/mods.toml", "META-INF/neoforge.mods.toml"}) {
                ZipEntry e = zip.getEntry(name);
                if (e == null) continue;
                String toml = new String(zip.getInputStream(e).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                java.util.regex.Matcher m = MOD_ID.matcher(toml);
                boolean any = false;
                while (m.find()) {
                    String id = m.group(1);
                    if (id.equals("forge") || id.equals("minecraft") || id.equals("neoforge")) continue;
                    any = true;
                    if (!org.kylepitcock.legacyconnector.loader.NativeLibraries1201.table().containsKey(id.toLowerCase(java.util.Locale.ROOT))) return false;
                }
                return any;
            }
        } catch (IOException | RuntimeException e) {
            // unreadable: treated as a normal mod
        }
        return false;
    }

    private void run(List<Path> mods, Path report) throws IOException {
        if (Boolean.getBoolean("gap.translate")) mods = translated(mods);
        Map<String, Cls> own = new HashMap<>();
        for (Path m : mods) {
            // a library with a native 1.21.1 build (libraries.tsv) is replaced by that build at run time: its 1.20.1 classes must not shadow the 1.21.1 ones
            if (!Boolean.getBoolean("gap.keepNativeLibs") && isNativeLibrary(m)) continue;
            index(m, own);
        }
        // the mods' own members must be renamed like the references to them (descriptors with moved classes, SRG names of overrides)
        for (Cls c : own.values()) {
            Set<String> ms = new HashSet<>(), fs = new HashSet<>();
            for (String k : c.methods) {
                int at = k.indexOf('(');
                try {
                    ms.add(member(k.substring(0, at)) + desc(k.substring(at)));
                } catch (RuntimeException ex) {
                    ms.add(k);
                }
            }
            for (String k : c.fields) {
                int at = k.indexOf(':');
                fs.add(member(k.substring(0, at)) + ":" + desc(k.substring(at + 1)));
            }
            c.methods.clear();
            c.methods.addAll(ms);
            c.fields.clear();
            c.fields.addAll(fs);
            if (c.superName != null) c.superName = cls(c.superName);
            if (c.interfaces != null) for (int i = 0; i < c.interfaces.length; i++) c.interfaces[i] = cls(c.interfaces[i]);
        }
        PrintStream out = new PrintStream(Files.newOutputStream(report));
        // per jar detail (jar TAB kind TAB symbol), input of tools/compat/gap1201_rank.py
        PrintStream detail = new PrintStream(Files.newOutputStream(report.resolveSibling(report.getFileName() + ".detail.tsv")));
        out.println("Gap report: Forge 1.20.1 jars against Minecraft 1.21.1 + NeoForge (SRG renamed, Forge packages renamed)\n");
        for (Path mod : mods) {
            Set<String> mc = new TreeSet<>(), mm = new TreeSet<>(), mf = new TreeSet<>();
            int refs = 0;
            try (ZipFile zip = new ZipFile(mod.toFile())) {
                for (var en = zip.entries(); en.hasMoreElements(); ) {
                    ZipEntry e = en.nextElement();
                    if (!e.getName().endsWith(".class") || e.getName().startsWith("META-INF/")) continue;
                    ClassNode n = new ClassNode();
                    try {
                        new ClassReader(zip.getInputStream(e).readAllBytes()).accept(n, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    } catch (RuntimeException ex) {
                        continue;
                    }
                    refs += check(n, own, mc, mm, mf);
                }
            }
            String jar = mod.getFileName().toString();
            try (ZipFile z = new ZipFile(mod.toFile())) {
                var manifestEntry = z.getEntry("META-INF/MANIFEST.MF");
                if (manifestEntry != null) {
                    String src = new java.util.jar.Manifest(z.getInputStream(manifestEntry)).getMainAttributes().getValue("LegacyConnector-Source");
                    if (src != null) jar = src; // a translated jar is named after the original
                }
            }
            for (String x : SOFT.getOrDefault(jar, Set.of())) {
                int sp = x.indexOf(' ');
                String sym = x.substring(sp + 1);
                switch (x.substring(0, sp)) {
                    case "class" -> mc.add(sym);
                    case "method" -> mm.add(sym);
                    default -> mf.add(sym);
                }
            }
            long forgeApi = java.util.stream.Stream.of(mc, mm, mf).flatMap(Set::stream).filter(Gap1201::forgeSymbol).count();
            out.printf("%s: %d references, %d missing classes, %d missing methods, %d missing fields, %d unresolved Forge-API symbols%n", jar, refs, mc.size(), mm.size(), mf.size(), forgeApi);
            totalForge += forgeApi;
            final String jarName = jar;
            mc.forEach(x -> detail.println(jarName + "	class	" + x));
            mm.forEach(x -> detail.println(jarName + "	method	" + x));
            mf.forEach(x -> detail.println(jarName + "	field	" + x));
            detail.println(jarName + "	refs	" + refs);
            mc.forEach(s -> missingClasses.computeIfAbsent(s, k -> new TreeSet<>()).add(jarName));
            mm.forEach(s -> missingMethods.computeIfAbsent(s, k -> new TreeSet<>()).add(jarName));
            mf.forEach(s -> missingFields.computeIfAbsent(s, k -> new TreeSet<>()).add(jarName));
        }
        out.printf("%nTOTAL unresolved Forge-API symbols: %d%n", totalForge);
        ranked(out, "Missing classes (by number of jars)", missingClasses);
        ranked(out, "Missing methods", missingMethods);
        ranked(out, "Missing fields", missingFields);
        out.close();
        detail.close();
        System.out.println(mcSummary());
        System.out.println("report: " + report + " (" + missingClasses.size() + " classes, " + missingMethods.size() + " methods, " + missingFields.size() + " fields)");
    }

    /** -Dgap.shimIndex=<file>: the real shim index, so the facade classes get the names they have in the game (descriptors of the library tables match). */
    private static org.kylepitcock.legacyconnector.loader.shim.ShimIndex shimIndex() throws IOException {
        String file = System.getProperty("gap.shimIndex");
        if (file == null || !Files.isRegularFile(Path.of(file))) return org.kylepitcock.legacyconnector.loader.shim.ShimIndex.empty();
        try (java.io.InputStream in = Files.newInputStream(Path.of(file))) {
            return org.kylepitcock.legacyconnector.loader.shim.ShimIndex.read(in);
        }
    }

    /**
     * -Dgap.translate=true: the jars go through the real Forge1201Transformer first (names, Minecraft migration), so the report shows what is left
     * after the layer. The facade classes of the Forge API (legacyconnector/v1_20_1) are not indexed here and count as present.
     */
    private static List<Path> translated(List<Path> mods) throws IOException {
        String keep = System.getProperty("gap.translated.dir");
        Path dir = keep != null ? Files.createDirectories(Path.of(keep)) : Files.createTempDirectory("gap1201-");
        var transformer = new org.kylepitcock.legacyconnector.loader.transform.Forge1201Transformer(shimIndex());
        List<Path> out = new ArrayList<>();
        // the classes of all the jars: a mod class that extends a library class of another jar is judged with that class's supertypes and methods
        var peerSet = new org.kylepitcock.legacyconnector.loader.transform.PeerClasses();
        for (Path m : mods) {
            if (!Boolean.getBoolean("gap.keepNativeLibs") && isNativeLibrary(m)) continue;   // replaced by the native build at run time: nobody extends its 1.20.1 classes
            try {
                var lj = new org.kylepitcock.legacyconnector.loader.LegacyJar(m);
                lj.version = org.kylepitcock.legacyconnector.loader.LegacyVersion.V1_20_1;
                peerSet.add(transformer.scan(lj));
            } catch (RuntimeException | IOException e) {
                System.out.println("scan failed for " + m.getFileName() + ": " + e);
            }
        }
        var peers = peerSet.classes();
        for (Path m : mods) {
            Path t = dir.resolve(m.getFileName());
            try {
                var lj = new org.kylepitcock.legacyconnector.loader.LegacyJar(m);
                lj.version = org.kylepitcock.legacyconnector.loader.LegacyVersion.V1_20_1;
                var res = transformer.transform(lj, t, peers);
                if (res.softLinked > 0) SOFT.computeIfAbsent(m.getFileName().toString(), k -> new TreeSet<>()).addAll(res.softSymbols);
                if (res.softLinked > 0) System.out.println("soft-linked " + res.softLinked + " symbol(s) in " + m.getFileName());
                if (res.softLinked > 0 && Boolean.getBoolean("gap.softlist")) res.notes.stream().filter(n -> n.startsWith("Soft link: ") && n.length() > 40).forEach(n -> System.out.println("  " + m.getFileName() + " " + n));
                out.add(t);
            } catch (RuntimeException | IOException e) {
                System.out.println("translation failed for " + m.getFileName() + ": " + e);
            }
        }
        return out;
    }

    /** Distinct symbols and jar x symbol occurrences that are Minecraft's own (the area of the migration tables; Forge API gaps are not counted). */
    private String mcSummary() {
        StringBuilder sb = new StringBuilder("MC-gap (distinct/occurrences):");
        for (var e : new TreeMap<>(Map.of("classes", missingClasses, "methods", missingMethods, "fields", missingFields)).entrySet()) {
            int d = 0, o = 0;
            for (var en : e.getValue().entrySet()) {
                if (!(en.getKey().startsWith("net/minecraft/") || en.getKey().startsWith("com/mojang/"))) continue;
                d++;
                o += en.getValue().size();
            }
            sb.append(' ').append(e.getKey()).append(' ').append(d).append('/').append(o);
        }
        return sb.toString();
    }

    private static void ranked(PrintStream out, String title, Map<String, Set<String>> map) {
        out.println("\n== " + title + ": " + map.size() + " distinct ==");
        map.entrySet().stream().sorted(Comparator.comparingInt((Map.Entry<String, Set<String>> e) -> -e.getValue().size()).thenComparing(Map.Entry::getKey))
                .limit(Integer.getInteger("gap.limit", 400)).forEach(e -> out.println(String.format("%3d  %s", e.getValue().size(), e.getKey())));
    }

    private int check(ClassNode n, Map<String, Cls> own, Set<String> mc, Set<String> mm, Set<String> mf) {
        int[] refs = {0};
        var type = (java.util.function.Consumer<String>) t -> {
            if (t == null) return;
            refs[0]++;
            String name = t;
            while (name.startsWith("[")) name = name.substring(1);
            if (name.startsWith("L") && name.endsWith(";")) name = name.substring(1, name.length() - 1);
            else if (name.length() == 1) return;
            String r = cls(name);
            if (find(r, own) == null && !isLibrary(r)) mc.add(r);
        };
        type.accept(n.superName);
        if (n.interfaces != null) n.interfaces.forEach(type);
        for (MethodNode m : n.methods) {
            descTypes(m.desc, type);
            if (m.instructions == null) continue;
            for (AbstractInsnNode in : m.instructions) {
                if (in instanceof MethodInsnNode mi) {
                    refs[0]++;
                    String owner = cls(mi.owner), name = member(mi.name), d = desc(mi.desc);
                    descTypes(mi.desc, type);
                    if (owner.startsWith("[")) continue;
                    type.accept(mi.owner);
                    Cls c = find(owner, own);
                    if (c == null) continue; // reported as a missing class
                    // signature-polymorphic JDK methods (MethodHandle.invoke/invokeExact, VarHandle.get/set/...) take any descriptor
                    if ((owner.equals("java/lang/invoke/MethodHandle") || owner.equals("java/lang/invoke/VarHandle")) && polymorphic(owner, name)) continue;
                    if (!hasMember(owner, name + d, true, own, new HashSet<>()) && !callRewrites.contains(owner + "." + name + d) && !callRewrites.contains("*." + name + d)
                            && !(owner.startsWith("net/neoforged/") && callRewrites.contains("@event." + name + d))) mm.add(owner + "." + name + d);
                } else if (in instanceof FieldInsnNode fi) {
                    refs[0]++;
                    String owner = cls(fi.owner), name = member(fi.name), d = desc(fi.desc);
                    type.accept(fi.owner);
                    Cls c = find(owner, own);
                    if (c == null) continue;
                    if (!hasMember(owner, name + ":" + d, false, own, new HashSet<>())) mf.add(owner + "." + name + ":" + d);
                } else if (in instanceof TypeInsnNode ti) {
                    type.accept(ti.desc);
                } else if (in instanceof MultiANewArrayInsnNode ma) {
                    type.accept(ma.desc);
                } else if (in instanceof LdcInsnNode l && l.cst instanceof Type t && t.getSort() == Type.OBJECT) {
                    type.accept(t.getInternalName());
                } else if (in instanceof InvokeDynamicInsnNode) {
                    refs[0]++;
                }
            }
        }
        return refs[0];
    }

    /** The methods javac compiles with the call site's own descriptor (JLS 15.12.3): the JVM links them whatever the descriptor says. */
    private static boolean polymorphic(String owner, String name) {
        if (owner.equals("java/lang/invoke/MethodHandle")) return name.equals("invoke") || name.equals("invokeExact");
        return switch (name) {
            case "get", "set", "getVolatile", "setVolatile", "getOpaque", "setOpaque", "getAcquire", "setRelease", "compareAndSet", "compareAndExchange",
                    "compareAndExchangeAcquire", "compareAndExchangeRelease", "weakCompareAndSetPlain", "weakCompareAndSet", "weakCompareAndSetAcquire",
                    "weakCompareAndSetRelease", "getAndSet", "getAndSetAcquire", "getAndSetRelease", "getAndAdd", "getAndAddAcquire", "getAndAddRelease",
                    "getAndBitwiseOr", "getAndBitwiseOrAcquire", "getAndBitwiseOrRelease", "getAndBitwiseAnd", "getAndBitwiseAndAcquire",
                    "getAndBitwiseAndRelease", "getAndBitwiseXor", "getAndBitwiseXorAcquire", "getAndBitwiseXorRelease" -> true;
            default -> false;
        };
    }

    private static void descTypes(String desc, java.util.function.Consumer<String> type) {
        for (Type t : Type.getArgumentTypes(desc)) addType(t, type);
        addType(Type.getReturnType(desc), type);
    }

    private static void addType(Type t, java.util.function.Consumer<String> type) {
        if (t.getSort() == Type.ARRAY) t = t.getElementType();
        if (t.getSort() == Type.OBJECT) type.accept(t.getInternalName());
    }

    /**
     * -Dgap.assumeNative=prefix,prefix: API packages of libraries that have a native 1.21.1 build the pack would install (EMI, REI, The One Probe...) but whose
     * jar this offline run does not have; references to classes under them are not counted as gaps (off by default: it cannot see signature drift).
     */
    private static final String[] ASSUMED_NATIVE = System.getProperty("gap.assumeNative") == null ? null : System.getProperty("gap.assumeNative").split(",");

    /** Libraries a mod gets from the game at run time that this offline tool does not index (annotations, logging, Guava, Gson...). */
    private static boolean isLibrary(String name) {
        if (ASSUMED_NATIVE != null) for (String prefix : ASSUMED_NATIVE) if (name.startsWith(prefix)) return true;
        return name.startsWith("com/google/") || name.startsWith("org/slf4j/") || name.startsWith("org/apache/") || name.startsWith("io/netty/")
                || name.startsWith("org/lwjgl/") || name.startsWith("org/joml/") || name.startsWith("it/unimi/") || name.startsWith("com/mojang/")
                || name.startsWith("org/jetbrains/") || name.startsWith("org/objectweb/") || name.startsWith("org/spongepowered/") || name.startsWith("javax/")
                || name.startsWith("com/electronwill/") || name.startsWith("org/openjdk/") || name.startsWith("net/neoforged/fml/") || name.startsWith("net/neoforged/bus/")
                || name.startsWith("net/neoforged/neoforgespi/") || name.startsWith("legacyconnector/");
    }
}
