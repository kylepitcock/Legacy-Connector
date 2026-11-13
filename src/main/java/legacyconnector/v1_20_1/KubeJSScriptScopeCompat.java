package legacyconnector.v1_20_1;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Narrow source-level fixes for Alien Evolution 1.1.x addon scripts whose
 * legacy KubeJS 6 assumptions are incompatible with KubeJS 7 on Minecraft 1.21.1.
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
     * Applies only inspected AlienEvo script rewrites. KubeJS 7 deliberately
     * exposes a read-only global map to client/server scripts, renames the
     * EntityEvents hurt handlers, rejects '*' as a registry ID, and has a
     * top-level Vec3f binding that conflicts with AlienEvo's alias.
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
        String rewritten = script;

        if ("kubejs_scripts/alien_codex.js".equals(resourcePath)) {
            rewritten = replaceKnownPair(
                    rewritten,
                    CODEX_COMPONENT_BINDING,
                    "Component.translatable(",
                    "Component",
                    "AlienEvoComponent",
                    "AlienEvoComponent.translatable(",
                    3);
            rewritten = addStartupDisplayNameHelper(rewritten);
        } else if ("kubejs_scripts/display_player.js".equals(resourcePath)) {
            rewritten = replaceKnownPair(
                    rewritten,
                    DISPLAY_PLAYER_QUATERNION_BINDING,
                    "new Quaternionf(",
                    "Quaternionf",
                    "AlienEvoQuaternionf",
                    "new AlienEvoQuaternionf(",
                    3);
        } else if ("kubejs_scripts/failsafe.js".equals(resourcePath)) {
            rewritten = rewriteFailsafeGlobals(rewritten);
        } else if ("kubejs_scripts/level_command.js".equals(resourcePath)) {
            rewritten = rewriteLevelCommandGlobals(rewritten);
        } else if ("kubejs_scripts/fall.js".equals(resourcePath)) {
            rewritten = rewriteHurtEvent(rewritten, 4);
        } else if ("kubejs_scripts/hit.js".equals(resourcePath)) {
            rewritten = rewriteHurtEvent(rewritten, 8);
        } else if ("kubejs_scripts/workbench.js".equals(resourcePath)) {
            rewritten = rewriteUnfilteredEntityInteraction(rewritten);
        } else if ("kubejs_scripts/slam.js".equals(resourcePath)) {
            rewritten = rewriteVectorAlias(rewritten);
        } else if ("kubejs_scripts/code_hints.js".equals(resourcePath)) {
            rewritten = rewriteCodeHintsGlobals(rewritten);
        }

        return rewritten.equals(script) ? source : rewritten.getBytes(StandardCharsets.UTF_8);
    }

    private static String replaceKnownPair(
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
            return script;
        }
        return script
                .replace("const " + oldName + " =", "const " + newName + " =")
                .replace(expectedUse, replacementUse);
    }

 
    private static String addStartupDisplayNameHelper(String script) {
        String anchor = "global.getAlienDescription = function(id) {";
        if (count(script, anchor) != 1 || script.contains("global.getAlienDisplayName")) {
            return script;
        }

        // The addon script is loaded during KubeJS STARTUP, where the global
        // map is intentionally mutable. Publish the helper there so client
        // and server scripts can safely read it from their read-only view.
        return script + """

                global.getAlienDisplayName = (player, alienNumber) => {
                    if (player && player.persistentData.contains(`alienevo_${alienNumber}`)) {
                        return player.persistentData.getString(`alienevo_${alienNumber}`);
                    }

                    const key = `alienevo_alien_${alienNumber}`;
                    const alienInfo = global[key];
                    if (alienInfo && alienInfo[0]) {
                        const fullName = String(alienInfo[0]);
                        const alienPath = fullName.includes(':') ? fullName.split(':')[1] : fullName;
                        return alienPath.charAt(0).toUpperCase() + alienPath.slice(1);
                    }

                    return `Alien #${alienNumber}`;
                };
                """;
    }

    private static String rewriteFailsafeGlobals(String script) {
        String soundDeclaration =
                "global.sound = (entity, soundId, volume, pitch, shift) => {";
        String knockbackDeclaration =
                "global.applyKnockback = (entity, target, strength) => {";
        if (countLines(script, soundDeclaration) != 1
                || countLines(script, knockbackDeclaration) != 1
                || count(script, "global.sound(") != 2
                || count(script, "global.applyKnockback(") != 1) {
            return script;
        }

        return script
                .replace(soundDeclaration,
                        "function alienevoSound(entity, soundId, volume, pitch, shift) {")
                .replace(knockbackDeclaration,
                        "function alienevoApplyKnockback(entity, target, strength) {")
                .replace("global.sound(", "alienevoSound(")
                .replace("global.applyKnockback(", "alienevoApplyKnockback(");
    }

    private static String rewriteLevelCommandGlobals(String script) {
        String defaultNameWrite = "global[defaultNameKey] = defaultName;";
        String displayNameWrite = "global.getAlienDisplayName = (player, alienNumber) => {";
        if (count(script, defaultNameWrite) != 1 || count(script, displayNameWrite) != 1) {
            return script;
        }

        // The helper is only referenced inside this script in the inspected
        // AlienEvo release. Keep it local; its fallback derives the same name
        // from the read-only alien registry if no per-world override exists.
        return script
                .replace(defaultNameWrite,
                        "// KubeJS 7 global is read-only here; the display-name helper computes this fallback.")
                .replace(displayNameWrite,
                        "const getAlienDisplayName = (player, alienNumber) => {");
    }

    private static String rewriteHurtEvent(String script, int expectedCalls) {
        String oldEvent = "EntityEvents.hurt(";
        if (count(script, oldEvent) != expectedCalls) {
            return script;
        }
        return script.replace(oldEvent, "EntityEvents.beforeHurt(");
    }

    private static String rewriteUnfilteredEntityInteraction(String script) {
        String oldRegistration = "ItemEvents.entityInteracted('*', event => {";
        if (count(script, oldRegistration) != 1) {
            return script;
        }
        // KubeJS 7's targeted handler accepts one callback for all target IDs;
        // '*' is parsed as a literal ResourceLocation and is invalid.
        return script.replace(oldRegistration, "ItemEvents.entityInteracted(event => {");
    }

 
    private static String rewriteCodeHintsGlobals(String script) {
        if (count(script, "global.usedPositions") != 5
                || count(script, "global.usedSelfDestructPositions") != 5
                || count(script, "if (!global.usedPositions)") != 1
                || count(script, "if (!global.usedSelfDestructPositions)") != 1) {
            return script;
        }

        // These sets are used only by code_hints.js handlers. Keep their
        // lifetime at the script scope instead of writing to KubeJS 7's
        // immutable global map from a SERVER script.
        return "let alienevoUsedPositions = new Set();\n"
                + "let alienevoUsedSelfDestructPositions = new Set();\n"
                + script.replace("global.usedPositions", "alienevoUsedPositions")
                        .replace("global.usedSelfDestructPositions", "alienevoUsedSelfDestructPositions");
    }

    private static String rewriteVectorAlias(String script) {
        String oldDeclaration = "const Vec3f = Java.loadClass('org.joml.Vector3f');";
        if (countLines(script, oldDeclaration) != 1 || count(script, "new Vec3f(") != 1) {
            return script;
        }
        return script
                .replace(oldDeclaration,
                        "const AlienEvoVector3f = Java.loadClass('org.joml.Vector3f');")
                .replace("new Vec3f(", "new AlienEvoVector3f(");
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
