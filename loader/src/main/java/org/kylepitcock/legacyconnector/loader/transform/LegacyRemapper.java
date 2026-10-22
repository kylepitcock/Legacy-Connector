package org.kylepitcock.legacyconnector.loader.transform;

import java.util.HashMap;
import java.util.Map;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.mapping.McpMappings;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.synth.SynthDemand;
import org.objectweb.asm.commons.Remapper;

/**
 * Renames everything a legacy class references so that it links against the NeoForge 1.21.1 world:
 * <ul>
 *     <li>Minecraft / Forge / FML / LWJGL2 classes move under the version's shim prefix, or under its synth
 *     prefix when the hand-written shim lacks them (they are then generated, see {@code SynthJarBuilder});</li>
 *     <li>SRG member names become MCP names (the names the shim is written with);</li>
 *     <li>JDK and library classes removed since Java 8 are pointed at compat replacements;</li>
 *     <li>string constants that spell relocated class names or SRG ids are rewritten, so reflection keeps working.</li>
 * </ul>
 */
public final class LegacyRemapper extends Remapper {
    /** Classes removed from the JDK (or moved) since Java 8, mapped to replacements. Applies to every version. */
    static final Map<String, String> JDK_RELOCATIONS = Map.ofEntries(
            Map.entry("javax/xml/bind/DatatypeConverter", "org/kylepitcock/legacyconnector/compat/jdk/DatatypeConverter"),
            Map.entry("sun/misc/BASE64Encoder", "org/kylepitcock/legacyconnector/compat/jdk/BASE64Encoder"),
            Map.entry("sun/misc/BASE64Decoder", "org/kylepitcock/legacyconnector/compat/jdk/BASE64Decoder"),
            Map.entry("sun/reflect/Reflection", "org/kylepitcock/legacyconnector/compat/jdk/Reflection"),
            Map.entry("java/lang/Compiler", "org/kylepitcock/legacyconnector/compat/jdk/Compiler"),
            // Not exported since Java 9; mods constructed it instead of asking ImageIO.
            Map.entry("com/sun/imageio/plugins/png/PNGImageReader", "org/kylepitcock/legacyconnector/compat/jdk/PNGImageReader"),
            // ASM 5 -> 9: the Remapping*Adapter classes were replaced by *Remapper with the same constructors.
            Map.entry("org/objectweb/asm/commons/RemappingClassAdapter", "org/objectweb/asm/commons/ClassRemapper"),
            Map.entry("org/objectweb/asm/commons/RemappingMethodAdapter", "org/objectweb/asm/commons/MethodRemapper"),
            Map.entry("org/objectweb/asm/commons/RemappingFieldAdapter", "org/objectweb/asm/commons/FieldRemapper"),
            Map.entry("org/objectweb/asm/commons/RemappingAnnotationAdapter", "org/objectweb/asm/commons/AnnotationRemapper"),
            Map.entry("org/objectweb/asm/commons/RemappingSignatureAdapter", "org/objectweb/asm/commons/SignatureRemapper"),
            // Guava 17-21 -> 32: Objects.ToStringHelper moved to MoreObjects.
            Map.entry("com/google/common/base/Objects$ToStringHelper", "com/google/common/base/MoreObjects$ToStringHelper"),
            // commons-lang 2 was bundled with 1.7.10; only commons-lang3 ships today.
            Map.entry("org/apache/commons/lang/WordUtils", "org/apache/commons/lang3/text/WordUtils"),
            // commons-codec's Charsets constants are the JDK's StandardCharsets (same field names); not on every runtime.
            Map.entry("org/apache/commons/codec/Charsets", "java/nio/charset/StandardCharsets"));

    static final Map<String, String> JDK_PACKAGE_RELOCATIONS = Map.of(
            "jdk/nashorn/", "org/openjdk/nashorn/",
            "org/apache/commons/lang/", "org/apache/commons/lang3/",
            // log4j 2.0-beta9 (1.7.10) -> 2.x: the core helpers package became core.util.
            "org/apache/logging/log4j/core/helpers/", "org/apache/logging/log4j/core/util/");

    private final LegacyVersion version;
    private final McpMappings mcp;
    /** Per-jar overrides, e.g. classes a mod bundles inside {@code net/minecraft/} that the shim does not own. */
    private final Map<String, String> overrides = new HashMap<>();
    /** {@code owner.srgName(.desc)} members left unrenamed because the readable name would collide in that class. */
    private final java.util.Set<String> keep = new java.util.HashSet<>();
    /** {@code owner.readableName+desc} of a mod's own method that shares its name with a mapped vanilla one. */
    private final java.util.Set<String> renameLocal = new java.util.HashSet<>();

    private final ShimIndex shim;
    private final SynthDemand demand;
    private boolean recording = true;

    /** {@code shim} null disables synthesis (every legacy class is assumed to be hand-written). */
    public LegacyRemapper(LegacyVersion version, McpMappings mcp, ShimIndex shim, SynthDemand demand) {
        this.version = version;
        this.mcp = mcp;
        this.shim = shim;
        this.demand = demand;
    }

    public void override(String from, String to) {
        overrides.put(from, to);
    }

    /**
     * Mods that copied Minecraft code can declare both {@code field_70925_g} and their own {@code isShaking}; renaming
     * the SRG member would produce a duplicate (ClassFormatError). Such members keep their SRG name in that class.
     */
    public void preventCollisions(org.objectweb.asm.tree.ClassNode original) {
        preventInheritedCallCollisions(original);
        java.util.Map<String, java.util.List<String>> fields = new HashMap<>();
        for (var f : original.fields) fields.computeIfAbsent(mcp.field(f.name) + ":" + f.desc, k -> new java.util.ArrayList<>()).add(f.name);
        fields.values().forEach(names -> {
            if (names.size() > 1) for (String n : names) if (!mcp.field(n).equals(n)) keep.add(original.name + "." + n);
        });
        java.util.Map<String, java.util.List<String>> methods = new HashMap<>();
        for (var m : original.methods) methods.computeIfAbsent(mcp.method(m.name) + m.desc, k -> new java.util.ArrayList<>()).add(m.name);
        methods.forEach((key, names) -> {
            if (names.size() > 1) for (String n : names) if (!mcp.method(n).equals(n)) keep.add(original.name + "." + n + key.substring(key.indexOf('(')));
        });
    }

    /**
     * A class that declares {@code getMaximumHomeDistance()} under its readable name (a mod compiled partly with MCP
     * names) and calls the inherited vanilla method through its SRG name {@code func_110174_bM} means two different
     * methods; renaming the SRG call would make it call itself. The class's own method is renamed instead (declaration
     * and the references the jar makes to it), so the SRG call still reaches the vanilla method.
     */
    private void preventInheritedCallCollisions(org.objectweb.asm.tree.ClassNode original) {
        java.util.Set<String> declared = new java.util.HashSet<>();
        for (var m : original.methods) if (mcp.method(m.name).equals(m.name)) declared.add(m.name + m.desc);
        if (declared.isEmpty()) return;
        for (var m : original.methods) {
            if (m.instructions == null) continue;
            for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode call && call.owner.equals(original.name) && call.name.startsWith("func_")) {
                    String readable = mcp.method(call.name);
                    if (!readable.equals(call.name) && declared.contains(readable + call.desc)) {
                        renameLocal.add(original.name + "." + readable + call.desc);
                    }
                }
            }
        }
    }

    public LegacyVersion version() {
        return version;
    }

    @Override
    public String map(String internalName) {
        String o = overrides.get(internalName);
        if (o != null) return o;
        String jdk = JDK_RELOCATIONS.get(internalName);
        if (jdk != null) return jdk;
        for (var e : JDK_PACKAGE_RELOCATIONS.entrySet()) {
            if (internalName.startsWith(e.getKey())) return e.getValue() + internalName.substring(e.getKey().length());
        }
        if (LegacyVersion.isRelocatedRoot(internalName)) return relocate(internalName);
        return internalName;
    }

    private String relocate(String legacyName) {
        String shimName = version.shimPrefix() + legacyName;
        if (shim == null || shim.contains(shimName) || shim.size() == 0) return shimName;
        String synth = version.synthPrefix() + legacyName;
        if (recording && demand != null) demand.addClass(synth);
        return synth;
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        if (!keep.isEmpty() && keep.contains(owner + "." + name + descriptor)) return name;
        if (!renameLocal.isEmpty() && !name.startsWith("func_") && renameLocal.contains(owner + "." + name + descriptor)) return name + "$mod";
        return mcp.method(name);
    }

    @Override
    public String mapInvokeDynamicMethodName(String name, String descriptor) {
        return mcp.method(name);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        if (!keep.isEmpty() && keep.contains(owner + "." + name)) return name;
        return mcp.field(name);
    }

    @Override
    public Object mapValue(Object value) {
        if (value instanceof String s) return mapString(s);
        return super.mapValue(value);
    }

    /** Rewrites string literals used for reflection: class names (dotted or slashed) and SRG ids. */
    public String mapString(String s) {
        if (s.isEmpty() || s.length() > 512) return s;
        if (s.startsWith("field_")) recordReflectedField(s);
        String member = mcp.anyMember(s);
        if (member != s) return member;
        // Class.toString() / getName() forms compared against literals: "class X", "interface X", "[LX;".
        for (String prefix : new String[] {"class ", "interface "}) {
            if (s.startsWith(prefix) && s.length() > prefix.length()) return prefix + mapString(s.substring(prefix.length()));
        }
        if (s.startsWith("[") && s.endsWith(";")) {
            int dims = 0;
            while (dims < s.length() && s.charAt(dims) == '[') dims++;
            if (dims < s.length() - 2 && s.charAt(dims) == 'L') return s.substring(0, dims + 1) + mapString(s.substring(dims + 1, s.length() - 1)) + ";";
        }
        if (!looksLikeClassName(s)) return s;
        boolean dotted = s.indexOf('.') >= 0;
        String internal = dotted ? s.replace('.', '/') : s;
        // Strings only create synthetic classes the legacy version really had (reflection on Forge internals);
        // Class.forName probes for APIs that never existed there (e.g. ModLoader) must keep failing.
        recording = shim != null && LegacyVersion.isRelocatedRoot(internal)
                && org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData.forVersion(version).get(internal) != null;
        String mapped;
        try {
            mapped = map(internal);
        } finally {
            recording = true;
        }
        if (mapped.equals(internal)) {
            // Prefix checks such as name.startsWith("net.minecraft.") are relocated as well.
            if (internal.endsWith("/") && LegacyVersion.isRelocatedRoot(internal)) mapped = version.shimPrefix() + internal;
            else return s;
        }
        return dotted ? mapped.replace('/', '.') : mapped;
    }

    /**
     * A field named by its SRG id in a string is looked up by reflection; when its legacy owner is synthesized, the
     * field must be generated too (e.g. EntityBoat.isBoatEmpty, which IC2 reflects on).
     */
    private void recordReflectedField(String srg) {
        if (demand == null || shim == null || shim.size() == 0) return;
        org.kylepitcock.legacyconnector.loader.synth.LegacyFields.Field f = org.kylepitcock.legacyconnector.loader.synth.LegacyFields.bySrg(version, srg);
        if (f == null || shim.contains(version.shimPrefix() + f.owner())) return;
        String owner = map(f.owner());
        if (owner.equals(f.owner())) return;
        demand.add(owner, f.isStatic() ? SynthDemand.Kind.STATIC_FIELD : SynthDemand.Kind.FIELD, f.name(), mapDesc(f.desc()));
    }

    private static boolean looksLikeClassName(String s) {
        if (s.indexOf('/') < 0 && s.indexOf('.') < 0) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(Character.isJavaIdentifierPart(c) || c == '.' || c == '/')) return false;
        }
        return true;
    }
}
