package org.kylepitcock.legacyconnector.loader.transform;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Narrow resource-level compatibility rewrites for AlienEvo on Minecraft 1.21.1:
 * empty armor ItemStack guards, Omnitrix persistent-integer normalization, the
 * legacy ResourceLocation script constructor, typed SoundEvent playback, and
 * ResourceLocation/string normalization for the Omnitrix customization tab.
 */
final class PalladiumResourceCompat1201 {
    static final String ALIEN_EVO_STORE_ARMOR =
            "addon/alienevo/kubejs_scripts/store_armor.js";
    static final String ALIEN_EVO_SLOT_TRANSFORM =
            "addon/alienevo/kubejs_scripts/slot_transform.js";
    static final String ALIEN_EVO_QUICK_CHANGE =
            "data/alienevo/kubejs_scripts/quick_change.js";
    static final String ALIEN_EVO_TRANSFORM =
            "addon/alienevo/kubejs_scripts/transform.js";
    static final String ALIEN_EVO_POWER_UI =
            "assets/alienevo/kubejs_scripts/power_ui.js";
    static final String ALIEN_EVO_DIAMOND_CREATION =
            "addon/alienevo/kubejs_scripts/diamond_creation.js";

    static final String OMNITRIX_INTEGER_HELPER =
            "function toOmnitrixInteger(value) {\n"
                    + "    const JavaInteger = Java.loadClass('java.lang.Integer');\n"
                    + "    if (value != null && typeof value.getAsInt === 'function') {\n"
                    + "        return JavaInteger.valueOf(value.getAsInt());\n"
                    + "    }\n"
                    + "    return JavaInteger.valueOf(Math.trunc(Number(value)));\n"
                    + "}\n\n";
    static final String LEGACY_RESOURCE_LOCATION_HELPER =
            "function ResourceLocation(namespaceOrLocation, path) {\n"
                    + "    const AlienEvoJavaResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');\n"
                    + "    if (arguments.length > 1) {\n"
                    + "        return AlienEvoJavaResourceLocation.fromNamespaceAndPath(String(namespaceOrLocation), String(path));\n"
                    + "    }\n"
                    + "    return AlienEvoJavaResourceLocation.parse(String(namespaceOrLocation));\n"
                    + "}\n\n";
    static final String LEGACY_SOUND_EVENT_HELPER =
            "function getAlienEvoSoundEvent(soundId) {\n"
                    + "    const AlienEvoSoundRegistry = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries').SOUND_EVENT;\n"
                    + "    const AlienEvoSoundResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');\n"
                    + "    const soundEvent = AlienEvoSoundRegistry.get(AlienEvoSoundResourceLocation.parse(String(soundId)));\n"
                    + "    if (soundEvent == null) {\n"
                    + "        throw new Error('AlienEvo sound event is not registered: ' + soundId);\n"
                    + "    }\n"
                    + "    return soundEvent;\n"
                    + "}\n\n";
    private static final String ALIEN_EVO_CYCLE = "addon/alienevo/kubejs_scripts/omnitrix_cycle.js";
    private static final String ALIEN_EVO_DETRANSFORM = "addon/alienevo/kubejs_scripts/detransform.js";
    private static final String ALIEN_EVO_FLATTEN = "addon/alienevo/kubejs_scripts/flatten.js";
    private static final String ALIEN_EVO_DECOUPLE = "addon/alienevo/kubejs_scripts/decouple.js";
    private static final String RESOURCE_LOCATION_HELPER_MARKER =
            "function ResourceLocation(namespaceOrLocation, path) {";
    private static final String SOUND_EVENT_HELPER_MARKER =
            "function getAlienEvoSoundEvent(soundId) {";
    private static final String LEGACY_PEHKUI_SCALE_TYPES_BINDING =
            "const SCALE_TYPES = Java.loadClass(\"virtuoel.pehkui.api.ScaleTypes\");";
    private static final String OPTIONAL_PEHKUI_SCALE_TYPES_BINDING = """
            const SCALE_TYPES = (() => {
                try {
                    return Java.loadClass("virtuoel.pehkui.api.ScaleTypes");
                } catch (ignored) {
                    const noScale = { getScaleData: entity => ({ setScale: value => {} }) };
                    return { HEIGHT: noScale, WIDTH: noScale, STEP_HEIGHT: noScale, JUMP_HEIGHT: noScale };
                }
            })();""";


    static final String LEGACY_STORE_ARMOR_CHECK =
            "if (entity[slot + 'ArmorItem']) {";
    static final String SAFE_STORE_ARMOR_CHECK =
            "if (entity[slot + 'ArmorItem'] && !entity[slot + 'ArmorItem'].isEmpty()) {";

    static final String LEGACY_ARMOR_ID_RETURN =
            "return armorItem && armorItem.id !== 'minecraft:air';";
    static final String SAFE_ARMOR_ID_RETURN =
            "return armorItem && !armorItem.isEmpty();";
    static final String LEGACY_ARMOR_ID_CHECK =
            "if (armorItem && armorItem.id !== 'minecraft:air') {";
    static final String SAFE_ARMOR_ID_CHECK =
            "if (armorItem && !armorItem.isEmpty()) {";

    private static final String[][] POWER_UI_TAB_COMPARISONS = {
            {
                    "currentTab === omnitrixTab",
                    "currentTab.toString() === omnitrixTab",
                    "3"
            },
            {
                    "e.tab === omnitrixTab",
                    "e.tab.toString() === omnitrixTab",
                    "5"
            }
    };

    private static final String[][] OMNITRIX_INTEGER_WRITES = {
            {
                    "palladium.setProperty(entity, omnitrixPropertyKey, newAlienUnlocked);",
                    "palladium.setProperty(entity, omnitrixPropertyKey, toOmnitrixInteger(newAlienUnlocked));",
                    "2"
            },
            {
                    "palladium.setProperty(entity, omnitrixPropertyKey, selectedAlienNum);",
                    "palladium.setProperty(entity, omnitrixPropertyKey, toOmnitrixInteger(selectedAlienNum));",
                    "2"
            },
            {
                    "palladium.setProperty(entity, omnitrixPropertyKey, alienNumber);",
                    "palladium.setProperty(entity, omnitrixPropertyKey, toOmnitrixInteger(alienNumber));",
                    "1"
            },
            {
                    "palladium.setProperty(entity, omnitrixPropertyKey, alien_num);",
                    "palladium.setProperty(entity, omnitrixPropertyKey, toOmnitrixInteger(alien_num));",
                    "1"
            }
    };

    private PalladiumResourceCompat1201() {}

    /**
     * Apply narrowly scoped rewrites to the inspected AlienEvo resources.
     *
     * Missing or already migrated resources are no-ops. Every expected legacy
     * occurrence is counted exactly; unknown script shapes fail loudly rather
     * than leaving a partially or incorrectly rewritten callback.
     *
     * @return number of resource scripts changed
     */
    static int apply(Map<String, byte[]> resources) {
        Map<String, byte[]> original = new java.util.HashMap<>();
        resources.forEach((path, contents) -> original.put(path, contents.clone()));

        rewrite(
                resources,
                ALIEN_EVO_STORE_ARMOR,
                new String[][] {{LEGACY_STORE_ARMOR_CHECK, SAFE_STORE_ARMOR_CHECK, "1"}});
        rewrite(
                resources,
                ALIEN_EVO_SLOT_TRANSFORM,
                new String[][] {
                        {LEGACY_ARMOR_ID_RETURN, SAFE_ARMOR_ID_RETURN, "1"},
                        {LEGACY_ARMOR_ID_CHECK, SAFE_ARMOR_ID_CHECK, "1"}
                });
        rewrite(
                resources,
                ALIEN_EVO_QUICK_CHANGE,
                new String[][] {
                        {LEGACY_ARMOR_ID_RETURN, SAFE_ARMOR_ID_RETURN, "1"},
                        {LEGACY_ARMOR_ID_CHECK, SAFE_ARMOR_ID_CHECK, "1"}
                });
        rewriteTransformScript(resources);
        rewrite(resources, ALIEN_EVO_POWER_UI, POWER_UI_TAB_COMPARISONS);
        rewritePowerUiEntityPreview(resources);
        rewriteLegacyScriptApis(resources);
        rewriteDiamondCreationSound(resources);
        rewriteScoreboardApi(resources);
        rewriteOptionalPehkuiApi(resources);

        // A single resource can receive more than one independent rewrite.
        // Report the number of distinct changed script files, not rewrite passes.
        int changedResources = 0;
        for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
            byte[] before = original.get(entry.getKey());
            if (before == null || !java.util.Arrays.equals(before, entry.getValue())) {
                changedResources++;
            }
        }
        return changedResources;
    }

    /**
     * AlienEvo 1.1.3's PowersScreen script calls the old seven-argument
     * InventoryScreen.renderEntityInInventory overload. Minecraft 1.21.1 adds
     * a Vector3f translation argument before the two quaternions. Supply a
     * zero translation to preserve the old preview placement.
     */
    private static int rewritePowerUiEntityPreview(Map<String, byte[]> resources) {
        byte[] original = resources.get(ALIEN_EVO_POWER_UI);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        String callMarker = "InventoryScreen.renderEntityInInventory(";
        List<PreviewInsertion> insertions = new java.util.ArrayList<>();
        int searchFrom = 0;

        while (true) {
            int call = script.indexOf(callMarker, searchFrom);
            if (call < 0) break;

            int open = call + "InventoryScreen.renderEntityInInventory".length();
            int close = matchingCallParen(script, open);
            if (close < 0) {
                throw new IllegalStateException(
                        "AlienEvo power_ui.js has an unterminated InventoryScreen.renderEntityInInventory call");
            }

            List<ScriptArgument> arguments = splitScriptArguments(script, open + 1, close);
            if (arguments.size() == 8) {
                String translation = script.substring(
                        arguments.get(4).start(), arguments.get(4).end()).trim();
                if (!translation.contains("Vector3f")) {
                    throw new IllegalStateException(
                            "AlienEvo power_ui.js has an unexpected eight-argument entity preview call");
                }
            } else if (arguments.size() == 7) {
                String cameraOrientation = script.substring(
                        arguments.get(5).start(), arguments.get(5).end()).trim();
                if (!"null".equals(cameraOrientation)) {
                    throw new IllegalStateException(
                            "AlienEvo power_ui.js has an unexpected seven-argument entity preview call");
                }
                // Repeat the original separator around the inserted argument to preserve formatting.
                int argumentStart = arguments.get(4).start();
                int insertion = argumentStart;
                int argumentEnd = arguments.get(4).end();
                while (insertion < argumentEnd && Character.isWhitespace(script.charAt(insertion))) {
                    insertion++;
                }
                String followingSeparator = script.substring(argumentStart, insertion);
                insertions.add(new PreviewInsertion(insertion, followingSeparator));
            } else {
                throw new IllegalStateException(
                        "AlienEvo power_ui.js entity preview call has " + arguments.size()
                                + " arguments; expected the inspected 7-argument legacy or 8-argument migrated form");
            }

            searchFrom = close + 1;
        }

        if (insertions.isEmpty()) return 0;

        String migrated = script;
        for (int i = insertions.size() - 1; i >= 0; i--) {
            PreviewInsertion insertion = insertions.get(i);
            migrated = migrated.substring(0, insertion.offset())
                    + "new (Java.loadClass('org.joml.Vector3f'))(0.0, 0.0, 0.0),"
                    + insertion.followingSeparator()
                    + migrated.substring(insertion.offset());
        }

        resources.put(ALIEN_EVO_POWER_UI, migrated.getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    /**
     * KubeJS 7 exposes numeric persistent-data entries as NBT tags. Palladium's
     * IntegerProperty accepts Number values, not IntTag instances, so normalize
     * the six Omnitrix-cycle writes to java.lang.Integer before calling it.
     */
    private static int rewriteTransformScript(Map<String, byte[]> resources) {
        byte[] original = resources.get(ALIEN_EVO_TRANSFORM);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        boolean helperAdded = false;
        if (!script.startsWith(OMNITRIX_INTEGER_HELPER)) {
            String anchor = "function getRandomInt(min, max) {";
            if (!script.startsWith(anchor)) {
                throw new IllegalStateException(
                        "AlienEvo transform.js has an unexpected prologue; cannot add integer normalization safely");
            }
            script = OMNITRIX_INTEGER_HELPER + script;
            resources.put(ALIEN_EVO_TRANSFORM, script.getBytes(StandardCharsets.UTF_8));
            helperAdded = true;
        }

        int rewritten = rewrite(resources, ALIEN_EVO_TRANSFORM, OMNITRIX_INTEGER_WRITES);
        return helperAdded || rewritten > 0 ? 1 : 0;
    }


    /**
     * Restore the legacy ResourceLocation constructor used by inspected
     * AlienEvo addon/client/data scripts. Also resolve cycle and transformation
     * sound IDs to registered SoundEvent instances before Java overload
     * selection, avoiding KubeJS/Rhino ambiguity between SoundEvent and Holder.
     *
     * @return number of distinct AlienEvo script resources changed
     */
    private static int rewriteLegacyScriptApis(Map<String, byte[]> resources) {
        int changed = 0;
        for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
            String path = entry.getKey();
            if (!isAlienEvoKubeJsScript(path)) continue;

            String script = new String(entry.getValue(), StandardCharsets.UTF_8);
            String migrated = script;

            if (migrated.contains("\"forge:entity_gravity\"")) {
                migrated = replaceExactScriptOccurrence(
                        path, migrated, "\"forge:entity_gravity\"",
                        "\"minecraft:generic.gravity\"");
            }

            if (migrated.contains("new ResourceLocation(")
                    && !migrated.contains(RESOURCE_LOCATION_HELPER_MARKER)) {
                migrated = insertAfterOmnitrixIntegerHelper(migrated, LEGACY_RESOURCE_LOCATION_HELPER);
            }

            if (ALIEN_EVO_CYCLE.equals(path)) {
                migrated = addSoundEventHelper(migrated);
                migrated = replaceExactScriptOccurrence(
                        path,
                        migrated,
                        "`alienevo:${soundToPlay}`",
                        "getAlienEvoSoundEvent('alienevo:' + String(soundToPlay))");
            } else if (ALIEN_EVO_TRANSFORM.equals(path)) {
                migrated = addSoundEventHelper(migrated);
                migrated = replaceExactScriptOccurrence(
                        path,
                        migrated,
                        "`alienevo:${transformSound}_transform`",
                        "getAlienEvoSoundEvent('alienevo:' + String(transformSound) + '_transform')");
            } else if (ALIEN_EVO_DETRANSFORM.equals(path)) {
                migrated = addSoundEventHelper(migrated);
                migrated = replaceExactScriptOccurrence(
                        path,
                        migrated,
                        "`alienevo:${watchType}_detransform`",
                        "getAlienEvoSoundEvent('alienevo:' + String(watchType) + '_detransform')");
            } else if (ALIEN_EVO_DECOUPLE.equals(path)) {
                migrated = addSoundEventHelper(migrated);
                migrated = replaceExactScriptOccurrence(
                        path,
                        migrated,
                        "getDecoupleSoundId(watchPrefix),",
                        "getAlienEvoSoundEvent(getDecoupleSoundId(watchPrefix)),");
            }

            if (!migrated.equals(script)) {
                entry.setValue(migrated.getBytes(StandardCharsets.UTF_8));
                changed++;
            }
        }
        return changed;
    }


    /**
     * AlienEvo 1.1.3's diamond-creation ability still passes its legacy string
     * sound ID to Level.playSound. In 1.21.1 Rhino sees both the Holder and
     * SoundEvent overloads and cannot choose one. Resolve the ID to the
     * registered SoundEvent first, just as the other AlienEvo ability scripts
     * do. This is scoped to the inspected diamond_creation.js resource.
     */
    private static int rewriteDiamondCreationSound(Map<String, byte[]> resources) {
        byte[] original = resources.get(ALIEN_EVO_DIAMOND_CREATION);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        List<ScriptArgument> soundArguments = new java.util.ArrayList<>();
        int searchFrom = 0;
        while (true) {
            int call = script.indexOf(".playSound(", searchFrom);
            if (call < 0) break;
            int open = call + ".playSound".length();
            int close = matchingCallParen(script, open);
            if (close < 0) {
                throw new IllegalStateException(
                        "AlienEvo diamond_creation.js has an unterminated Level.playSound call");
            }

            List<ScriptArgument> arguments = splitScriptArguments(script, open + 1, close);
            if (arguments.size() == 8
                    && "null".equals(script.substring(arguments.get(0).start(), arguments.get(0).end()).trim())) {
                ScriptArgument sound = arguments.get(4);
                String expression = script.substring(sound.start(), sound.end()).trim();
                if (!expression.startsWith("getAlienEvoSoundEvent(")) {
                    soundArguments.add(sound);
                }
            }
            searchFrom = close + 1;
        }

        if (soundArguments.isEmpty()) return 0;

        String migrated = script;
        for (int i = soundArguments.size() - 1; i >= 0; i--) {
            ScriptArgument range = soundArguments.get(i);
            String expression = migrated.substring(range.start(), range.end()).trim();
            migrated = migrated.substring(0, range.start())
                    + "getAlienEvoSoundEvent(" + expression + ")"
                    + migrated.substring(range.end());
        }
        migrated = addSoundEventHelper(migrated);
        resources.put(ALIEN_EVO_DIAMOND_CREATION, migrated.getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    private record ScriptArgument(int start, int end) {}

    private record PreviewInsertion(int offset, String followingSeparator) {}

    /** Finds the closing parenthesis of a JavaScript call while ignoring strings and comments. */
    private static int matchingCallParen(String script, int open) {
        int depth = 1;
        char quote = 0;
        boolean escaped = false;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int i = open + 1; i < script.length(); i++) {
            char c = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : 0;
            if (lineComment) {
                if (c == '\n') lineComment = false;
                continue;
            }
            if (blockComment) {
                if (c == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
                continue;
            }
            if (quote != 0) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
                continue;
            }
            if (c == '/' && next == '/') {
                lineComment = true;
                i++;
            } else if (c == '/' && next == '*') {
                blockComment = true;
                i++;
            } else if (c == '\'' || c == '"' || c == 96) {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    /** Splits only commas at the top level of a call's argument list. */
    private static List<ScriptArgument> splitScriptArguments(String script, int start, int end) {
        List<ScriptArgument> out = new java.util.ArrayList<>();
        int parens = 0, brackets = 0, braces = 0;
        int argumentStart = start;
        char quote = 0;
        boolean escaped = false;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int i = start; i < end; i++) {
            char c = script.charAt(i);
            char next = i + 1 < end ? script.charAt(i + 1) : 0;
            if (lineComment) {
                if (c == '\n') lineComment = false;
                continue;
            }
            if (blockComment) {
                if (c == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
                continue;
            }
            if (quote != 0) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
                continue;
            }
            if (c == '/' && next == '/') {
                lineComment = true;
                i++;
            } else if (c == '/' && next == '*') {
                blockComment = true;
                i++;
            } else if (c == '\'' || c == '"' || c == 96) {
                quote = c;
            } else if (c == '(') {
                parens++;
            } else if (c == ')') {
                parens--;
            } else if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets--;
            } else if (c == '{') {
                braces++;
            } else if (c == '}') {
                braces--;
            } else if (c == ',' && parens == 0 && brackets == 0 && braces == 0) {
                out.add(new ScriptArgument(argumentStart, i));
                argumentStart = i + 1;
            }
        }
        out.add(new ScriptArgument(argumentStart, end));
        return out;
    }

    /**
     * AlienEvo 1.1.3 uses the pre-1.21 Scoreboard and ScoreAccess APIs in
     * command and ability scripts. In 1.21.1 player score lookup requires a
     * ScoreHolder and ScoreAccess exposes get()/set(). Scope these edits to
     * the exact scripts inspected in the original AlienEvo archive.
     */
    private static int rewriteScoreboardApi(Map<String, byte[]> resources) {
        String[] paths = {
                "addon/alienevo/kubejs_scripts/charge_leap.js",
                "addon/alienevo/kubejs_scripts/detransform.js",
                "addon/alienevo/kubejs_scripts/rolling_mechanics.js",
                "addon/alienevo/kubejs_scripts/stat_ability.js",
                "data/alienevo/kubejs_scripts/level_command.js",
                "data/alienevo/kubejs_scripts/xp.js"
        };
        int changed = 0;
        for (String path : paths) {
            byte[] original = resources.get(path);
            if (original == null) continue;

            String script = new String(original, StandardCharsets.UTF_8);
            String migrated = script;
            for (String playerName : new String[] {"username", "playerName", "playerUsername"}) {
                String legacy = "getOrCreatePlayerScore(" + playerName + ",";
                String modern = "getOrCreatePlayerScore(Java.loadClass('net.minecraft.world.scores.ScoreHolder').forNameOnly(String("
                        + playerName + ")),";
                migrated = migrated.replace(legacy, modern);
            }
            migrated = migrated.replace(".getScore()", ".get()");
            migrated = migrated.replace(".setScore(", ".set(");
            if ("addon/alienevo/kubejs_scripts/detransform.js".equals(path)) {
                migrated = guardDetransformTimerObjective(migrated);
            }

            if (!migrated.equals(script)) {
                resources.put(path, migrated.getBytes(StandardCharsets.UTF_8));
                changed++;
            }
        }
        return changed;
    }

    /** AlienEvo's flatten ability uses one optional Pehkui class at script load time; keep the script group loadable without Pehkui. */
    private static int rewriteOptionalPehkuiApi(Map<String, byte[]> resources) {
        byte[] original = resources.get(ALIEN_EVO_FLATTEN);
        if (original == null) return 0;
        String script = new String(original, StandardCharsets.UTF_8);
        if (!script.contains(LEGACY_PEHKUI_SCALE_TYPES_BINDING)) return 0;
        String lineEnding = script.contains("\r\n") ? "\r\n" : "\n";
        String migrated = replaceExactScriptOccurrence(
                ALIEN_EVO_FLATTEN, script, LEGACY_PEHKUI_SCALE_TYPES_BINDING,
                OPTIONAL_PEHKUI_SCALE_TYPES_BINDING.replace("\n", lineEnding));
        resources.put(ALIEN_EVO_FLATTEN, migrated.getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    /** A missing timer objective must not abort reversion before the Omnitrix and transformation state are restored. */
    private static String guardDetransformTimerObjective(String script) {
        String declaration = "let timerObjective = scoreboardManager.getObjective('AlienEvo.Timer');";
        int declarationIndex = script.indexOf(declaration);
        if (declarationIndex < 0) return script;
        if (occurrences(script, declaration) != 1) {
            throw new IllegalStateException("AlienEvo detransform.js has multiple timer objective declarations");
        }
        int lineStart = script.lastIndexOf('\n', declarationIndex) + 1;
        int declarationEnd = script.indexOf('\n', declarationIndex);
        if (declarationEnd < 0) declarationEnd = script.length();
        else declarationEnd++;
        String indent = script.substring(lineStart, declarationIndex);
        String lineEnding = declarationEnd > 0 && script.charAt(declarationEnd - 2) == '\r' ? "\r\n" : "\n";
        String guard = indent + "if (timerObjective != null) {";
        if (script.startsWith(guard, declarationEnd)) return script;

        String timerWrite = "entity.server.runCommandSilent(`execute as ${playerUsername} at @s run scoreboard players set @s AlienEvo.Timer ${Math.floor(currentTimerValue / 2)}`);";
        int timerWriteIndex = script.indexOf(timerWrite, declarationEnd);
        if (timerWriteIndex < 0 || occurrences(script, timerWrite) != 1) {
            throw new IllegalStateException("AlienEvo detransform.js has an unexpected timer write shape");
        }
        int timerWriteEnd = script.indexOf('\n', timerWriteIndex);
        if (timerWriteEnd < 0) timerWriteEnd = script.length();
        else timerWriteEnd++;
        return script.substring(0, declarationEnd)
                + guard + lineEnding
                + script.substring(declarationEnd, timerWriteEnd)
                + indent + "}" + lineEnding
                + script.substring(timerWriteEnd);
    }

    private static boolean isAlienEvoKubeJsScript(String path) {
        return path.endsWith(".js")
                && (path.startsWith("addon/alienevo/kubejs_scripts/")
                        || path.startsWith("assets/alienevo/kubejs_scripts/")
                        || path.startsWith("data/alienevo/kubejs_scripts/"));
    }

    private static String insertAfterOmnitrixIntegerHelper(String script, String helper) {
        if (script.startsWith(OMNITRIX_INTEGER_HELPER)) {
            return OMNITRIX_INTEGER_HELPER
                    + helper
                    + script.substring(OMNITRIX_INTEGER_HELPER.length());
        }
        return helper + script;
    }

    private static String addSoundEventHelper(String script) {
        if (script.contains(SOUND_EVENT_HELPER_MARKER)) return script;
        return insertAfterOmnitrixIntegerHelper(script, LEGACY_SOUND_EVENT_HELPER);
    }

    private static String replaceExactScriptOccurrence(
            String path, String script, String legacy, String safe) {
        int legacyCount = occurrences(script, legacy);
        int safeCount = occurrences(script, safe);
        if (legacyCount == 0 && safeCount == 1) return script;
        if (legacyCount != 1 || safeCount != 0) {
            throw new IllegalStateException(
                    "AlienEvo resource " + path + " has an unexpected sound-event rewrite shape: expected "
                            + "1 legacy occurrence, found " + legacyCount + " legacy and " + safeCount + " migrated");
        }
        return script.replace(legacy, safe);
    }

    private static int rewrite(Map<String, byte[]> resources, String path, String[][] replacements) {
        byte[] original = resources.get(path);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        String migrated = script;
        boolean changed = false;

        for (String[] replacement : replacements) {
            String legacy = replacement[0];
            String safe = replacement[1];
            int expected = Integer.parseInt(replacement[2]);
            int legacyCount = occurrences(migrated, legacy);
            int safeCount = occurrences(migrated, safe);

            if (legacyCount == 0 && safeCount == expected) continue;
            if (legacyCount != expected || safeCount != 0) {
                throw new IllegalStateException(
                        "AlienEvo resource " + path + " has an unexpected compatibility-rewrite shape: expected "
                                + expected + " legacy occurrence(s), found " + legacyCount
                                + " legacy and " + safeCount + " migrated");
            }

            migrated = migrated.replace(legacy, safe);
            changed = true;
        }

        if (changed) {
            resources.put(path, migrated.getBytes(StandardCharsets.UTF_8));
            return 1;
        }
        return 0;
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int offset = 0; (offset = text.indexOf(needle, offset)) >= 0; offset += needle.length()) {
            count++;
        }
        return count;
    }
}
