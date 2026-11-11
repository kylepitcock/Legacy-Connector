package legacyconnector.v1_20_1;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Narrow source-level fixes for Alien Evolution 1.1.x addon scripts whose
 * top-level const names collide with names already present in KubeJS 7's
 * shared script scope.
 *
 * <p>Each rewrite is gated by namespace, exact resource path, expected class
 * binding, and identifier occurrence count. Unknown script revisions are
 * left unchanged.</p>
 */
public final class KubeJSScriptScopeCompat {
    private static final Pattern COMPONENT_IDENTIFIER =
            Pattern.compile("(?<![A-Za-z0-9_$])Component(?![A-Za-z0-9_$])");
    private static final Pattern QUATERNIONF_IDENTIFIER =
            Pattern.compile("(?<![A-Za-z0-9_$])Quaternionf(?![A-Za-z0-9_$])");

    private static final String CODEX_COMPONENT_BINDING =
            "const Component = Java.loadClass(\"net.minecraft.network.chat.Component\");";
    private static final String DISPLAY_PLAYER_QUATERNION_BINDING =
            "const Quaternionf = Java.loadClass('org.joml.Quaternionf');";

    private KubeJSScriptScopeCompat() {
    }

    /**
     * Renames only the two known AlienEvo top-level Java aliases that collide
     * with names in the KubeJS 7 shared script scope.
     */
    public static byte[] rewriteKnownIdentifierCollisions(
            String namespace,
            String resourcePath,
            byte[] source) {
        Objects.requireNonNull(namespace, "addon pack namespace");
        Objects.requireNonNull(resourcePath, "addon script resource path");
        Objects.requireNonNull(source, "addon script source");

        if (!"alienevo".equals(namespace)) {
            return source;
        }

        String script = new String(source, StandardCharsets.UTF_8);
        if ("kubejs_scripts/alien_codex.js".equals(resourcePath)) {
            return renameExactBinding(
                    source,
                    script,
                    CODEX_COMPONENT_BINDING,
                    COMPONENT_IDENTIFIER,
                    "Component",
                    "AlienEvoComponent",
                    3);
        }

        if ("kubejs_scripts/display_player.js".equals(resourcePath)) {
            return renameExactBinding(
                    source,
                    script,
                    DISPLAY_PLAYER_QUATERNION_BINDING,
                    QUATERNIONF_IDENTIFIER,
                    "Quaternionf",
                    "AlienEvoQuaternionf",
                    3);
        }

        return source;
    }

    private static byte[] renameExactBinding(
            byte[] original,
            String script,
            String expectedBinding,
            Pattern identifier,
            String oldName,
            String newName,
            int expectedOccurrences) {
        if (count(script, identifier) != expectedOccurrences
                || countLines(script, expectedBinding) != 1) {
            return original;
        }

        String rewritten = identifier.matcher(script).replaceAll(newName);
        return rewritten.equals(script) ? original : rewritten.getBytes(StandardCharsets.UTF_8);
    }

    private static int count(String text, Pattern pattern) {
        int count = 0;
        var matcher = pattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static int countLines(String text, String expectedLine) {
        int count = 0;
        for (String line : text.split("\\R", -1)) {
            if (line.strip().equals(expectedLine)) {
                count++;
            }
        }
        return count;
    }
}
