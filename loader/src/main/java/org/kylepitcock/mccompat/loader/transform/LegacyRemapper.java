package org.kylepitcock.mccompat.loader.transform;

import java.util.HashMap;
import java.util.Map;
import org.kylepitcock.mccompat.loader.LegacyVersion;
import org.kylepitcock.mccompat.loader.mapping.McpMappings;
import org.objectweb.asm.commons.Remapper;

/**
 * Renames everything a legacy class references so that it links against the NeoForge 1.21.1 world:
 * <ul>
 *     <li>Minecraft / Forge / FML / LWJGL2 classes move under the version's shim prefix;</li>
 *     <li>SRG member names become MCP names (the names the shim is written with);</li>
 *     <li>JDK and library classes removed since Java 8 are pointed at compat replacements;</li>
 *     <li>string constants that spell relocated class names or SRG ids are rewritten, so reflection keeps working.</li>
 * </ul>
 */
public final class LegacyRemapper extends Remapper {
    /** Classes removed from the JDK (or moved) since Java 8, mapped to replacements. Applies to every version. */
    static final Map<String, String> JDK_RELOCATIONS = Map.ofEntries(
            Map.entry("javax/xml/bind/DatatypeConverter", "org/kylepitcock/mccompat/compat/jdk/DatatypeConverter"),
            Map.entry("sun/misc/BASE64Encoder", "org/kylepitcock/mccompat/compat/jdk/BASE64Encoder"),
            Map.entry("sun/misc/BASE64Decoder", "org/kylepitcock/mccompat/compat/jdk/BASE64Decoder"),
            Map.entry("sun/reflect/Reflection", "org/kylepitcock/mccompat/compat/jdk/Reflection"),
            Map.entry("java/lang/Compiler", "org/kylepitcock/mccompat/compat/jdk/Compiler"),
            // Guava 17-21 -> 32: Objects.ToStringHelper moved to MoreObjects.
            Map.entry("com/google/common/base/Objects$ToStringHelper", "com/google/common/base/MoreObjects$ToStringHelper"),
            // commons-lang 2 was bundled with 1.7.10; only commons-lang3 ships today.
            Map.entry("org/apache/commons/lang/WordUtils", "org/apache/commons/lang3/text/WordUtils"));

    static final Map<String, String> JDK_PACKAGE_RELOCATIONS = Map.of(
            "jdk/nashorn/", "org/openjdk/nashorn/",
            "org/apache/commons/lang/", "org/apache/commons/lang3/");

    private final LegacyVersion version;
    private final McpMappings mcp;
    /** Per-jar overrides, e.g. classes a mod bundles inside {@code net/minecraft/} that the shim does not own. */
    private final Map<String, String> overrides = new HashMap<>();

    public LegacyRemapper(LegacyVersion version, McpMappings mcp) {
        this.version = version;
        this.mcp = mcp;
    }

    public void override(String from, String to) {
        overrides.put(from, to);
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
        if (LegacyVersion.isRelocatedRoot(internalName)) return version.shimPrefix() + internalName;
        return internalName;
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        return mcp.method(name);
    }

    @Override
    public String mapInvokeDynamicMethodName(String name, String descriptor) {
        return mcp.method(name);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
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
        String member = mcp.anyMember(s);
        if (member != s) return member;
        if (!looksLikeClassName(s)) return s;
        boolean dotted = s.indexOf('.') >= 0;
        String internal = dotted ? s.replace('.', '/') : s;
        String mapped = map(internal);
        if (mapped.equals(internal)) {
            // Prefix checks such as name.startsWith("net.minecraft.") are relocated as well.
            if (internal.endsWith("/") && LegacyVersion.isRelocatedRoot(internal)) mapped = version.shimPrefix() + internal;
            else return s;
        }
        return dotted ? mapped.replace('/', '.') : mapped;
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
