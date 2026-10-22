package org.kylepitcock.legacyconnector.loader.transform;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndexGenerator;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData;
import org.objectweb.asm.ClassReader;

/**
 * Class lookup across the three worlds a transformed legacy class can link against:
 * the mod's own jar, the shim, and libraries/JDK visible to the loader.
 */
public final class Hierarchy {
    /** Library/JDK classes are immutable for the process lifetime, so the cache is shared between jars. */
    private static final Map<String, Optional<ClassInfo>> LIBRARY_CACHE = new ConcurrentHashMap<>();

    private final Map<String, ClassInfo> local = new HashMap<>();
    private final ShimIndex shim;
    private final ClassLoader libraries;
    /** Classes of the other legacy jars being loaded alongside (mods extend each other's classes). */
    private final Map<String, ClassInfo> peers;
    private final LegacyVersion version;
    private final Map<String, ClassInfo> synth = new HashMap<>();

    public Hierarchy(ShimIndex shim, ClassLoader libraries) {
        this(shim, libraries, Map.of(), null);
    }

    public Hierarchy(ShimIndex shim, ClassLoader libraries, Map<String, ClassInfo> peers, LegacyVersion version) {
        this.shim = shim;
        this.libraries = libraries;
        this.peers = peers;
        this.version = version;
    }

    /** Library namespaces present at runtime but possibly invisible to the loader's class loader (game layer). */
    private static final String[] RUNTIME_LIBRARIES = {"java/", "javax/", "jdk/", "sun/", "com/google/", "io/netty/", "org/apache/",
            "com/mojang/", "org/lwjgl/", "gnu/trove/", "it/unimi/", "org/slf4j/", "com/typesafe/", "org/objectweb/", "net/minecraft/",
            "net/neoforged/", "org/joml/", "paulscode/", "com/ibm/icu/", "org/openjdk/nashorn/", "oshi/", "org/kylepitcock/",
            "scala/", "javax/vecmath/"};

    /**
     * True only for a class that cannot exist at runtime: not in any legacy jar, the shim, the synthesized API or a
     * runtime library. Used to detect integrations with mods that are not installed.
     */
    public boolean isAbsent(String name) {
        if (name.startsWith("legacyconnector") || isLocal(name) || isPeer(name) || isShim(name) || isSynth(name)) return false;
        for (String p : RUNTIME_LIBRARIES) if (name.startsWith(p)) return false;
        return find(name) == null;
    }

    public boolean isPeer(String name) {
        return peers.containsKey(name);
    }

    public String synthPrefix() {
        return version.synthPrefix();
    }

    public boolean isSynth(String name) {
        return version != null && name.startsWith(version.synthPrefix());
    }

    /** Maps an unrelocated legacy name the way {@link LegacyRemapper} does. */
    public String relocate(String legacyName) {
        if (legacyName == null) return null;
        if (!LegacyVersion.isRelocatedRoot(legacyName)) return legacyName;
        String s = version.shimPrefix() + legacyName;
        return shim.contains(s) || shim.size() == 0 ? s : version.synthPrefix() + legacyName;
    }

    /**
     * Structural view of a synthesized class: legacy supertypes (relocated), no members. Members are what the
     * mods demand, so lookups that reach a synthesized class without finding a member must demand it there.
     */
    private ClassInfo synthInfo(String name) {
        return synth.computeIfAbsent(name, n -> {
            String legacy = n.substring(version.synthPrefix().length());
            LegacyHierarchyData.Entry e = LegacyHierarchyData.forVersion(version).get(legacy);
            if (e == null) return new ClassInfo(n, "java/lang/Object", java.util.List.of(), org.objectweb.asm.Opcodes.ACC_PUBLIC);
            java.util.List<String> ifaces = new java.util.ArrayList<>();
            for (String i : e.interfaces()) ifaces.add(relocate(i));
            return new ClassInfo(n, relocate(e.superName()), ifaces, e.access() | org.objectweb.asm.Opcodes.ACC_PUBLIC);
        });
    }

    /** Legacy hierarchy entry for a synthesized class, or null when the data does not know it. */
    public LegacyHierarchyData.Entry legacyEntry(String synthName) {
        return LegacyHierarchyData.forVersion(version).get(synthName.substring(version.synthPrefix().length()));
    }

    public void addLocal(ClassInfo info) {
        local.put(info.name, info);
    }

    public boolean isLocal(String name) {
        return local.containsKey(name);
    }

    public boolean isShim(String name) {
        return shim.contains(name);
    }

    public ShimIndex shim() {
        return shim;
    }

    /** Null when the class cannot be found anywhere (typically an optional dependency on another mod). */
    public ClassInfo find(String name) {
        ClassInfo info = local.get(name);
        if (info != null) return info;
        info = shim.get(name);
        if (info != null) return info;
        if (isSynth(name)) return synthInfo(name);
        info = peers.get(name);
        if (info != null) return info;
        if (name.startsWith("[")) return null;
        return LIBRARY_CACHE.computeIfAbsent(name, this::loadLibrary).orElse(null);
    }

    private Optional<ClassInfo> loadLibrary(String name) {
        if (libraries == null) return Optional.empty();
        try (InputStream in = libraries.getResourceAsStream(name + ".class")) {
            if (in == null) return Optional.empty();
            return Optional.of(ShimIndexGenerator.read(new ClassReader(in)));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    public enum Lookup { FOUND, MISSING_MEMBER, MISSING_CLASS, UNKNOWN }

    /**
     * Resolves a method following JVM rules loosely (superclasses, then superinterfaces).
     * {@code UNKNOWN} means some ancestor could not be inspected, so absence is not proven.
     */
    public Lookup resolveMethod(String owner, String name, String desc) {
        ClassInfo start = find(owner);
        if (start == null) return Lookup.MISSING_CLASS;
        if (name.equals("<init>")) return start.hasMethod(name, desc) ? Lookup.FOUND : Lookup.MISSING_MEMBER;
        return searchMethod(start, name, desc, new java.util.HashSet<>());
    }

    private Lookup searchMethod(ClassInfo c, String name, String desc, java.util.Set<String> seen) {
        if (!seen.add(c.name)) return Lookup.MISSING_MEMBER;
        if (c.hasMethod(name, desc)) return Lookup.FOUND;
        // Signature-polymorphic methods (MethodHandle.invoke*, VarHandle.*) accept any descriptor.
        if ((c.name.equals("java/lang/invoke/MethodHandle") || c.name.equals("java/lang/invoke/VarHandle"))
                && c.methods.keySet().stream().anyMatch(k -> k.startsWith(name + "("))) {
            return Lookup.FOUND;
        }
        boolean unknown = false;
        if (c.superName != null) {
            ClassInfo sup = find(c.superName);
            if (sup == null) unknown = true;
            else {
                Lookup l = searchMethod(sup, name, desc, seen);
                if (l == Lookup.FOUND) return l;
                if (l == Lookup.UNKNOWN) unknown = true;
            }
        }
        for (String iface : c.interfaces) {
            ClassInfo i = find(iface);
            if (i == null) unknown = true;
            else {
                Lookup l = searchMethod(i, name, desc, seen);
                if (l == Lookup.FOUND) return l;
                if (l == Lookup.UNKNOWN) unknown = true;
            }
        }
        // Interfaces implicitly inherit Object's public methods.
        if ((c.access & org.objectweb.asm.Opcodes.ACC_INTERFACE) != 0 && !seen.contains("java/lang/Object")) {
            ClassInfo obj = find("java/lang/Object");
            if (obj != null && obj.hasMethod(name, desc)) return Lookup.FOUND;
        }
        return unknown ? Lookup.UNKNOWN : Lookup.MISSING_MEMBER;
    }

    public Lookup resolveField(String owner, String name, String desc) {
        ClassInfo start = find(owner);
        if (start == null) return Lookup.MISSING_CLASS;
        java.util.ArrayDeque<ClassInfo> queue = new java.util.ArrayDeque<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        queue.add(start);
        boolean unknown = false;
        while (!queue.isEmpty()) {
            ClassInfo c = queue.poll();
            if (!seen.add(c.name)) continue;
            if (c.hasField(name, desc)) return Lookup.FOUND;
            for (String parent : parents(c)) {
                ClassInfo p = find(parent);
                if (p == null) unknown = true;
                else queue.add(p);
            }
        }
        return unknown ? Lookup.UNKNOWN : Lookup.MISSING_MEMBER;
    }

    /**
     * Finds the shim class that offers a static virtual-field accessor for {@code owner.name}, i.e. a method
     * {@code mcc$get$name(LDeclaring;)desc} (or {@code ()desc} for static fields). Returns null if the field
     * is real, belongs to the mod, or no accessor exists.
     */
    public String findVirtualFieldOwner(String owner, String name, String desc, boolean isStatic) {
        ClassInfo c = find(owner);
        java.util.Set<String> seen = new java.util.HashSet<>();
        while (c != null && seen.add(c.name)) {
            if (shim.contains(c.name)) {
                // Shim classes declare the phantom field (so legacy code links) *and* the accessor; accessor wins.
                String getterDesc = isStatic ? "()" + desc : "(L" + c.name + ";)" + desc;
                if (c.hasMethod(ShimIndex.GETTER_PREFIX + name, getterDesc)) return c.name;
            }
            if (c.hasField(name, desc)) return null; // a real field (e.g. declared by the mod) shadows inherited accessors
            c = c.superName == null ? null : find(c.superName);
        }
        return null;
    }

    private static java.util.List<String> parents(ClassInfo c) {
        java.util.List<String> out = new java.util.ArrayList<>(c.interfaces);
        if (c.superName != null) out.add(0, c.superName);
        return out;
    }
}
