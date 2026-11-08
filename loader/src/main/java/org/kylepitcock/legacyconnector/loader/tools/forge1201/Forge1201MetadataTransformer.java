package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Converts the platform-specific parts of a Forge 47.x {@code mods.toml} into a
 * NeoForge 21.1 {@code neoforge.mods.toml}.
 *
 * <p>This class is deliberately a pure metadata transformation. It does not rewrite
 * bytecode, write a JAR, or enable Forge 1.20.1 discovery. The locator must continue
 * to safely skip the original input until the complete translation pipeline is ready.</p>
 */
public final class Forge1201MetadataTransformer {
    private static final String JAVA_FML = "javafml";

    private Forge1201MetadataTransformer() {}

    /**
     * Rewrites the root loader version, platform dependency IDs/ranges, and Forge's
     * {@code mandatory} dependency flag into NeoForge's {@code type} property.
     * Mod declarations, unrelated dependency IDs/ranges, ordering, sides, comments,
     * optionality, and license text are preserved semantically.
     *
     * @param forgeToml source {@code META-INF/mods.toml} text
     * @param neoForgeLoaderRange host JavaFML loader range, e.g. {@code [1,)}
     * @param neoForgeVersionRange host NeoForge range, e.g. {@code [21.1.258,)}
     * @param minecraftVersionRange host Minecraft range, e.g. {@code [1.21.1,1.22)}
     * @return translated TOML text for {@code META-INF/neoforge.mods.toml}
     * @throws IllegalArgumentException when the input is not a supported Forge JavaFML
     *                                  metadata file or is missing a required platform dependency
     */
    public static String transformToml(
            String forgeToml,
            String neoForgeLoaderRange,
            String neoForgeVersionRange,
            String minecraftVersionRange) {
        Objects.requireNonNull(forgeToml, "forgeToml");
        requireRange(neoForgeLoaderRange, "neoForgeLoaderRange");
        requireRange(neoForgeVersionRange, "neoForgeVersionRange");
        requireRange(minecraftVersionRange, "minecraftVersionRange");

        String[] lines = forgeToml.split("\\R", -1);
        List<Section> sections = findSections(lines);
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("Forge metadata is empty");
        }

        int loaderVersionRewrites = 0;
        boolean foundJavaFml = false;
        boolean foundForgeDependency = false;
        boolean foundMinecraftDependency = false;

        for (Section section : sections) {
            if (section.name().isEmpty()) {
                for (int line = section.start(); line < section.end(); line++) {
                    Assignment assignment = assignment(lines[line]);
                    if (assignment == null) continue;
                    if (assignment.key().equals("modLoader")) {
                        foundJavaFml = JAVA_FML.equalsIgnoreCase(assignment.value());
                    } else if (assignment.key().equals("loaderVersion")) {
                        lines[line] = replaceValue(lines[line], assignment, neoForgeLoaderRange);
                        loaderVersionRewrites++;
                    }
                }
                continue;
            }

            if (!section.name().startsWith("dependencies.")) continue;

            String dependencyId = null;
            for (int line = section.start() + 1; line < section.end(); line++) {
                Assignment assignment = assignment(lines[line]);
                if (assignment != null && assignment.key().equals("modId")) {
                    dependencyId = assignment.value();
                    break;
                }
            }

            boolean mandatory = isMandatoryDependency(lines, section);
            rewriteDependencyType(lines, section, mandatory);

            if ("forge".equalsIgnoreCase(dependencyId)) {
                rewriteDependency(lines, section, "forge", "neoforge", neoForgeVersionRange);
                foundForgeDependency |= mandatory;
            } else if ("minecraft".equalsIgnoreCase(dependencyId) && mandatory) {
                rewriteDependency(lines, section, "minecraft", "minecraft", minecraftVersionRange);
                foundMinecraftDependency = true;
            }
        }

        if (!foundJavaFml) {
            throw new IllegalArgumentException("Source metadata does not declare modLoader=javafml");
        }
        if (loaderVersionRewrites != 1) {
            throw new IllegalArgumentException(
                    "Expected exactly one root loaderVersion assignment, found " + loaderVersionRewrites);
        }
        if (!foundForgeDependency) {
            throw new IllegalArgumentException("Source metadata has no mandatory Forge dependency section");
        }
        if (!foundMinecraftDependency) {
            throw new IllegalArgumentException("Source metadata has no mandatory Minecraft dependency section");
        }

        return String.join("\n", lines);
    }

    private static boolean isMandatoryDependency(String[] lines, Section section) {
        for (int line = section.start() + 1; line < section.end(); line++) {
            Assignment assignment = assignment(lines[line]);
            if (assignment != null && assignment.key().equals("mandatory")) {
                return !"false".equalsIgnoreCase(assignment.value());
            }
        }
        return true;
    }

    /**
     * Forge 1.20.1 expresses dependency optionality through {@code mandatory};
     * NeoForge 1.21.1 expresses it through {@code type}.
     */
    private static void rewriteDependencyType(String[] lines, Section section, boolean mandatory) {
        boolean foundMandatory = false;
        boolean foundType = false;
        for (int line = section.start() + 1; line < section.end(); line++) {
            Assignment assignment = assignment(lines[line]);
            if (assignment == null) continue;

            if (assignment.key().equals("mandatory")) {
                if (foundMandatory || foundType) {
                    throw new IllegalArgumentException(
                            "Dependency section " + section.name() + " has duplicate or conflicting optionality fields");
                }
                lines[line] = replaceAssignment(lines[line], assignment, "type",
                        mandatory ? "required" : "optional");
                foundMandatory = true;
            } else if (assignment.key().equals("type")) {
                if (foundMandatory || foundType) {
                    throw new IllegalArgumentException(
                            "Dependency section " + section.name() + " has duplicate or conflicting optionality fields");
                }
                foundType = true;
                String expected = mandatory ? "required" : "optional";
                if (!assignment.value().equalsIgnoreCase(expected)) {
                    throw new IllegalArgumentException(
                            "Conflicting dependency type in section " + section.name());
                }
            }
        }
        // NeoForge defaults an omitted dependency type to "required". That matches
        // Forge's default mandatory=true when the source omits the mandatory key.
    }

    private static String replaceAssignment(
            String line, Assignment assignment, String key, String value) {
        int equals = line.indexOf('=');
        if (equals < 0) throw new IllegalArgumentException("Invalid TOML assignment: " + line);
        int keyStart = 0;
        while (keyStart < equals && Character.isWhitespace(line.charAt(keyStart))) keyStart++;
        int keyEnd = equals;
        while (keyEnd > keyStart && Character.isWhitespace(line.charAt(keyEnd - 1))) keyEnd--;
        return line.substring(0, keyStart) + key + line.substring(keyEnd, assignment.valueStart())
                + quote(value) + line.substring(assignment.valueEnd());
    }

    private static void rewriteDependency(
            String[] lines, Section section, String expectedId, String replacementId, String replacementRange) {
        boolean foundId = false;
        boolean foundRange = false;

        for (int line = section.start() + 1; line < section.end(); line++) {
            Assignment assignment = assignment(lines[line]);
            if (assignment == null) continue;

            if (assignment.key().equals("modId")) {
                if (!expectedId.equalsIgnoreCase(assignment.value())) {
                    throw new IllegalArgumentException(
                            "Dependency section " + section.name() + " changed while being transformed");
                }
                lines[line] = replaceValue(lines[line], assignment, replacementId);
                foundId = true;
            } else if (assignment.key().equals("versionRange")) {
                lines[line] = replaceValue(lines[line], assignment, replacementRange);
                foundRange = true;
            }
        }

        if (!foundId || !foundRange) {
            throw new IllegalArgumentException(
                    "Dependency section " + section.name() + " must declare modId and versionRange");
        }
    }

    private static List<Section> findSections(String[] lines) {
        List<Section> sections = new ArrayList<>();
        String currentName = "";
        int currentStart = 0;

        for (int line = 0; line < lines.length; line++) {
            String header = stripComment(lines[line]).trim();
            if (!isTableHeader(header)) continue;

            sections.add(new Section(currentName, currentStart, line));
            currentName = tableName(header);
            currentStart = line;
        }

        sections.add(new Section(currentName, currentStart, lines.length));
        return sections;
    }

    private static boolean isTableHeader(String line) {
        return line.length() >= 2 && line.charAt(0) == '[' && line.charAt(line.length() - 1) == ']';
    }

    private static String tableName(String header) {
        boolean arrayTable = header.startsWith("[[") && header.endsWith("]]");
        int start = arrayTable ? 2 : 1;
        int end = header.length() - (arrayTable ? 2 : 1);
        return header.substring(start, end).trim();
    }

    private static Assignment assignment(String line) {
        int commentStart = commentStart(line);
        int limit = commentStart < 0 ? line.length() : commentStart;
        int equals = equalsOutsideQuotes(line, limit);
        if (equals < 0) return null;

        String key = line.substring(0, equals).trim();
        if (key.isEmpty()) return null;

        int valueStart = equals + 1;
        while (valueStart < limit && Character.isWhitespace(line.charAt(valueStart))) valueStart++;

        int valueEnd = limit;
        while (valueEnd > valueStart && Character.isWhitespace(line.charAt(valueEnd - 1))) valueEnd--;

        String rawValue = line.substring(valueStart, valueEnd);
        String value = unquote(rawValue);
        return new Assignment(key, value, valueStart, valueEnd);
    }

    private static int equalsOutsideQuotes(String line, int limit) {
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < limit; i++) {
            char ch = line.charAt(i);
            if (quote == '"' && !escaped && ch == '\\') {
                escaped = true;
                continue;
            }
            if (quote != 0) {
                if (!escaped && ch == quote) quote = 0;
                escaped = false;
            } else if (ch == '"' || ch == '\'') {
                quote = ch;
            } else if (ch == '=') {
                return i;
            }
        }
        return -1;
    }

    private static int commentStart(String line) {
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quote == '"' && !escaped && ch == '\\') {
                escaped = true;
                continue;
            }
            if (quote != 0) {
                if (!escaped && ch == quote) quote = 0;
                escaped = false;
            } else if (ch == '"' || ch == '\'') {
                quote = ch;
            } else if (ch == '#') {
                return i;
            }
        }
        return -1;
    }

    private static String stripComment(String line) {
        int comment = commentStart(line);
        return comment < 0 ? line : line.substring(0, comment);
    }

    private static String unquote(String rawValue) {
        if (rawValue.length() < 2) return rawValue;
        char quote = rawValue.charAt(0);
        if ((quote == '"' || quote == '\'') && rawValue.charAt(rawValue.length() - 1) == quote) {
            return rawValue.substring(1, rawValue.length() - 1);
        }
        return rawValue;
    }

    private static String replaceValue(String line, Assignment assignment, String value) {
        return line.substring(0, assignment.valueStart())
                + quote(value)
                + line.substring(assignment.valueEnd());
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void requireRange(String value, String label) {
        if (value == null || value.isBlank() || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(label + " must be a non-empty single-line version range");
        }
    }

    private record Section(String name, int start, int end) {}
    private record Assignment(String key, String value, int valueStart, int valueEnd) {}
}
