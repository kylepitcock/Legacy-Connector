package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.mapping.NotchNames;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * First step for a Minecraft 1.2.5 jar (opt-in, see {@code docs/wiki/Legacy-1.2.5.md}): from 1.2.5's obfuscation to 1.4.7's, after which the jar is
 * an ordinary 1.4.7 jar for the rest of the pipeline (flat names, forward tables, the 1.7.10 shim).
 *
 * <ul>
 *   <li>classes, fields and methods of the game: through the table {@code mappings/1.2.5/notch-to-147.tsv} that {@code :loader:genMappings125} joins from
 *       Ornithe's Calamus and Feather (client and server separately; see {@link org.kylepitcock.legacyconnector.loader.mapping.Mc125Mappings}). Obfuscated member
 *       names are unique only per class, so a reference whose owner does not declare the member is resolved up the supertypes (the jar's own classes, then the
 *       1.4.7 hierarchy of {@code notch.txt}); a method of a mod class that overrides a game method is renamed with it;</li>
 *   <li>the capitalised ModLoader statics ({@code ModLoader.AddName}) and {@code BaseMod} hooks ({@code Load}, {@code OnTickInGame}) of Beta-era API names to the
 *       lower-case names the 1.4.7 shim has (names only: a hook whose signature changed between versions is not adapted);</li>
 *   <li>copies of game classes in the jar (base class edits of jar mods) are dropped and reported, as for Beta.</li>
 * </ul>
 */
public final class Notch125To147 {
    /** Prefix of a 1.2.5 game member that has no 1.4.7 counterpart: the reference stays unresolved rather than binding to a different member of the same short name. */
    public static final String UNJOINED_PREFIX = "mcc125$";
    public static final String RESOURCE = "legacyconnector/mappings/1.2.5/notch-to-147.tsv";

    /**
     * Capitalised {@code ModLoader} statics that the 1.4.7 shim's ModLoader has under their lower-case-first names. {@code AddAchievementDesc}, {@code AddAllFuel}
     * and {@code OpenGUI} are missing from the shim (open gap, see the 1.2.5 note) and stay as they are.
     */
    public static final Set<String> MODLOADER_STATICS = Set.of("AddName", "AddRecipe", "AddShapelessRecipe", "AddSmelting", "AddSpawn", "RemoveSpawn",
            "RegisterBlock", "RegisterEntityID", "RegisterTileEntity", "RegisterKey", "SetInGameHook", "SetInGUIHook", "AddArmor", "AddLocalization",
            "AddAnimation", "AddOverride", "AddTrade", "AddBiome", "AddCommand", "GetUniqueSpriteIndex", "GetUniqueBlockModelID",
            "GetUniqueEntityId", "GetPrivateValue", "SetPrivateValue", "GetMinecraftInstance", "GetLogger", "IsGUIOpen", "IsModLoaded", "LoadConfig", "SaveConfig",
            "ThrowException", "TakenFromCrafting", "TakenFromFurnace", "OnItemPickup");
    /** ModLoaderMP's classes, in the default package of a 1.2.5 jar (the shim has them in net/minecraft/src, like BaseMod). */
    static final Set<String> MODLOADER_MP = Set.of("ModLoaderMp", "BaseModMp", "Packet230ModLoader");
    /** Capitalised {@code BaseMod} hooks and their 1.4.7 names. */
    public static final Map<String, String> BASEMOD_HOOKS = Map.ofEntries(Map.entry("Load", "load"), Map.entry("ModsLoaded", "modsLoaded"),
            Map.entry("OnTickInGame", "onTickInGame"), Map.entry("OnTickInGUI", "onTickInGUI"), Map.entry("KeyboardEvent", "keyboardEvent"),
            Map.entry("GenerateSurface", "generateSurface"), Map.entry("GenerateNether", "generateNether"), Map.entry("AddRenderer", "addRenderer"),
            Map.entry("RenderWorldBlock", "renderWorldBlock"), Map.entry("RenderInvBlock", "renderInvBlock"), Map.entry("TakenFromCrafting", "takenFromCrafting"),
            Map.entry("TakenFromFurnace", "takenFromFurnace"), Map.entry("AddFuel", "addFuel"), Map.entry("RegisterAnimation", "registerAnimation"),
            Map.entry("Version", "getVersion"));

    /** One side's rows. */
    public static final class Table {
        final Map<String, String> classes = new HashMap<>();
        final Map<String, String> classesBack = new HashMap<>();
        /** {@code F|M owner.name desc} (1.2.5) to {owner147, name147, desc147} */
        final Map<String, String[]> members = new HashMap<>();

        public boolean isEmpty() {
            return classes.isEmpty();
        }

        private Map<String, String> descFix;

        /**
         * Members the join found under another descriptor (1.3 retyped {@code Minecraft.thePlayer} from EntityPlayerSP to EntityClientPlayerMP, ...): {@code name olddesc} (the
         * 1.2.5 descriptor with its classes renamed) to the 1.4.7 member's own descriptor, so the references can be pointed at the field or method that exists.
         */
        Map<String, String> descFix() {
            if (descFix != null) return descFix;
            Map<String, String> fix = new HashMap<>();
            Remapper byClass = new Remapper() {
                @Override
                public String map(String internalName) {
                    return classes.getOrDefault(internalName, internalName);
                }
            };
            for (var e : members.entrySet()) {
                String key = e.getKey(); // "F|M owner.name desc"
                if (key.charAt(0) != 'F' && key.charAt(0) != 'M') continue; // U and B rows keep their own descriptors
                int sp = key.lastIndexOf(' ');
                String old = byClass.mapDesc(key.substring(sp + 1));
                String[] to = e.getValue();
                // only a change of class types (the same member retyped), keyed by the declaring class: short obfuscated names repeat everywhere
                if (!old.equals(to[2]) && old.replaceAll("L[^;]*;", "L;").equals(to[2].replaceAll("L[^;]*;", "L;"))) fix.putIfAbsent(to[0] + "." + to[1] + " " + old, to[2]);
            }
            return descFix = fix;
        }
    }

    private static volatile Table[] resource;

    private Notch125To147() {}

    /** Parses {@code notch-to-147.tsv} rows into the client and the server table. */
    public static Table[] parse(BufferedReader in) throws IOException {
        Table client = new Table(), server = new Table();
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            String[] p = line.split("\t");
            if (p.length < 4) continue;
            Table t = p[1].equals("c") ? client : server;
            if (p[0].equals("C")) {
                t.classes.put(p[2], p[3]);
                t.classesBack.put(p[3], p[2]);
            } else if (p[0].equals("U")) {
                if (p.length >= 6) t.members.put("U " + p[2] + "." + p[3] + " " + p[4], new String[] {p[2], p[5], p[4]});
            } else if (p.length >= 8) {
                t.members.put(p[0] + " " + p[2] + "." + p[3] + " " + p[4], new String[] {p[5], p[6], p[7]});
            }
        }
        return new Table[] {client, server};
    }

    /** The table of this build ({@code -Pmappings125}), or null when the build was made without it. */
    public static synchronized Table[] fromResource() {
        if (resource != null) return resource;
        try (InputStream raw = Notch125To147.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (raw == null) return null;
            return resource = parse(new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("Corrupt " + RESOURCE, e);
        }
    }

    /** True when this build carries the join table (so the opt-in can work). */
    public static boolean available() {
        return fromResource() != null;
    }

    /** Local view of a class of the jar: supertypes. */
    private record Local(String superName, String[] interfaces, Set<String> methods) {}

    /** Renames the classes of {@code jar} in place; returns the report notes. */
    public static List<String> apply(LegacyJar jar, Map<String, byte[]> classes) {
        Table[] t = fromResource();
        if (t == null) throw new IllegalStateException("this build has no 1.2.5 table (build with -Pmappings125)");
        List<String> notes = apply(jar.notchSide == 's' ? t[1] : t[0], NotchNames.get("1.4.7"), classes);
        String dump = System.getProperty("legacyconnector.mc125.unresolved");
        if (dump != null && !dump.isBlank()) {
            // diagnostics (tools/compat/probe.py --mc 1.2.5 -Dlegacyconnector.mc125.unresolved=<file>): one line per game member the join table could not name
            StringBuilder sb = new StringBuilder();
            for (String u : LAST_UNRESOLVED.get()) sb.append(jar.fileName()).append('\t').append(jar.notchSide).append('\t').append(u).append('\n');
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(dump), sb.toString(), StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                // diagnostics only
            }
        }
        return notes;
    }

    /** The unresolved game references of the latest {@link #apply} call on this thread ({@code owner.name desc}). */
    private static final ThreadLocal<Set<String>> LAST_UNRESOLVED = ThreadLocal.withInitial(Set::of);

    public static List<String> apply(Table table, NotchNames n147, Map<String, byte[]> classes) {
        List<String> notes = new ArrayList<>();
        TreeSet<String> baseEdits = new TreeSet<>();
        Map<String, Local> local = new HashMap<>();
        TreeSet<String> bundledApi = new TreeSet<>();
        for (String name : List.copyOf(classes.keySet())) {
            if (org.kylepitcock.legacyconnector.loader.Mc125Jars.MODLOADER_API.contains(name)) {
                // a mod that ships its own copy of ModLoader / ModLoaderMP: the shim provides them (a copy would shadow it and name unrenamed game classes)
                bundledApi.add(name);
                classes.remove(name);
                continue;
            }
            if (table.classes.containsKey(name)) {
                baseEdits.add(name);
                classes.remove(name);
                continue;
            }
            ClassReader r = new ClassReader(classes.get(name));
            Set<String> declared = new HashSet<>();
            r.accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String mname, String mdesc, String sig, String[] ex) {
                    declared.add(mname + mdesc);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            local.put(name, new Local(r.getSuperName(), r.getInterfaces(), declared));
        }
        Resolver resolver = new Resolver(table, n147, local);
        int renamed = 0;
        LAST_BRIDGES.set(new int[2]);
        int[] counts = new int[2];
        int[] unjoinedOverrides = new int[2]; // renamed to mcc125$<Feather name>, left under the obfuscated name
        Map<String, Integer> adaptedCalls = new java.util.TreeMap<>(), unadaptedCalls = new java.util.TreeMap<>();
        List<String> unbridged = new ArrayList<>();
        Remapper byClass = new Remapper() {
            @Override
            public String map(String internalName) {
                return table.classes.getOrDefault(internalName, internalName);
            }
        };
        for (String name : List.copyOf(classes.keySet())) {
            byte[] bytes = classes.get(name);
            // overrides of game methods whose signature differs in 1.4.7 (the host calls the 1.4.7 shape): a bridge with that shape calls the mod's method
            List<Bridge> bridges = new ArrayList<>();
            Local self = local.get(name);
            new ClassReader(bytes).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String mname, String mdesc, String sig, String[] ex) {
                    if (mname.startsWith("<") || mname.length() > 3 || self == null || self.superName() == null) return null;
                    boolean noBridge = (access & (org.objectweb.asm.Opcodes.ACC_STATIC | org.objectweb.asm.Opcodes.ACC_PRIVATE | org.objectweb.asm.Opcodes.ACC_ABSTRACT)) != 0;
                    String[] hit = resolver.lookup("M", self.superName(), mname, mdesc);
                    if (hit == null) hit = resolver.lookup("B", self.superName(), mname, mdesc);
                    if (hit == null) {
                        String[] u = resolver.lookup("U", self.superName(), mname, mdesc);
                        if (u != null && (access & (org.objectweb.asm.Opcodes.ACC_STATIC | org.objectweb.asm.Opcodes.ACC_PRIVATE)) == 0) {
                            unbridged.add(name + "." + mname + mdesc + ": no 1.4.7 method '" + u[1] + "'");
                            // the declaration must carry the side independent name too, or the 1.4.7 stage renames the short letter a second time (to whatever 1.4.7 member has it)
                            if (resolver.mapMethodName(name, mname, mdesc).startsWith(UNJOINED_PREFIX)) unjoinedOverrides[0]++;
                            else unjoinedOverrides[1]++;
                        }
                        return null;
                    }
                    if (noBridge) return null;
                    String oldDesc = byClass.mapDesc(mdesc);
                    if (oldDesc.equals(hit[2])) return null;
                    counts[0]++;
                    String[] reason = new String[1];
                    OverrideBridges.Plan plan = OverrideBridges.plan(oldDesc, hit[2], reason);
                    if (plan == null) {
                        unbridged.add(name + "." + hit[1] + oldDesc + " -> " + hit[2] + ": " + reason[0]);
                    } else {
                        counts[1]++;
                        bridges.add(new Bridge(hit[1], oldDesc, hit[2], plan));
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            LAST_BRIDGES.get()[0] += counts[0];
            counts[0] = 0;
            ClassWriter cw = new ClassWriter(0);
            List<java.util.function.Consumer<org.objectweb.asm.ClassVisitor>> adapters = new ArrayList<>();
            org.objectweb.asm.ClassVisitor tail = new BridgeAdder(cw, bridges, adapters);
            LAST_BRIDGES.get()[1] += counts[1];
            counts[1] = 0;
            ClassRemapper remap = new ClassRemapper(new DescFix(tail, table.descFix(), n147, local), resolver);
            new ClassReader(bytes).accept(new CallAdapt(remap, name, resolver, local, byClass, adapters, adaptedCalls, unadaptedCalls), 0);
            classes.put(name, cw.toByteArray());
            renamed++;
        }
        if (!bundledApi.isEmpty()) {
            notes.add("Dropped " + bundledApi.size() + " bundled ModLoader/ModLoaderMP class(es) (" + String.join(", ", bundledApi) + "): the layer provides that API");
        }
        if (!baseEdits.isEmpty()) {
            notes.add("Dropped " + baseEdits.size() + " modified Minecraft class(es) (base class edits of a jar mod; the layer cannot apply them): "
                    + String.join(", ", baseEdits.stream().limit(8).toList()) + (baseEdits.size() > 8 ? ", ..." : ""));
        }
        LAST_UNRESOLVED.set(new TreeSet<>(resolver.unresolved));
        LAST_UNBRIDGED.set(unbridged);
        LAST_UNJOINED.set(unjoinedOverrides);
        LAST_CALLS.set(new Map[] {adaptedCalls, unadaptedCalls});
        notes.add(LAST_BRIDGES.get()[0] + " override(s) of game methods whose signature changed in 1.3, " + LAST_BRIDGES.get()[1] + " bridged to the 1.4.7 signature"
                + (unbridged.isEmpty() ? "" : " (not bridged, e.g. " + unbridged.stream().limit(3).toList() + ")"));
        notes.add("Minecraft 1.2.5 names rewritten to 1.4.7's in " + renamed + " class(es) (" + resolver.unresolved.size() + " unresolved game reference(s)"
                + (resolver.unresolved.isEmpty() ? "" : ", e.g. " + resolver.unresolved.stream().limit(5).toList()) + ")");
        return notes;
    }

    /** Points references at the descriptor a joined member has in 1.4.7 when it differs from the 1.2.5 one in class types only (after the class renaming). */
    private static final class DescFix extends org.objectweb.asm.ClassVisitor {
        final Map<String, String> fix;

        final NotchNames n147;
        final Map<String, Local> local;

        DescFix(org.objectweb.asm.ClassVisitor next, Map<String, String> fix, NotchNames n147, Map<String, Local> local) {
            super(org.objectweb.asm.Opcodes.ASM9, next);
            this.fix = fix;
            this.n147 = n147;
            this.local = local;
        }

        /** The descriptor of {@code owner.name} after the fix: looked up on the owner and its superclasses (a reference names the receiver's class, not the declaring one). */
        String fixed(String owner, String name, String desc) {
            String c = owner;
            for (int i = 0; i < 24 && c != null; i++) {
                String f = fix.get(c + "." + name + " " + desc);
                if (f != null) return f;
                Local l = local.get(c);
                if (l != null) {
                    c = l.superName();
                } else {
                    String[] up = n147.supertypes(c);
                    c = up == null || up.length == 0 ? null : up[0];
                }
            }
            return desc;
        }

        @Override
        public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
            org.objectweb.asm.MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
            if (mv == null) return null;
            return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9, mv) {
                @Override
                public void visitFieldInsn(int op, String owner, String n, String d) {
                    super.visitFieldInsn(op, owner, n, fixed(owner, n, d));
                }

                @Override
                public void visitMethodInsn(int op, String owner, String n, String d, boolean itf) {
                    super.visitMethodInsn(op, owner, n, fixed(owner, n, d), itf);
                }
            };
        }
    }

    /** Name resolution of one jar. */
    private static final class Resolver extends Remapper {
        final Table table;
        final NotchNames n147;
        final Map<String, Local> local;
        final Set<String> unresolved = new TreeSet<>();

        Resolver(Table table, NotchNames n147, Map<String, Local> local) {
            this.table = table;
            this.n147 = n147;
            this.local = local;
        }

        @Override
        public String map(String internalName) {
            String m = table.classes.get(internalName);
            if (m != null) return m;
            // ModLoaderMP's and Forge 3.x's own classes (not obfuscated: default package and {@code forge/}) live in net/minecraft/src of the shim
            if (!local.containsKey(internalName)) {
                if (MODLOADER_MP.contains(internalName)) return "net/minecraft/src/" + internalName;
                if (internalName.startsWith("forge/")) return "net/minecraft/src/" + internalName;
            }
            return internalName;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            if (name.startsWith("<")) return name;
            if (MODLOADER_STATICS.contains(name) && isModLoader(owner)) return Character.toLowerCase(name.charAt(0)) + name.substring(1);
            String hook = BASEMOD_HOOKS.get(name);
            // a class that already declares the target (the real 1.2.5 spelling is lower case, and a mod may carry a helper called Version() next to getVersion()) keeps its names
            if (hook != null && extendsBaseMod(owner) && !declaresTarget(owner, hook, descriptor)) return hook;
            return resolve("M", owner, name, descriptor);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            return resolve("F", owner, name, descriptor);
        }

        private boolean isServer() {
            Table[] t = fromResource();
            return t != null && table == t[1];
        }

        private static boolean isModLoader(String owner) {
            return owner.equals("ModLoader") || owner.equals("net/minecraft/src/ModLoader");
        }

        private boolean declaresTarget(String owner, String hook, String descriptor) {
            Local l = local.get(owner);
            return l != null && l.methods().contains(hook + descriptor);
        }

        private boolean extendsBaseMod(String owner) {
            String c = owner;
            for (int i = 0; i < 24 && c != null; i++) {
                if (c.equals("BaseMod") || c.equals("net/minecraft/src/BaseMod")) return true;
                Local l = local.get(c);
                c = l == null ? null : l.superName();
            }
            return false;
        }

        /** Name of the 1.4.7 member for a 1.2.5 reference, or the name unchanged when it is not a game member. */
        private String resolve(String kind, String owner, String name, String desc) {
            if (name.length() > 3 && !table.classes.containsKey(owner)) return name; // readable name of a mod member: nothing to rename
            String[] hit = lookup(kind, owner, name, desc);
            if (hit == null && kind.equals("M")) hit = lookup("B", owner, name, desc); // same name, other signature in 1.4.7: an override gets a bridge (OverrideBridges)
            if (hit != null) return hit[1];
            if (name.length() <= 3 && !table.classes.containsKey(owner) && local.containsKey(owner)) {
                String[] named = lookup("U", owner, name, desc);
                if (named != null) return UNJOINED_PREFIX + named[1];
            }
            if (name.length() <= 3 && table.classes.containsKey(owner)) {
                unresolved.add(owner + "." + name + desc);
                // named by the readable name when the table has one (U rows): the client and the server of 1.2.5 use different letters for the same member
                String[] named = lookup("U", owner, name, desc);
                if (named != null) return UNJOINED_PREFIX + named[1];
                // a 1.2.5 member 1.4.7 does not have: its short name could be ANOTHER member of 1.4.7 (the obfuscation is re-randomized every release), so it must not stay as it is;
                // the marker never exists, which the link report shows and the synthesizer stubs, instead of calling the wrong method
                return UNJOINED_PREFIX + name;
            }
            return name;
        }

        /** The table row ({owner147, name147, desc147}) of {@code kind owner.name desc} on the class or its supertypes (jar classes first, then 1.4.7's hierarchy), or null. */
        String[] lookup(String kind, String owner, String name, String desc) {
            Set<String> seen = new HashSet<>();
            List<String> queue = new ArrayList<>(List.of(owner));
            for (int i = 0; i < queue.size() && i < 64; i++) {
                String cls = queue.get(i);
                if (!seen.add(cls)) continue;
                String[] hit = table.members.get(kind + " " + cls + "." + name + " " + desc);
                if (hit != null) return hit;
                Local l = local.get(cls);
                if (l != null) {
                    if (l.superName() != null) queue.add(l.superName());
                    queue.addAll(List.of(l.interfaces()));
                } else if (cls.equals("forge/ISidedInventory")) {
                    // Forge 3.x's interface extends the game's IInventory (client obfuscation io, server ni; read from the Forge 1.2.5 3.3.7 zips)
                    queue.add(isServer() ? "ni" : "io");
                } else if (table.classes.containsKey(cls)) {
                    String[] up = n147.supertypes(table.classes.get(cls));
                    if (up != null) for (String s : up) if (s != null) queue.add(table.classesBack.getOrDefault(s, s));
                }
            }
            return null;
        }
    }

    /** A bridge method to add to a mod class: {@code name} with the 1.4.7 signature {@code newDesc}, calling the class's own {@code name oldDesc}. */
    private record Bridge(String name, String oldDesc, String newDesc, OverrideBridges.Plan plan) {}

    /** Adds the bridge methods of a class when it has been written. */
    private static final class BridgeAdder extends org.objectweb.asm.ClassVisitor {
        final List<Bridge> bridges;
        final List<java.util.function.Consumer<org.objectweb.asm.ClassVisitor>> adapters;
        String owner;

        BridgeAdder(org.objectweb.asm.ClassVisitor next, List<Bridge> bridges, List<java.util.function.Consumer<org.objectweb.asm.ClassVisitor>> adapters) {
            super(org.objectweb.asm.Opcodes.ASM9, next);
            this.bridges = bridges;
            this.adapters = adapters;
        }

        @Override
        public void visit(int version, int access, String name, String sig, String superName, String[] interfaces) {
            owner = name;
            super.visit(version, access, name, sig, superName, interfaces);
        }

        @Override
        public void visitEnd() {
            for (Bridge b : bridges) OverrideBridges.emit(cv, owner, b.name(), b.oldDesc(), b.newDesc(), b.plan());
            for (var r : adapters) r.accept(cv);
            super.visitEnd();
        }
    }

    /** Calls of game methods whose arity changed: adapted (to the 1.4.7 method) and not adaptable, as {@code owner.name desc} to count, of the latest {@link #apply} on this thread. */
    @SuppressWarnings("unchecked")
    private static final ThreadLocal<Map<String, Integer>[]> LAST_CALLS = ThreadLocal.withInitial(() -> new Map[] {Map.of(), Map.of()});

    public static Map<String, Integer> lastAdaptedCalls() {
        return LAST_CALLS.get()[0];
    }

    public static Map<String, Integer> lastUnadaptedCalls() {
        return LAST_CALLS.get()[1];
    }

    /**
     * Before the renaming: a call of a game method that has only a B row (same name, another signature in 1.4.7) cannot keep its arguments, so it goes through an adapter generated into the
     * calling class (the argument lists are reconciled like the override bridges do, in reverse). Calls of methods the mod class declares itself, and calls the join names exactly, stay.
     */
    private static final class CallAdapt extends org.objectweb.asm.ClassVisitor {
        final String self;
        final Resolver resolver;
        final Map<String, Local> local;
        final Remapper byClass;
        final List<java.util.function.Consumer<org.objectweb.asm.ClassVisitor>> adapters;
        final Map<String, Integer> adapted, unadapted;
        final Map<String, String> made = new HashMap<>();
        String superName;

        CallAdapt(org.objectweb.asm.ClassVisitor next, String self, Resolver resolver, Map<String, Local> local, Remapper byClass, List<java.util.function.Consumer<org.objectweb.asm.ClassVisitor>> adapters, Map<String, Integer> adapted, Map<String, Integer> unadapted) {
            super(org.objectweb.asm.Opcodes.ASM9, next);
            this.self = self;
            this.resolver = resolver;
            this.local = local;
            this.byClass = byClass;
            this.adapters = adapters;
            this.adapted = adapted;
            this.unadapted = unadapted;
        }

        private boolean declaredLocally(String owner, String name, String desc) {
            String c = owner;
            for (int i = 0; i < 24 && c != null; i++) {
                Local l = local.get(c);
                if (l == null) return false;
                if (l.methods().contains(name + desc)) return true;
                c = l.superName();
            }
            return false;
        }

        @Override
        public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
            org.objectweb.asm.MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
            if (mv == null) return null;
            return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9, mv) {
                @Override
                public void visitMethodInsn(int op, String owner, String n, String d, boolean itf) {
                    if (n.length() > 3 || n.startsWith("<") || owner.startsWith("[") || declaredLocally(owner, n, d) || resolver.lookup("M", owner, n, d) != null) {
                        super.visitMethodInsn(op, owner, n, d, itf);
                        return;
                    }
                    String[] b = resolver.lookup("B", owner, n, d);
                    if (b == null) {
                        super.visitMethodInsn(op, owner, n, d, itf);
                        return;
                    }
                    String key = owner + "." + n + d;
                    String oldMapped = byClass.mapDesc(d);
                    String[] reason = new String[1];
                    OverrideBridges.Plan plan = OverrideBridges.planCall(oldMapped, b[2], reason);
                    if (plan == null) {
                        unadapted.merge(key + ": " + reason[0], 1, Integer::sum);
                        super.visitMethodInsn(op, owner, n, d, itf);
                        return;
                    }
                    adapted.merge(key, 1, Integer::sum);
                    boolean special = op == org.objectweb.asm.Opcodes.INVOKESPECIAL;
                    boolean isStatic = op == org.objectweb.asm.Opcodes.INVOKESTATIC;
                    String id = op + ":" + key;
                    String aname = made.get(id);
                    if (aname == null) {
                        aname = "mcc125$call$" + made.size();
                        made.put(id, aname);
                        final String an = aname;
                        final String ownerMapped = byClass.map(owner);
                        adapters.add(out -> OverrideBridges.emitCall(out, an, special || isStatic ? null : org.objectweb.asm.Type.getObjectType(ownerMapped), special, oldMapped,
                                op == org.objectweb.asm.Opcodes.INVOKEINTERFACE ? op : op, ownerMapped, b[1], b[2], itf, plan));
                    }
                    // the receiver (this, for a super call) and the arguments are on the stack: a static adapter takes the receiver first, the instance adapter of a super call is called on this
                    if (special) super.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, self, aname, d, false);
                    else if (isStatic) super.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, self, aname, d, false);
                    else super.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, self, aname, "(L" + owner + ";" + d.substring(1), false);
                }
            };
        }
    }

    /** {changed-signature overrides, bridged} of the latest {@link #apply} on this thread, and why the others were not bridged. */
    private static final ThreadLocal<int[]> LAST_BRIDGES = ThreadLocal.withInitial(() -> new int[2]);
    private static final ThreadLocal<int[]> LAST_UNJOINED = ThreadLocal.withInitial(() -> new int[2]);

    /** {renamed, left} overrides of unjoined game members of the latest {@link #apply} on this thread. */
    public static int[] lastUnjoinedOverrides() {
        return LAST_UNJOINED.get().clone();
    }

    private static final ThreadLocal<List<String>> LAST_UNBRIDGED = ThreadLocal.withInitial(List::of);

    public static int[] lastBridgeCounts() {
        return LAST_BRIDGES.get().clone();
    }

    public static List<String> lastUnbridged() {
        return LAST_UNBRIDGED.get();
    }
}
