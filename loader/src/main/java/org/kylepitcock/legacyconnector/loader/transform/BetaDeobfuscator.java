package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.BetaJars;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.mapping.BetaMappings;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * First step for a Beta 1.7.3 jar: from the obfuscated names it was compiled against to the names the shim uses, before the
 * jar goes through the same relocation as every other family ({@code net/minecraft/} into {@code legacyconnector/vb1_7_3/}).
 *
 * <ul>
 *   <li>game classes: official client or server name (see {@link BetaJars#side}) to biny's ({@code uu} to
 *       {@code net/minecraft/block/Block});</li>
 *   <li>members: obfuscated names are only unique per class, so a reference is resolved up the hierarchy (the jar's classes, the
 *       other b1.7.3 jars of the set, then the game) to the game class that declares it; a mod method overriding a game method is
 *       renamed with it;</li>
 *   <li>the ModLoader API ({@code BaseMod}, ...) and Forge ({@code forge/}) to {@code net/minecraft/src/}; bundled copies are dropped
 *       (the shim provides them);</li>
 *   <li>mod classes in the default package or {@code net/minecraft/} to {@code legacyconnector_b173/<mod>/} (see {@link BetaJars});</li>
 *   <li>copies of game classes (base class edits of jar mods) are dropped and reported: the layer cannot apply them.</li>
 * </ul>
 * String constants are left alone (ModLoader looks mods up by simple class name; reflection by obfuscated name is handled at run time).
 */
public final class BetaDeobfuscator {
    private BetaDeobfuscator() {}

    /** Local view of a class of the jar: supertypes and declared fields (name:desc). */
    private record Local(String superName, String[] interfaces, Set<String> fields, List<String[]> methods) {}

    /** Renames the classes of {@code jar} in place (keys become the new names); returns report notes. */
    public static List<String> apply(LegacyJar jar, Map<String, byte[]> classes) {
        List<String> notes = new ArrayList<>();
        BetaMappings.Names names = BetaMappings.get().side(BetaJars.side(jar));
        String pkg = BetaJars.packageOf(jar);
        if (pkg == null) pkg = "unknown";

        TreeSet<String> baseEdits = new TreeSet<>();
        TreeSet<String> apiCopies = new TreeSet<>();
        Map<String, Local> local = new HashMap<>();
        Map<String, String> localMoved = new HashMap<>();
        for (String name : List.copyOf(classes.keySet())) {
            if (names.isVanilla(name)) {
                baseEdits.add(names.classes.get(name));
                classes.remove(name);
                continue;
            }
            if (BetaJars.apiName(name) != null) {
                apiCopies.add(name);
                classes.remove(name);
                continue;
            }
            try {
                local.put(name, header(classes.get(name)));
            } catch (RuntimeException e) {
                continue; // reported by the translation
            }
            if (name.indexOf('/') < 0 || name.startsWith("net/minecraft/")) localMoved.put(name, BetaJars.MOD_ROOT + pkg + "/" + name);
        }
        if (!baseEdits.isEmpty()) {
            notes.add("Dropped " + baseEdits.size() + " modified Minecraft class(es) (base class edits cannot be applied; the mod may miss what they changed): "
                    + abbreviate(baseEdits));
        }
        if (!apiCopies.isEmpty()) notes.add("Dropped bundled copies of the ModLoader/Forge API (provided by the layer): " + abbreviate(apiCopies));

        Namer namer = new Namer(names, local, localMoved);
        // A mod class that declares a method under a name its override of a game method is renamed to would end up with two
        // identical methods: such members keep their obfuscated names in that class.
        for (var e : local.entrySet()) namer.preventCollisions(e.getKey(), e.getValue());

        Map<String, byte[]> out = new LinkedHashMap<>();
        for (var e : classes.entrySet()) {
            ClassWriter w = new ClassWriter(0);
            try {
                new ClassReader(e.getValue()).accept(new ClassRemapper(w, namer), 0);
            } catch (RuntimeException ex) {
                out.put(e.getKey(), e.getValue()); // unreadable: the translation reports it
                continue;
            }
            out.put(namer.map(e.getKey()), w.toByteArray());
        }
        classes.clear();
        classes.putAll(out);
        if (!namer.unresolvedGame.isEmpty()) {
            notes.add(namer.unresolvedGame.size() + " reference(s) to obfuscated game members have no b1.7.3 name (kept as they are): "
                    + abbreviate(namer.unresolvedGame));
        }
        return notes;
    }

    private static String abbreviate(TreeSet<String> names) {
        List<String> list = new ArrayList<>(names);
        return list.size() > 8 ? list.subList(0, 8) + " ..." : list.toString();
    }

    private static Local header(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        Set<String> fields = new HashSet<>();
        List<String[]> methods = new ArrayList<>();
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                fields.add(name + ":" + desc);
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                methods.add(new String[] {name, desc});
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return new Local(reader.getSuperName(), reader.getInterfaces(), fields, methods);
    }

    private static final class Namer extends Remapper {
        private final BetaMappings.Names names;
        private final Map<String, Local> local;
        private final Map<String, String> localMoved;
        private final Map<String, String> memo = new HashMap<>();
        private final Set<String> keep = new HashSet<>();
        final TreeSet<String> unresolvedGame = new TreeSet<>();

        Namer(BetaMappings.Names names, Map<String, Local> local, Map<String, String> localMoved) {
            this.names = names;
            this.local = local;
            this.localMoved = localMoved;
        }

        @Override
        public String map(String internalName) {
            String api = BetaJars.apiName(internalName);
            if (api != null) return api;
            String game = names.classes.get(internalName);
            if (game != null) return game;
            String moved = localMoved.get(internalName);
            if (moved != null) return moved;
            moved = BetaJars.moved(internalName);
            if (moved != null) return moved;
            // a default-package class no jar of the set has: another mod's API that came as a jar mod or an installer (ShockAhPI's
            // DimensionBase and ToolBase, PlayerAPI's PlayerBase, GuiAPI's ModSettings); the layer provides it (shim or synthesized)
            if (internalName.indexOf('/') < 0 && !internalName.isEmpty() && !internalName.startsWith("[")) return BetaJars.API_PACKAGE + internalName;
            return internalName;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            if (name.startsWith("<") || owner.startsWith("[")) return name;
            if (!keep.isEmpty() && keep.contains(owner + "." + name + descriptor)) return name;
            String hit = resolve(owner, name, descriptor, false);
            return hit != null ? hit : name;
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            if (!keep.isEmpty() && keep.contains(owner + "." + name + ":" + descriptor)) return name;
            String hit = resolve(owner, name, descriptor, true);
            return hit != null ? hit : name;
        }

        /** The game member a reference reaches through the jar's (or the mod set's) classes, or null if it is the mod's own. */
        private String resolve(String owner, String name, String desc, boolean field) {
            String key = (field ? "F" : "M") + owner + "." + name + desc;
            if (memo.containsKey(key)) return memo.get(key);
            String hit = walk(owner, name, desc, field, 0);
            // readable names on game classes are members ModLoader or Forge added to them (Block.isLadder): not obfuscated
            if (hit == null && names.isVanilla(owner) && name.length() <= 2) unresolvedGame.add(names.classes.get(owner) + "." + name + desc);
            memo.put(key, hit);
            return hit;
        }

        private String walk(String owner, String name, String desc, boolean field, int depth) {
            if (owner == null || depth > 64) return null;
            if (names.isVanilla(owner)) return field ? names.resolveField(owner, name, desc) : names.resolveMethod(owner, name, desc);
            String superName;
            String[] interfaces;
            Local l = local.get(owner);
            if (l != null) {
                // a field the mod class declares itself shadows any game field of that name
                if (field && l.fields.contains(name + ":" + desc)) return null;
                superName = l.superName;
                interfaces = l.interfaces;
            } else {
                BetaJars.ClassMeta m = BetaJars.meta(owner);
                if (m == null) return null;
                superName = m.superName();
                interfaces = m.interfaces();
            }
            String hit = walk(superName, name, desc, field, depth + 1);
            if (hit != null) return hit;
            for (String i : interfaces) {
                hit = walk(i, name, desc, field, depth + 1);
                if (hit != null) return hit;
            }
            return null;
        }

        void preventCollisions(String owner, Local l) {
            Map<String, List<String>> byTarget = new HashMap<>();
            for (String[] m : l.methods) {
                if (m[0].startsWith("<")) continue;
                String target = mapMethodName(owner, m[0], m[1]);
                byTarget.computeIfAbsent(target + mapMethodDesc(m[1]), k -> new ArrayList<>()).add(m[0] + m[1]);
            }
            for (List<String> group : byTarget.values()) {
                if (group.size() > 1) for (String m : group) keep.add(owner + "." + m);
            }
            Map<String, List<String>> fieldTargets = new HashMap<>();
            for (String f : l.fields) {
                int c = f.indexOf(':');
                String target = mapFieldName(owner, f.substring(0, c), f.substring(c + 1));
                fieldTargets.computeIfAbsent(target + ":" + mapDesc(f.substring(c + 1)), k -> new ArrayList<>()).add(f);
            }
            for (List<String> group : fieldTargets.values()) {
                if (group.size() > 1) for (String f : group) keep.add(owner + "." + f);
            }
        }
    }
}
