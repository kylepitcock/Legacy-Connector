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
        return apply(jar.notchSide == 's' ? t[1] : t[0], NotchNames.get("1.4.7"), classes);
    }

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
        for (String name : List.copyOf(classes.keySet())) {
            byte[] bytes = classes.get(name);
            ClassWriter cw = new ClassWriter(0);
            new ClassReader(bytes).accept(new ClassRemapper(cw, resolver), 0);
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
        notes.add("Minecraft 1.2.5 names rewritten to 1.4.7's in " + renamed + " class(es) (" + resolver.unresolved.size() + " unresolved game reference(s)"
                + (resolver.unresolved.isEmpty() ? "" : ", e.g. " + resolver.unresolved.stream().limit(5).toList()) + ")");
        return notes;
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
            return m != null ? m : internalName;
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
            Set<String> seen = new HashSet<>();
            List<String> queue = new ArrayList<>(List.of(owner));
            for (int i = 0; i < queue.size() && i < 64; i++) {
                String cls = queue.get(i);
                if (!seen.add(cls)) continue;
                String[] hit = table.members.get(kind + " " + cls + "." + name + " " + desc);
                if (hit != null) return hit[1];
                Local l = local.get(cls);
                if (l != null) {
                    if (l.superName() != null) queue.add(l.superName());
                    queue.addAll(List.of(l.interfaces()));
                } else if (table.classes.containsKey(cls)) {
                    String[] up = n147.supertypes(table.classes.get(cls));
                    if (up != null) for (String s : up) if (s != null) queue.add(table.classesBack.getOrDefault(s, s));
                }
            }
            if (name.length() <= 3 && table.classes.containsKey(owner)) unresolved.add(owner + "." + name + desc);
            return name;
        }
    }
}
