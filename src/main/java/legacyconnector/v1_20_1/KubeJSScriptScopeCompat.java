package legacyconnector.v1_20_1;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Narrow source-level fixes for Alien Evolution 1.1.x addon scripts whose
 * top-level const names collide with names already present in KubeJS 7's
 * shared script scope.
 *
 * <p>Each rewrite is gated by namespace, exact resource path, and exact
 * declaration/use counts. Unknown script revisions are left unchanged.</p>
 */
public final class KubeJSScriptScopeCompat {
    private static final String CODEX_COMPONENT_BINDING =
            "const Component = Java.loadClass(\"net.minecraft.network.chat.Component\");";
    private static final String DISPLAY_PLAYER_QUATERNION_BINDING =
            "const Quaternionf = Java.loadClass('org.joml.Quaternionf');";

    private KubeJSScriptScopeCompat() {
    }

    /**
     * Renames only the two known AlienEvo top-level Java aliases that collide
     * with names already present in the KubeJS 7 shared script scope. The
     * fully-qualified Java class name inside each string literal is preserved.
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
            return replaceKnownPair(
                    source,
                    script,
                    CODEX_COMPONENT_BINDING,
                    "Component.translatable(",
                    "Component",
                    "AlienEvoComponent",
                    "AlienEvoComponent.translatable(",
                    3);
        }

        if ("kubejs_scripts/display_player.js".equals(resourcePath)) {
            return replaceKnownPair(
                    source,
                    script,
                    DISPLAY_PLAYER_QUATERNION_BINDING,
                    "new Quaternionf(",
                    "Quaternionf",
                    "AlienEvoQuaternionf",
                    "new AlienEvoQuaternionf(",
                    3);
        }

        return source;
    }

    private static byte[] replaceKnownPair(
            byte[] original,
            String script,
            String expectedBinding,
            String expectedUse,
            String oldName,
            String newName,
            String replacementUse,
            int expectedNameOccurrences) {
        if (countLines(script, expectedBinding) != 1
                || count(script, expectedUse) != 1
                || count(script, oldName) != expectedNameOccurrences) {
            return original;
        }
        String rewritten = script
                .replace("const " + oldName + " =", "const " + newName + " =")
                .replace(expectedUse, replacementUse);
        return rewritten.equals(script) ? original : rewritten.getBytes(StandardCharsets.UTF_8);
    }

    private static int count(String text, String needle) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
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
