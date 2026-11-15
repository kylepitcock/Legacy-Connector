package org.kylepitcock.legacyconnector.loader.tools.forge1201;

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
import java.util.Locale;
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
 * Detects Forge 1.20.1 (47.x) mods from META-INF/mods.toml and reads their dependency blocks (offline report tool).
 * Ported from kentvillamor08's fork; the real jar detection lives in {@code LegacyJarInspector}.
 */
public final class Forge1201JarInspector {
    private Forge1201JarInspector() {}

    /**
     * Recognizes the narrow Forge 1.20.1 target family without sending it through the legacy 1.7/1.12
     * bytecode remapper. This is intentionally a detection-only path until the 1.20.1 API bridge exists.
     */
    public static Forge1201JarInfo inspectForge1201(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            if (zip.getEntry("org/kylepitcock/legacyconnector/loader/LegacyModLocator.class") != null
                    || zip.getEntry(org.kylepitcock.legacyconnector.loader.Provenance.RESOURCE) != null
                    || zip.getEntry("META-INF/neoforge.mods.toml") != null
                    || zip.getEntry("fabric.mod.json") != null
                    || zip.getEntry("quilt.mod.json") != null) {
                return null;
            }

            ZipEntry entry = zip.getEntry("META-INF/mods.toml");
            if (entry == null) return null;
            String toml;
            try (InputStream in = zip.getInputStream(entry)) {
                toml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            return inspectForge1201Metadata(path, toml);
        }
    }

    /**
     * Parses a Forge metadata document already read from an archive. The caller is responsible for
     * checking loader-marker files (for example neoforge.mods.toml) before using this Forge-specific
     * recognizer. This overload allows bounded in-memory inspection of Jar-in-Jar archives.
     */
    public static Forge1201JarInfo inspectForge1201Metadata(Path path, String toml) {
        List<TomlSection> sections = parseTomlSections(toml);
        Map<String, String> root = sections.stream()
                .filter(section -> section.name().isEmpty())
                .map(TomlSection::values)
                .findFirst()
                .orElse(Map.of());
        String modLoader = root.get("modLoader");
        String loaderVersion = root.get("loaderVersion");
        if (!"javafml".equalsIgnoreCase(modLoader) || !hasMajorRangeLowerBound(loaderVersion, "47")) {
            return null;
        }

        String minecraftRange = dependencyRange(sections, "minecraft");
        String forgeRange = dependencyRange(sections, "forge");
        if (!hasRangeLowerBound(minecraftRange, "1.20.1")
                || !hasMajorRangeLowerBound(forgeRange, "47")) {
            return null;
        }

        List<Forge1201JarInfo.Mod> mods = readModMetadata(sections);
        if (mods.isEmpty()) return null;
        return new Forge1201JarInfo(path, modLoader, loaderVersion, minecraftRange, forgeRange, mods);
    }

    /**
     * Reads scalar mod declarations from either Forge or NeoForge mods.toml text.
     * Unsupported TOML constructs remain the caller's responsibility to disclose.
     */
    /** Reads mod and legacy mandatory-flag dependency metadata from Forge mods.toml. */
    public static List<Forge1201JarInfo.Mod> readModMetadata(String toml) {
        return readModMetadata(parseTomlSections(toml), false);
    }

    /** Reads mod and typed dependency metadata from NeoForge neoforge.mods.toml. */
    public static List<Forge1201JarInfo.Mod> readNeoForgeModMetadata(String toml) {
        return readModMetadata(parseTomlSections(toml), true);
    }

    private static List<Forge1201JarInfo.Mod> readModMetadata(List<TomlSection> sections) {
        return readModMetadata(sections, false);
    }

    private static List<Forge1201JarInfo.Mod> readModMetadata(
            List<TomlSection> sections, boolean neoForgeMetadata) {
        List<Forge1201JarInfo.Mod> mods = new ArrayList<>();
        for (TomlSection section : sections) {
            if (!section.name().equals("mods")) continue;
            String modId = section.values().get("modId");
            if (modId == null || !modId.matches("[a-z][a-z0-9_]{1,63}")) continue;
            mods.add(new Forge1201JarInfo.Mod(
                    modId,
                    section.values().getOrDefault("version", "unspecified"),
                    section.values().getOrDefault("displayName", modId),
                    dependenciesForMod(sections, modId, neoForgeMetadata)));
        }
        return List.copyOf(mods);
    }

    private static List<Forge1201JarInfo.Dependency> dependenciesForMod(
            List<TomlSection> sections, String ownerModId, boolean neoForgeMetadata) {
        List<Forge1201JarInfo.Dependency> dependencies = new ArrayList<>();
        String prefix = "dependencies.";
        for (TomlSection section : sections) {
            if (!section.name().startsWith(prefix)
                    || !section.name().substring(prefix.length()).equals(ownerModId)) {
                continue;
            }
            Map<String, String> values = section.values();
            String dependencyId = values.get("modId");
            if (dependencyId == null || dependencyId.isBlank()) continue;
            String declaredType = values.get("type");
            Forge1201JarInfo.DependencyType dependencyType;
            if (neoForgeMetadata && (declaredType == null || declaredType.isBlank())) {
                // NeoForge defaults to required; the legacy Forge mandatory key is not its dependency-type field.
                dependencyType = Forge1201JarInfo.DependencyType.REQUIRED;
            } else if (!neoForgeMetadata && (declaredType == null || declaredType.isBlank())) {
                dependencyType = "false".equalsIgnoreCase(values.getOrDefault("mandatory", "true"))
                        ? Forge1201JarInfo.DependencyType.OPTIONAL
                        : Forge1201JarInfo.DependencyType.REQUIRED;
            } else {
                try {
                    dependencyType = Forge1201JarInfo.DependencyType.valueOf(
                            declaredType.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    dependencyType = Forge1201JarInfo.DependencyType.UNKNOWN;
                }
            }
            dependencies.add(new Forge1201JarInfo.Dependency(
                    dependencyId,
                    values.getOrDefault("versionRange", "*"),
                    dependencyType,
                    values.getOrDefault("ordering", "NONE"),
                    values.getOrDefault("side", "BOTH")));
        }
        return List.copyOf(dependencies);
    }

    private static String dependencyRange(List<TomlSection> sections, String dependencyId) {
        for (TomlSection section : sections) {
            if (!section.name().startsWith("dependencies.")) continue;
            Map<String, String> values = section.values();
            if (!dependencyId.equalsIgnoreCase(values.get("modId"))) continue;
            if ("false".equalsIgnoreCase(values.get("mandatory"))) continue;
            String range = values.get("versionRange");
            if (range != null) return range;
        }
        return null;
    }

    private static boolean hasRangeLowerBound(String range, String expected) {
        return expected.equals(rangeLowerBound(range));
    }

    private static boolean hasMajorRangeLowerBound(String range, String expectedMajor) {
        String lowerBound = rangeLowerBound(range);
        return lowerBound != null && (lowerBound.equals(expectedMajor) || lowerBound.startsWith(expectedMajor + "."));
    }

    private static String rangeLowerBound(String range) {
        if (range == null || range.isBlank()) return null;
        String value = range.trim();
        if (value.startsWith("[") || value.startsWith("(")) value = value.substring(1).trim();
        int comma = value.indexOf(',');
        if (comma >= 0) value = value.substring(0, comma).trim();
        else if (value.endsWith("]") || value.endsWith(")")) value = value.substring(0, value.length() - 1).trim();
        return value;
    }

    private record TomlSection(String name, Map<String, String> values) {}

    /**
     * Parses the scalar keys used by Forge's mods.toml. This deliberately is not a general TOML implementation:
     * arrays, inline tables and multiline values are ignored, while quoted strings, comments and repeated tables
     * (notably [[mods]] and [[dependencies.*]]) are handled.
     */
    private static List<TomlSection> parseTomlSections(String toml) {
        List<TomlSection> sections = new ArrayList<>();
        String currentName = "";
        Map<String, String> currentValues = new HashMap<>();
        for (String raw : toml.split("\\R")) {
            String line = stripTomlComment(raw).trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("[") && line.endsWith("]")) {
                sections.add(new TomlSection(currentName, Map.copyOf(currentValues)));
                boolean arrayTable = line.startsWith("[[") && line.endsWith("]]");
                int start = arrayTable ? 2 : 1;
                int end = line.length() - (arrayTable ? 2 : 1);
                currentName = line.substring(start, end).trim();
                currentValues = new HashMap<>();
                continue;
            }

            int equals = findTomlEquals(line);
            if (equals <= 0) continue;
            String key = line.substring(0, equals).trim();
            String value = parseTomlString(line.substring(equals + 1).trim());
            if (value != null) currentValues.put(key, value);
        }
        sections.add(new TomlSection(currentName, Map.copyOf(currentValues)));
        return sections;
    }

    private static String stripTomlComment(String line) {
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if ((ch == '"' || ch == '\'') && (quote == 0 || quote == ch)) {
                quote = quote == 0 ? ch : 0;
            } else if (ch == '#' && quote == 0) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static int findTomlEquals(String line) {
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if ((ch == '"' || ch == '\'') && (quote == 0 || quote == ch)) {
                quote = quote == 0 ? ch : 0;
            } else if (ch == '=' && quote == 0) {
                return i;
            }
        }
        return -1;
    }

    private static String parseTomlString(String value) {
        if (value.length() < 2) return value;
        char quote = value.charAt(0);
        if (quote != '"' && quote != '\'') return value;
        if (value.charAt(value.length() - 1) != quote) return null;
        String body = value.substring(1, value.length() - 1);
        if (quote == '\'') return body;

        StringBuilder out = new StringBuilder(body.length());
        boolean escaped = false;
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (!escaped && ch == '\\') {
                escaped = true;
                continue;
            }
            if (escaped) {
                out.append(switch (ch) {
                    case 'b' -> '\b';
                    case 't' -> '\t';
                    case 'n' -> '\n';
                    case 'f' -> '\f';
                    case 'r' -> '\r';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> ch;
                });
                escaped = false;
            } else {
                out.append(ch);
            }
        }
        if (escaped) out.append('\\');
        return out.toString();
    }
}
