package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Compares dependencies declared by a Forge 1.20.1 jar with the ids of NeoForge 1.21.1 jars
 * already present in the user's mods directories.
 *
 * <p>An id match is only a candidate for native substitution. It does not prove that the installed
 * mod version exposes the API expected by the old dependency.</p>
 */
public final class Forge1201DependencyResolver {
    private static final Pattern MOD_ID = Pattern.compile("(?m)^\\s*modId\\s*=\\s*\"([a-zA-Z0-9_.-]+)\"");

    private Forge1201DependencyResolver() {}

    public enum Status {
        HOST_BRIDGE,
        NATIVE_ID_PRESENT,
        REQUIRED_MISSING,
        OPTIONAL_MISSING
    }

    public record Resolution(String modId, String versionRange, boolean mandatory, Status status) {}

    /**
     * Reads only META-INF/neoforge.mods.toml. Legacy Forge jars and other loader formats are not
     * counted as native NeoForge replacements.
     */
    public static Set<String> findNativeModIds(Collection<Path> modDirectories) {
        Set<String> ids = new TreeSet<>();
        for (Path directory : modDirectories) {
            if (!Files.isDirectory(directory)) continue;
            try (var files = Files.list(directory)) {
                for (Path jar : files
                        .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar"))
                        .toList()) {
                    collectNativeModIds(jar, ids);
                }
            } catch (IOException | RuntimeException ignored) {
                // A missing or temporarily unreadable mods directory must not block legacy discovery.
            }
        }
        return Set.copyOf(ids);
    }

    private static void collectNativeModIds(Path jar, Set<String> ids) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry metadata = zip.getEntry("META-INF/neoforge.mods.toml");
            if (metadata == null) return;
            String toml;
            try (var in = zip.getInputStream(metadata)) {
                toml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            String section = "";
            for (String line : stripTomlComments(toml).split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    boolean arrayTable = trimmed.startsWith("[[") && trimmed.endsWith("]]");
                    int start = arrayTable ? 2 : 1;
                    int end = trimmed.length() - (arrayTable ? 2 : 1);
                    section = trimmed.substring(start, end).trim();
                    continue;
                }
                if (!section.equals("mods")) continue;
                Matcher matcher = MOD_ID.matcher(line);
                if (matcher.find()) ids.add(matcher.group(1).toLowerCase(java.util.Locale.ROOT));
            }
        } catch (IOException | RuntimeException ignored) {
            // Invalid or unrelated archives are not native mod candidates.
        }
    }

    private static String stripTomlComments(String toml) {
        StringBuilder clean = new StringBuilder(toml.length());
        for (String line : toml.split("\\R", -1)) {
            char quote = 0;
            int end = line.length();
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if ((ch == '"' || ch == '\'') && (quote == 0 || quote == ch)) {
                    quote = quote == 0 ? ch : 0;
                } else if (ch == '#' && quote == 0) {
                    end = i;
                    break;
                }
            }
            clean.append(line, 0, end).append('\n');
        }
        return clean.toString();
    }

    public static java.util.List<Resolution> resolve(
            Forge1201JarInfo jar, Collection<String> nativeModIds) {
        Set<String> nativeIds = new TreeSet<>();
        for (String id : nativeModIds) nativeIds.add(id.toLowerCase(java.util.Locale.ROOT));

        java.util.List<Resolution> result = new java.util.ArrayList<>();
        for (Forge1201JarInfo.Mod mod : jar.mods()) {
            for (Forge1201JarInfo.Dependency dependency : mod.dependencies()) {
                String id = dependency.modId().toLowerCase(java.util.Locale.ROOT);
                Status status;
                if (id.equals("minecraft") || id.equals("forge") || id.equals("fml")) {
                    status = Status.HOST_BRIDGE;
                } else if (nativeIds.contains(id)) {
                    status = Status.NATIVE_ID_PRESENT;
                } else {
                    status = dependency.mandatory() ? Status.REQUIRED_MISSING : Status.OPTIONAL_MISSING;
                }
                result.add(new Resolution(id, dependency.versionRange(), dependency.mandatory(), status));
            }
        }
        return java.util.List.copyOf(result);
    }

    public static String summarize(Forge1201JarInfo jar, Collection<String> nativeModIds) {
        return resolve(jar, nativeModIds).stream()
                .map(resolution -> resolution.modId() + "=" + switch (resolution.status()) {
                    case HOST_BRIDGE -> "host API bridge required";
                    case NATIVE_ID_PRESENT -> "native mod ID present; API/version not yet verified";
                    case REQUIRED_MISSING -> "required dependency not found";
                    case OPTIONAL_MISSING -> "optional dependency not found";
                })
                .reduce((a, b) -> a + "; " + b)
                .orElse("no dependencies declared");
    }
}
