package org.kylepitcock.legacyconnector.loader.transform;

import java.util.HashMap;
import java.util.Map;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardMembers;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardRenames;
import org.kylepitcock.legacyconnector.loader.mapping.NotchNames;
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
            Map.entry("javax/xml/bind/annotation/adapters/HexBinaryAdapter", "org/kylepitcock/legacyconnector/compat/jdk/HexBinaryAdapter"),
            Map.entry("sun/misc/BASE64Encoder", "org/kylepitcock/legacyconnector/compat/jdk/BASE64Encoder"),
            Map.entry("sun/misc/BASE64Decoder", "org/kylepitcock/legacyconnector/compat/jdk/BASE64Decoder"),
            Map.entry("sun/reflect/Reflection", "org/kylepitcock/legacyconnector/compat/jdk/Reflection"),
            // JDK 9+: only the serialization constructors are left; mods built enum constants through newConstructorAccessor (Constructor.newInstance rejects enums)
            Map.entry("sun/reflect/ReflectionFactory", "org/kylepitcock/legacyconnector/compat/jdk/ReflectionFactory"),
            Map.entry("sun/reflect/ConstructorAccessor", "org/kylepitcock/legacyconnector/compat/jdk/ConstructorAccessor"),
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
            // log4j's string helpers moved to the API jar (JourneyMap's version check)
            Map.entry("org/apache/logging/log4j/core/helpers/Strings", "org/apache/logging/log4j/util/Strings"),
            Map.entry("org/apache/logging/log4j/core/util/Strings", "org/apache/logging/log4j/util/Strings"),
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
        this.notch = version.numericIds() ? NotchNames.get(version.id()) : null;
        ForwardMembers table = version.forward() == null ? null : ForwardMembers.forVersion(version);
        this.members = table == null || table.isEmpty() ? null : table;
    }

    /** Member renames of a forward version (1.6.4 to 1.7.10 names), null for the others. */
    private final ForwardMembers members;

    /** The MCP name of a (possibly notch-named) legacy class, as the forward tables and the 1.6.4 hierarchy data use them. */
    private String legacyClass(String name) {
        if (notch != null && name.indexOf('/') < 0 && !notchLocals.containsKey(name)) {
            String mcpName = notch.mcpClass(name);
            if (mcpName != null) return mcpName;
        }
        return name;
    }

    /** Supertypes of a class the 1.6.4 hierarchy data does not know: the jar's own classes. */
    private String[] localSupers(String name) {
        String[] up = notchLocals.get(name);
        if (up == null) return null;
        String[] out = new String[up.length];
        for (int i = 0; i < up.length; i++) out[i] = up[i] == null ? null : legacyClass(up[i]);
        return out;
    }

    /** A descriptor with notch class names replaced by MCP names, the form the forward tables use. */
    private String legacyDesc(String desc) {
        String d = notchDesc(desc);
        return version.flatNames() && d.contains("net/minecraft/src/") ? flatDesc(d) : d;
    }

    private String notchDesc(String desc) {
        if (notch == null || !hasBareClass(desc)) return desc;
        StringBuilder out = new StringBuilder(desc.length() + 16);
        for (int i = 0; i < desc.length(); i++) {
            char ch = desc.charAt(i);
            out.append(ch);
            if (ch == 'L') {
                int end = desc.indexOf(';', i);
                if (end < 0) break;
                out.append(legacyClass(desc.substring(i + 1, end))).append(';');
                i = end;
            }
        }
        return out.toString();
    }

    /** A descriptor with flat {@code net/minecraft/src} names (1.4.7) replaced by the packaged names the forward tables use. */
    private String flatDesc(String desc) {
        StringBuilder out = new StringBuilder(desc.length() + 16);
        for (int i = 0; i < desc.length(); i++) {
            char ch = desc.charAt(i);
            out.append(ch);
            if (ch == 'L') {
                int end = desc.indexOf(';', i);
                if (end < 0) break;
                out.append(ForwardRenames.flat(version, desc.substring(i + 1, end))).append(';');
                i = end;
            }
        }
        return out.toString();
    }

    private static boolean hasBareClass(String desc) {
        for (int i = 0; i < desc.length(); i++) {
            if (desc.charAt(i) == 'L') {
                int end = desc.indexOf(';', i);
                if (end < 0) return false;
                int slash = desc.indexOf('/', i);
                if (slash < 0 || slash > end) return true;
                i = end;
            }
        }
        return false;
    }

    /**
     * Supertypes ({@code [super, interfaces...]}) of the jar's own classes, so a notch-named member inherited through a mod class is found.
     * Only 1.6.4 uses it (see {@link NotchNames}); a jar's own default-package class named like a notch class is not renamed.
     */
    private java.util.Map<String, String[]> notchLocals = java.util.Map.of();
    private final NotchNames notch;

    public void notchLocals(java.util.Map<String, String[]> locals) {
        this.notchLocals = locals;
    }

    public void override(String from, String to) {
        overrides.put(from, to);
    }

    /** A default-package class of another jar, which that jar moved to {@code to}; vanilla (notch) and ModLoader names are not taken for it. */
    public void peerDefaultPackage(String from, String to) {
        if (MODLOADER_CLASSES.contains(from) || (notch != null && notch.mcpClass(from) != null)) return;
        overrides.putIfAbsent(from, to);
    }

    /** Packages that jars of several legacy versions define (see {@link CrossVersionPackages}). */
    private CrossVersionPackages crossVersion = CrossVersionPackages.NONE;

    public void crossVersion(CrossVersionPackages crossVersion) {
        this.crossVersion = crossVersion;
    }

    /**
     * Mods that copied Minecraft code can declare both {@code field_70925_g} and their own {@code isShaking}; renaming
     * the SRG member would produce a duplicate (ClassFormatError). Such members keep their SRG name in that class.
     */
    public void preventCollisions(org.objectweb.asm.tree.ClassNode original) {
        preventInheritedCallCollisions(original);
        java.util.Map<String, java.util.List<String>> fields = new HashMap<>();
        for (var f : original.fields) fields.computeIfAbsent(readableField(original.name, f.name) + ":" + f.desc, k -> new java.util.ArrayList<>()).add(f.name);
        fields.values().forEach(names -> {
            if (names.size() > 1) for (String n : names) if (!readableField(original.name, n).equals(n)) keep.add(original.name + "." + n);
        });
        if (members != null) {
            // A forward rename (1.5.2 Entity.getEntityName to 1.7.10 getCommandSenderName) must not collide with a method the class declares under
            // the new name already (1.5.2 mods implement both; the two merged in 1.7.10): the class keeps its old name, calls through it too.
            java.util.Set<String> declaredReadable = new java.util.HashSet<>();
            for (var m : original.methods) declaredReadable.add(readable(original.name, m.name, m.desc) + m.desc);
            for (var m : original.methods) {
                String readable = readable(original.name, m.name, m.desc);
                if (readable.startsWith("func_") || readable.startsWith("<")) continue;
                String forwarded = members.method(legacyClass(original.name), readable, legacyDesc(m.desc), this::localSupers);
                if (!forwarded.equals(readable) && declaredReadable.contains(forwarded + m.desc)) keep.add(original.name + "." + m.name + m.desc);
            }
        }
        java.util.Map<String, java.util.List<String>> methods = new HashMap<>();
        for (var m : original.methods) methods.computeIfAbsent(readable(original.name, m.name, m.desc) + m.desc, k -> new java.util.ArrayList<>()).add(m.name);
        methods.forEach((key, names) -> {
            if (names.size() > 1) for (String n : names) if (!readable(original.name, n, key.substring(key.indexOf('('))).equals(n)) keep.add(original.name + "." + n + key.substring(key.indexOf('(')));
        });
        if (members != null) {
            // a forward rename (getEntityName -> getCommandSenderName) onto a method the class declares itself, or two renames onto one name, would duplicate it
            java.util.Map<String, java.util.List<org.objectweb.asm.tree.MethodNode>> byTarget = new HashMap<>();
            for (var m : original.methods) {
                String readable = readable(original.name, m.name, m.desc);
                String mapped = members.method(legacyClass(original.name), readable, legacyDesc(m.desc), this::localSupers);
                byTarget.computeIfAbsent(mapped + m.desc, k -> new java.util.ArrayList<>()).add(m);
            }
            byTarget.forEach((key, list) -> {
                if (list.size() < 2) return;
                String target = key.substring(0, key.indexOf('('));
                boolean declaresTarget = list.stream().anyMatch(m -> readable(original.name, m.name, m.desc).equals(target));
                boolean first = true;
                for (var m : list) {
                    if (readable(original.name, m.name, m.desc).equals(target)) continue; // the declaration that already has the target name stays
                    if (!declaresTarget && first) { first = false; continue; } // one of the renamed ones may take the name
                    keep.add(original.name + "." + m.name + m.desc);
                }
            });
        }
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
                    // a forward version renames some SRG names itself (1.6.4 func_110174_bM -> getMaximumHomeDistance): the final name counts
                    if (members != null) readable = members.method(legacyClass(original.name), readable, legacyDesc(call.desc), this::localSupers);
                    if (!readable.equals(call.name) && declared.contains(readable + call.desc)) {
                        renameLocal.add(original.name + "." + readable + call.desc);
                    }
                }
            }
        }
    }

    /** The readable (MCP) name of a method a class declares, as {@link #mapMethodName} will produce it (a notch-named jar's declarations go through the notch table first). */
    private String readable(String owner, String name, String desc) {
        if (notch != null && !name.startsWith("func_")) {
            String srg = notch.method(owner, name, desc, notchLocals);
            if (srg != null) name = srg;
        }
        return mcp.method(name);
    }

    private String readableField(String owner, String name) {
        if (notch != null && !name.startsWith("field_")) {
            String srg = notch.field(owner, name, notchLocals);
            if (srg != null) name = srg;
        }
        return mcp.field(name);
    }

    public LegacyVersion version() {
        return version;
    }

    /**
     * Suffix {@link Living152ClassRemapper} puts on an {@code EntityLiving} reference that must stay the real 1.7.10 {@code EntityLiving} in a 1.5.2 jar
     * (the superclass of a mod entity, {@code super.} calls); every other use of it becomes {@code EntityLivingBase} (see {@link LivingCasts152}).
     */
    static final String LIVING_KEEP = "$mcc152keep";

    /** True for the class a 1.5.2 jar calls {@code EntityLiving} (also under its obfuscated name). */
    boolean isLiving152(String internalName) {
        return (version == LegacyVersion.V1_5_2 || version == LegacyVersion.V1_4_7) && internalName != null && "net/minecraft/entity/EntityLiving".equals(ForwardRenames.flat(version, legacyClass(internalName)));
    }

    @Override
    public String map(String internalName) {
        if (internalName.endsWith(LIVING_KEEP)) return version.linkPrefix() + "net/minecraft/entity/EntityLiving";
        String o = overrides.get(internalName);
        if (o != null) return o;
        if (notch != null && internalName.indexOf('/') < 0 && !notchLocals.containsKey(internalName)) {
            String mcp = notch.mcpClass(internalName);
            if (mcp != null) return map(mcp);
        }
        if (internalName.indexOf('/') < 0 && MODLOADER_CLASSES.contains(internalName) && !notchLocals.containsKey(internalName)) {
            // ModLoader's classes are not obfuscated; a reobfuscated jar names them in the default package (mod_X extends BaseMod)
            return map("net/minecraft/src/" + internalName);
        }
        String jdk = JDK_RELOCATIONS.get(internalName);
        if (jdk != null) return jdk;
        for (var e : JDK_PACKAGE_RELOCATIONS.entrySet()) {
            if (internalName.startsWith(e.getKey())) return e.getValue() + internalName.substring(e.getKey().length());
        }
        if (LegacyVersion.isRelocatedRoot(internalName) || relocatedForVersion(internalName)) return relocate(internalName);
        String moved = crossVersion.map(version, internalName);
        if (moved != null) return moved;
        // String literals keep keyword segments: "class.insecta" (Forestry) is an id, not a package.
        return mappingString ? internalName : renameKeywordPackages(internalName);
    }

    private boolean mappingString;

    /** The ModLoader emulation classes a reobfuscated 1.4.x jar references by their bare names (see {@code legacyconnector/v1_6_4/net/minecraft/src}). */
    private static final java.util.Set<String> MODLOADER_CLASSES = java.util.Set.of("BaseMod", "ModLoader", "MLProp", "TradeEntry", "ModTextureStatic",
            "ModTextureAnimation", "EntityRendererProxy");

    private static final java.util.Set<String> JAVA_KEYWORDS = java.util.Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
            "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null", "_");

    /**
     * Kotlin and Scala mods can use Java keywords as package names ({@code ejektaflex.bountiful.api.enum}); the JVM
     * accepts them but module resolution rejects the whole jar ("enum is not a Java identifier"), which stops the
     * launch. Such segments get a {@code _} suffix; every legacy jar is remapped the same way, so references between
     * mods stay consistent.
     */
    static String renameKeywordPackages(String internalName) {
        int end = internalName.lastIndexOf('/');
        if (end < 0) return internalName;
        StringBuilder out = null;
        int start = 0;
        while (start < end) {
            int slash = internalName.indexOf('/', start);
            String segment = internalName.substring(start, slash);
            if (JAVA_KEYWORDS.contains(segment)) {
                if (out == null) out = new StringBuilder(internalName.length() + 4).append(internalName, 0, start);
                out.append(segment).append("_/");
            } else if (out != null) {
                out.append(segment).append('/');
            }
            start = slash + 1;
        }
        return out == null ? internalName : out.append(internalName, end + 1, internalName.length()).toString();
    }

    /**
     * Names relocated into the shim (or the synthesized API) for this version only, on top of {@link LegacyVersion#isRelocatedRoot}.
     * A jar that bundles such a class itself (a Beta library shipping TWL) keeps its copy under its private root unless the shim has
     * the class (see {@code LegacyJarTransformer.relocateBundled}); otherwise the jar and the synthesized-API jar would both export the
     * package and the module layer refuses to boot (#220).
     */
    boolean relocatedForVersion(String internalName) {
        if (version != LegacyVersion.VB1_7_3) return false;
        // Beta GuiAPI uses TWL model/widget types; preserve their hierarchy through the versioned shims.
        if (internalName.startsWith("de/matthiasmann/twl/")) return true;
        // b1.7.3 mods call LWJGL's OpenAL directly while they load (IndustrialCraft 2's audio): the real AL classes initialised on the
        // mod-loading thread, before the sound engine made a context, fail for good and the client's resource reload then never ends.
        // Their OpenAL goes to the synthesized API (silent) until the layer emulates it.
        if (internalName.startsWith("org/lwjgl/openal/")) return true;
        // LWJGL 2's BufferUtils (direct NIO buffers): the shim's works without LWJGL, so client builds of mods load on a dedicated server
        return internalName.equals("org/lwjgl/BufferUtils");
    }

    private String relocate(String legacyName) {
        String shimName = version.linkPrefix() + ForwardRenames.map(version, legacyName);
        // A forward version's own classes (1.6.4's network/tick API, which 1.7.10 lacks or shapes differently) win over the forwarded shim.
        if (shim != null && version.forward() != null) {
            String own = version.shimPrefix() + ForwardRenames.map(version, legacyName);
            if (shim.contains(own)) return own;
        }
        if (shim == null || shim.contains(shimName) || shim.size() == 0) return shimName;
        // one name per class: a flat 1.4.7 name (reached through the notch table) and its packaged name (reached through a Forge class's supertypes) are the same class
        String synth = version.synthPrefix() + ForwardRenames.flat(version, legacyName);
        if (recording && demand != null) demand.addClass(synth);
        return synth;
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        if (owner.endsWith(LIVING_KEEP)) owner = owner.substring(0, owner.length() - LIVING_KEEP.length());
        if (!keep.isEmpty() && keep.contains(owner + "." + name + descriptor)) return name;
        if (notch != null && !name.startsWith("func_")) {
            String srg = notch.method(owner, name, descriptor, notchLocals);
            if (srg != null) name = srg;
        }
        if (!renameLocal.isEmpty() && !name.startsWith("func_") && renameLocal.contains(owner + "." + name + descriptor)) return name + "$mod";
        String readable = mcp.method(name);
        if (members != null) {
            return members.method(legacyClass(owner), readable, legacyDesc(descriptor), this::localSupers);
        }
        return readable;
    }

    @Override
    public String mapInvokeDynamicMethodName(String name, String descriptor) {
        return mcp.method(name);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        if (owner.endsWith(LIVING_KEEP)) owner = owner.substring(0, owner.length() - LIVING_KEEP.length());
        if (!keep.isEmpty() && keep.contains(owner + "." + name)) return name;
        if (notch != null && !name.startsWith("field_")) {
            String srg = notch.field(owner, name, notchLocals);
            if (srg != null) name = srg;
        }
        String readable = mcp.field(name);
        if (members != null) return members.field(legacyClass(owner), readable, this::localSupers);
        return readable;
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
        if (s.charAt(0) == '(' && DESCRIPTOR.matcher(s).matches()) {
            // a method descriptor used for reflection (CodeChickenLib's ObfMapping finds methods by name and descriptor)
            java.util.regex.Matcher m = CLASS_IN_DESCRIPTOR.matcher(s);
            StringBuilder out = new StringBuilder();
            while (m.find()) m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement("L" + mapStringClass(m.group(1)) + ";"));
            m.appendTail(out);
            return out.toString();
        }
        if (!looksLikeClassName(s)) return s;
        boolean dotted = s.indexOf('.') >= 0;
        String internal = dotted ? s.replace('.', '/') : s;
        String mapped = mapStringClass(internal);
        if (mapped.equals(internal)) {
            // Prefix checks such as name.startsWith("net.minecraft.") are relocated as well.
            if (internal.endsWith("/") && LegacyVersion.isRelocatedRoot(internal)) mapped = version.linkPrefix() + internal;
            else return s;
        }
        return dotted ? mapped.replace('/', '.') : mapped;
    }

    private static final java.util.regex.Pattern DESCRIPTOR =
            java.util.regex.Pattern.compile("\\((\\[*([BCDFIJSZ]|L[\\w/$]+;))*\\)(V|\\[*([BCDFIJSZ]|L[\\w/$]+;))");
    private static final java.util.regex.Pattern CLASS_IN_DESCRIPTOR = java.util.regex.Pattern.compile("L([\\w/$]+);");

    /** The relocated internal name of a class named by a string (unchanged when it is not a class of the legacy API). */
    private String mapStringClass(String internal) {
        // Strings only create synthetic classes the legacy version really had (reflection on Forge internals);
        // Class.forName probes for APIs that never existed there (e.g. ModLoader) must keep failing.
        recording = shim != null && LegacyVersion.isRelocatedRoot(internal)
                && org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData.forVersion(version).get(internal) != null;
        mappingString = true;
        try {
            return map(internal);
        } finally {
            recording = true;
            mappingString = false;
        }
    }

    /**
     * A field named by its SRG id in a string is looked up by reflection; when its legacy owner is synthesized, the
     * field must be generated too (e.g. EntityBoat.isBoatEmpty, which IC2 reflects on).
     */
    private void recordReflectedField(String srg) {
        if (demand == null || shim == null || shim.size() == 0) return;
        org.kylepitcock.legacyconnector.loader.synth.LegacyFields.Field f = org.kylepitcock.legacyconnector.loader.synth.LegacyFields.bySrg(version, srg);
        if (f == null || shim.contains(version.linkPrefix() + ForwardRenames.map(version, f.owner()))) return;
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
