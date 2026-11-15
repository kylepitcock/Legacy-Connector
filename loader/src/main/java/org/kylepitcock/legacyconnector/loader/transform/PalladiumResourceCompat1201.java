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
    static final String ALIEN_EVO_BLOCK_PARTICLES =
            "addon/alienevo/kubejs_scripts/spawn_particles_block.js";
    static final String ALIEN_EVO_TRANSFORM =
            "addon/alienevo/kubejs_scripts/transform.js";
    static final String ALIEN_EVO_POWER_UI =
            "assets/alienevo/kubejs_scripts/power_ui.js";
    static final String ALIEN_EVO_BLOCKS =
            "addon/alienevo/kubejs_scripts/blocks.js";
    static final String ALIEN_EVO_SERVER_CRYSTAL_TICKS =
            "data/alienevo/kubejs_scripts/legacyconnector/crystal_block_expiry.js";
    static final String AFOMNI_AOE_DAMAGE =
            "addon/afomni/kubejs_scripts/aoe_damage.js";
    static final String ALIEN_EVO_DIAMOND_CREATION =
            "addon/alienevo/kubejs_scripts/diamond_creation.js";
    static final String ALIEN_EVO_STAT_ABILITY =
            "addon/alienevo/kubejs_scripts/stat_ability.js";
    static final String ALIEN_EVO_DIRECTIONAL_DAMAGE =
            "addon/alienevo/kubejs_scripts/directional_damage.js";
    static final String ALIEN_EVO_ENTITY_DAMAGE =
            "addon/alienevo/kubejs_scripts/entity_damage.js";
    static final String ALIEN_EVO_INFINITY =
            "addon/alienevo/kubejs_scripts/infinity.js";
    static final String ALIEN_EVO_ROLLING_MECHANICS =
            "addon/alienevo/kubejs_scripts/rolling_mechanics.js";
    private static final String LEGACY_ATTRIBUTE_OPERATION_HELPER = """
            function legacyConnectorAttributeOperation(operation) {
                const key = String(operation);
                switch (key) {
                    case "addition": return "add_value";
                    case "multiply_base": return "add_multiplied_base";
                    case "multiply_total": return "add_multiplied_total";
                    default: return key;
                }
            }

            """;
    static final String ALIEN_EVO_FAILSAFE =
            "data/alienevo/kubejs_scripts/failsafe.js";
    static final String ALIEN_EVO_SERVER_STUFF =
            "data/alienevo/kubejs_scripts/server_stuff.js";

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
    static final String LEGACY_SOUND_SOURCE_HELPER =
            "function getAlienEvoSoundSource(soundSource) {\n"
                    + "    const AlienEvoSoundSource = Java.loadClass('net.minecraft.sounds.SoundSource');\n"
                    + "    return AlienEvoSoundSource.valueOf(String(soundSource).toUpperCase());\n"
                    + "}\n\n";
    private static final String ALIEN_EVO_CYCLE = "addon/alienevo/kubejs_scripts/cycle_sounds.js";
    private static final String ALIEN_EVO_DETRANSFORM = "addon/alienevo/kubejs_scripts/detransform.js";
    static final String ALIEN_EVO_RECHARGE = "addon/alienevo/kubejs_scripts/recharge.js";
    private static final String ALIEN_EVO_FLATTEN = "addon/alienevo/kubejs_scripts/flatten.js";
    private static final String ALIEN_EVO_DECOUPLE = "addon/alienevo/kubejs_scripts/decouple.js";
    static final String ALIEN_EVO_CORE_REMOVAL = "addon/alienevo/kubejs_scripts/core_removal.js";
    private static final String RESOURCE_LOCATION_HELPER_MARKER =
            "function ResourceLocation(namespaceOrLocation, path) {";
    private static final String SOUND_EVENT_HELPER_MARKER =
            "function getAlienEvoSoundEvent(soundId) {";
    private static final String SOUND_SOURCE_HELPER_MARKER =
            "function getAlienEvoSoundSource(soundSource) {";

    private static final String ALIEN_EVO_OBTAIN_SCROLL_ADVANCEMENT =
            "data/alienevo/advancements/obtain_scroll.json";
    private static final String ALIEN_EVO_MACHINE_SCROLL_TAG =
            "data/alienevo/tags/item/machine_scroll.json";
    private static final String AFOMNI_CRYSTALSAPIEN_ANIMATION_COPY =
            "assets/afomni/animations/crystalsapien.animation_copy.json";
    private static final String AFOMNI_CRYSTALSAPIEN_ANIMATION =
            "assets/afomni/animations/crystalsapien.animation.json";
    private static final String AFOMNI_HUMONGOSAUR_ANIMATION_SOURCE =
            "assets/afomni/animations/model.humongousaur.anim.json";
    private static final String AFOMNI_HUMONGOSAUR_ANIMATION_TARGET =
            "assets/afomni/animations/model.humongousaur.animation.json";
    private static final String AFOMNI_VAXASAURIAN_LAYER =
            "assets/afomni/palladium/render_layers/vaxasaurian.json";
    private static final String ALIEN_EVO_XLR8_ANIMATION_SOURCE =
            "assets/alienevo/animations/xlr8.json";
    private static final String ALIEN_EVO_XLR8_ANIMATION_TARGET =
            "assets/alienevo/animations/xlr8.animation.json";
    private static final String ALIEN_EVO_KINECELERAN_LAYER =
            "assets/alienevo/palladium/render_layers/aliens/alien_4/kineceleran.json";

    private static final String ALIEN_EVO_PACKET_STRING_HELPER_MARKER =
            "function legacyConnectorAlienEvoPacketString(value) {";
    private static final String ALIEN_EVO_PACKET_STRING_HELPER = """
            function legacyConnectorAlienEvoPacketString(value) {
                if (value != null && typeof value.getAsString === 'function') {
                    return String(value.getAsString());
                }
                return value;
            }

            """;
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
     * @return number of resource entries changed
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
        rewritePowerUiEditBoxTick(resources);
        rewriteAlienEvoTemporaryCrystalBlockTicks(resources);
        rewriteAfOmniAoeDamageMarkerGuard(resources);
        rewriteDiamondShardTexture(resources);
        rewriteAfOmniParticleTextureReferences(resources);
        rewriteGeckoLibAnimationFileNames(resources);
        removeDuplicateMalformedGeckoLibAnimation(resources);
        addMissingAlienEvoMachineScrollTag(resources);
        rewriteLegacyScriptApis(resources);
        rewriteAlienEvoLevelPlaySoundCalls(resources);
        rewriteScoreboardApi(resources);
        rewriteServerApiAccess(resources);
        rewriteOmnitrixPacketPlayerAccess(resources);
        rewriteOmnitrixPacketStringValues(resources);
        rewriteAlienEvoXpDeathPlayerAccess(resources);
        rewriteAlienEvoRechargeCooldown(resources);
        rewriteAlienEvoStatAbilityOperation(resources);
        // Both ScriptableAbility callbacks receive the active entity; KubeJS 7 no longer exposes Utils.server.
        rewrite(resources, ALIEN_EVO_STAT_ABILITY, new String[][] {
                {"Utils.server.scoreboard", "entity.server.scoreboard", "2"},
                {"entity.removeAttribute(attribute, UUID)",
                        "entity.removeAttribute(attribute, 'alienevo:stat_ability/' + String(uuid).toLowerCase().replace(/[^a-z0-9/._-]/g, '_'))", "1"},
                {"entity.modifyAttribute(attribute, UUID,",
                        "entity.modifyAttribute(attribute, 'alienevo:stat_ability/' + String(uuid).toLowerCase().replace(/[^a-z0-9/._-]/g, '_'),", "1"}
        });
        rewriteOptionalPehkuiApi(resources);
        rewriteAlienEvoBlockParticleOptions(resources);
        rewriteAlienEvoSlotPersistentDataIntegers(resources);
        rewriteAlienEvoSetSecondsOnFire(resources);

        // A single resource can receive more than one independent rewrite.
        // Report distinct changed resource paths, including resources removed by a migration.
        int changedResources = 0;
        java.util.Set<String> changedPaths = new java.util.HashSet<>(original.keySet());
        changedPaths.addAll(resources.keySet());
        for (String path : changedPaths) {
            byte[] before = original.get(path);
            byte[] after = resources.get(path);
            // Count additions, in-place edits, and removals exactly once per resource path.
            if (!java.util.Arrays.equals(before, after)) {
                changedResources++;
            }
        }
        return changedResources;
    }

    /**
     * The legacy AlienEvo color editor calls EditBox.tick(), removed in 1.21.1.
     * Screen input processing already ticks the widget; calling this obsolete
     * method from renderPowerScreen throws on every rendered frame.
     */
    private static int rewritePowerUiEditBoxTick(Map<String, byte[]> resources) {
        return rewrite(resources, ALIEN_EVO_POWER_UI, new String[][] {
                {
                        "nickTextBox.tick();",
                        "/* Nickname EditBox input is ticked by the 1.21.1 screen; no manual tick() call. */",
                        "1"
                },
                {
                        "hexTextBox.tick();",
                        "/* HEX EditBox input is ticked by the 1.21.1 screen; no manual tick() call. */",
                        "1"
                },
                {
                        "let cleanedHexString = rawTextboxValue.replace(/[^0-9A-Fa-f]/g, \"\").slice(0, 6);",
                        "let cleanedHexString = rawTextboxValue.replaceAll(\"[^0-9A-Fa-f]\", \"\");\n"
                                + "            if (cleanedHexString.length() > 6) {\n"
                                + "                cleanedHexString = cleanedHexString.substring(0, 6);\n"
                                + "            }",
                        "1"
                }
        });
    }

    /**
     * KubeJS 1.20.1 allowed the server ticker callback to be attached while
     * registering the block during STARTUP. KubeJS 7 separates ticker setup
     * from the BlockEvents listener: the block builder remains in the addon
     * STARTUP resource, while the event listener must be loaded as a SERVER
     * script from data/<namespace>/kubejs_scripts.
     *
     * Preserve the original 2500/250 cadence and no-drop destruction behavior.
     */
    private static int rewriteAlienEvoTemporaryCrystalBlockTicks(Map<String, byte[]> resources) {
        byte[] original = resources.get(ALIEN_EVO_BLOCKS);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        String lineEnding = script.contains("\r\n") ? "\r\n" : "\n";
        String serverScript = "// Legacy Connector: Diamondhead temporary crystal expiry (SERVER script)" + lineEnding
                + "BlockEvents.blockEntityTick('alienevo:crystal_block', event => {" + lineEnding
                + "    event.level.destroyBlock(event.block.pos, false);" + lineEnding
                + "});" + lineEnding
                + "BlockEvents.blockEntityTick('alienevo:smooth_crystal_block', event => {" + lineEnding
                + "    event.level.destroyBlock(event.block.pos, false);" + lineEnding
                + "});" + lineEnding;
        byte[] serverBytes = serverScript.getBytes(StandardCharsets.UTF_8);
        byte[] existingServerScript = resources.get(ALIEN_EVO_SERVER_CRYSTAL_TICKS);
        if (existingServerScript != null && !java.util.Arrays.equals(existingServerScript, serverBytes)) {
            throw new IllegalStateException(
                    "AlienEvo already contains an unexpected server crystal-expiry script at "
                            + ALIEN_EVO_SERVER_CRYSTAL_TICKS);
        }

        String legacyStart = "blockEntity.serverTick(2500, 250, entity => {";
        String migratedOffset = "blockEntity.tickOffset(250);";
        int legacyCount = occurrences(script, legacyStart);
        int migratedCount = occurrences(script, migratedOffset);

        if (legacyCount == 0 && migratedCount == 2) {
            if (existingServerScript == null) {
                resources.put(ALIEN_EVO_SERVER_CRYSTAL_TICKS, serverBytes);
                return 1;
            }
            return 0;
        }
        if (legacyCount != 2 || migratedCount != 0
                || script.contains("BlockEvents.blockEntityTick('alienevo:crystal_block'")
                || script.contains("BlockEvents.blockEntityTick('alienevo:smooth_crystal_block'")) {
            throw new IllegalStateException(
                    "AlienEvo blocks.js has an unexpected temporary crystal tick migration shape: expected "
                            + "two legacy serverTick calls, found " + legacyCount + " legacy and "
                            + migratedCount + " migrated");
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?m)^([\\t ]*)blockEntity\\.serverTick\\(2500, 250, entity => \\{\\R"
                        + "[\\t ]*entity\\.level\\.destroyBlock\\(entity\\.blockPos, false\\)\\R"
                        + "[\\t ]*\\}\\)");
        java.util.regex.Matcher matcher = pattern.matcher(script);
        StringBuffer migrated = new StringBuffer();
        int replacements = 0;
        while (matcher.find()) {
            String indent = matcher.group(1);
            String replacement = indent + "blockEntity.serverTicking();" + lineEnding
                    + indent + "blockEntity.tickFrequency(2500);" + lineEnding
                    + indent + "blockEntity.tickOffset(250);";
            matcher.appendReplacement(migrated, java.util.regex.Matcher.quoteReplacement(replacement));
            replacements++;
        }
        matcher.appendTail(migrated);
        if (replacements != 2) {
            throw new IllegalStateException(
                    "AlienEvo blocks.js expected two complete legacy temporary crystal callbacks, found "
                            + replacements);
        }

        resources.put(ALIEN_EVO_BLOCKS, migrated.toString().getBytes(StandardCharsets.UTF_8));
        if (existingServerScript == null) {
            resources.put(ALIEN_EVO_SERVER_CRYSTAL_TICKS, serverBytes);
            return 2;
        }
        return 1;
    }

    /**
     * AFOMNI's shared AOE ability may include Palladium Marker entities in its
     * target AABB. Markers are not LivingEntity instances and cannot receive
     * setSecondsOnFire. Keep normal damage handling for the other supported
     * targets, exclude internal marker entities, and apply fire only to living
     * targets with a positive configured fire duration.
     */
    private static int rewriteAfOmniAoeDamageMarkerGuard(Map<String, byte[]> resources) {
        byte[] original = resources.get(AFOMNI_AOE_DAMAGE);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        String lineEnding = script.contains("\r\n") ? "\r\n" : "\n";
        String header = "StartupEvents.registry('palladium:abilities', (event) => {";
        String declaration = "    const LivingEntity = Java.loadClass('net.minecraft.world.entity.LivingEntity');";
        String legacyFilter = "targets[j].type !== \"palladium:suit_stand\") {";
        String migratedFilter =
                "targets[j].type !== \"palladium:suit_stand\" && targets[j].type !== \"minecraft:marker\") {";
        String fireGuard = "if (firetick > 0 && targets[j] instanceof LivingEntity) {";

        int headerCount = occurrences(script, header);
        int declarationCount = occurrences(script, declaration);
        int legacyFilterCount = occurrences(script, legacyFilter);
        int migratedFilterCount = occurrences(script, migratedFilter);
        int fireCallCount = occurrences(script, "targets[j].setSecondsOnFire(firetick);");
        int fireGuardCount = occurrences(script, fireGuard);

        boolean alreadyMigrated = headerCount == 1 && declarationCount == 1
                && legacyFilterCount == 0 && migratedFilterCount == 1
                && fireCallCount == 1 && fireGuardCount == 1;
        if (alreadyMigrated) return 0;

        boolean legacyShape = headerCount == 1 && declarationCount == 0
                && legacyFilterCount == 1 && migratedFilterCount == 0
                && fireCallCount == 1 && fireGuardCount == 0;
        if (!legacyShape) {
            throw new IllegalStateException(
                    "AFOMNI aoe_damage.js has an unexpected Marker/fire migration shape: "
                            + "header=" + headerCount + ", LivingEntity declaration=" + declarationCount
                            + ", legacy filter=" + legacyFilterCount + ", migrated filter=" + migratedFilterCount
                            + ", setSecondsOnFire calls=" + fireCallCount + ", fire guards=" + fireGuardCount);
        }

        String migrated = script;
        if (declarationCount == 0) {
            migrated = migrated.replace(header, header + lineEnding + declaration);
        }
        migrated = migrated.replace(legacyFilter, migratedFilter);

        java.util.regex.Pattern fireCallPattern = java.util.regex.Pattern.compile(
                "(?m)^([\t ]*)targets\\[j\\]\\.setSecondsOnFire\\(firetick\\);\\R?");
        java.util.regex.Matcher fireCallMatcher = fireCallPattern.matcher(migrated);
        StringBuffer fireRewritten = new StringBuffer();
        int fireCallReplacements = 0;
        while (fireCallMatcher.find()) {
            String indent = fireCallMatcher.group(1);
            String replacement = indent + fireGuard + lineEnding
                    + indent + "    targets[j].setSecondsOnFire(firetick);" + lineEnding
                    + indent + "}" + lineEnding;
            fireCallMatcher.appendReplacement(
                    fireRewritten, java.util.regex.Matcher.quoteReplacement(replacement));
            fireCallReplacements++;
        }
        fireCallMatcher.appendTail(fireRewritten);
        if (fireCallReplacements != 1) {
            throw new IllegalStateException(
                    "AFOMNI aoe_damage.js expected one unguarded setSecondsOnFire call, found "
                            + fireCallReplacements);
        }

        resources.put(AFOMNI_AOE_DAMAGE, fireRewritten.toString().getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    /** Fix AlienEvo's item model reference to the existing vanilla diamond-spike texture. */
    private static int rewriteDiamondShardTexture(Map<String, byte[]> resources) {
        String path = "assets/alienevo/models/item/diamond_shard.json";
        return rewrite(resources, path, new String[][] {{
                "\"diamond/spike\"", "\"minecraft:diamond_spike/spike\"", "2"
        }});
    }

    /**
     * AlienEvo's obtain_scroll advancement checks the alienevo:machine_scroll
     * item tag, but the source JAR does not ship that tag. Register the one
     * intended item so the advancement can trigger without changing recipes
     * or other item membership.
     */
    private static int addMissingAlienEvoMachineScrollTag(Map<String, byte[]> resources) {
        byte[] advancementBytes = resources.get(ALIEN_EVO_OBTAIN_SCROLL_ADVANCEMENT);
        if (advancementBytes == null || resources.containsKey(ALIEN_EVO_MACHINE_SCROLL_TAG)) return 0;

        String advancement = new String(advancementBytes, StandardCharsets.UTF_8);
        if (!advancement.contains("\"tag\": \"alienevo:machine_scroll\"")) return 0;

        String tag = """
                {
                  "replace": false,
                  "values": [
                    "alienevo:machine_scroll"
                  ]
                }
                """;
        resources.put(ALIEN_EVO_MACHINE_SCROLL_TAG, tag.getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    /**
     * GeckoLib 4 discovers animation JSON assets by the .animation.json suffix.
     * AlienEvo 1.1.3 contains two animation files with older/nonstandard names
     * and its render-layer JSON points directly at those names. Keep the source
     * assets under the expected names, update the two inspected render-layer
     * references, and remove only the obsolete source filenames after migration.
     */
    private static int rewriteGeckoLibAnimationFileNames(Map<String, byte[]> resources) {
        int changed = 0;
        changed += addAnimationAlias(
                resources, AFOMNI_HUMONGOSAUR_ANIMATION_SOURCE, AFOMNI_HUMONGOSAUR_ANIMATION_TARGET);
        changed += addAnimationAlias(
                resources, ALIEN_EVO_XLR8_ANIMATION_SOURCE, ALIEN_EVO_XLR8_ANIMATION_TARGET);
        changed += rewrite(resources, AFOMNI_VAXASAURIAN_LAYER, new String[][] {{
                "afomni:animations/model.humongousaur.anim.json",
                "afomni:animations/model.humongousaur.animation.json", "1"
        }});
        changed += rewrite(resources, ALIEN_EVO_KINECELERAN_LAYER, new String[][] {{
                "alienevo:animations/xlr8.json",
                "alienevo:animations/xlr8.animation.json", "3"
        }});
        return changed;
    }

    private static int addAnimationAlias(
            Map<String, byte[]> resources, String sourcePath, String targetPath) {
        byte[] source = resources.get(sourcePath);
        if (source == null) return 0;

        byte[] existing = resources.get(targetPath);
        if (existing != null && !java.util.Arrays.equals(source, existing)) {
            throw new IllegalStateException(
                    "AlienEvo animation alias " + targetPath + " already exists with different content");
        }

        if (existing == null) {
            resources.put(targetPath, source.clone());
        }
        // The associated render-layer reference is migrated below, so keeping
        // the old malformed filename would only trigger another GeckoLib warning.
        resources.remove(sourcePath);
        return 1;
    }

    /**
     * The source JAR contains an unreferenced copy of crystalsapien.animation.json
     * named animation_copy.json. It is byte-identical to the valid file and has
     * no resource references; remove only that exact duplicate so GeckoLib does
     * not try to interpret the malformed name as an animation asset.
     */
    private static int removeDuplicateMalformedGeckoLibAnimation(Map<String, byte[]> resources) {
        byte[] duplicate = resources.get(AFOMNI_CRYSTALSAPIEN_ANIMATION_COPY);
        byte[] canonical = resources.get(AFOMNI_CRYSTALSAPIEN_ANIMATION);
        if (duplicate == null || canonical == null || !java.util.Arrays.equals(duplicate, canonical)) {
            return 0;
        }
        resources.remove(AFOMNI_CRYSTALSAPIEN_ANIMATION_COPY);
        return 1;
    }

    /**
     * AFOMNI's swamp particle JSONs refer to unshipped afomni:flames_00..13
     * sprites. The corresponding frames are supplied by AlienEvo instead.
     * Keep the AFOMNI-specific 00b..04b sprites untouched.
     */
    private static int rewriteAfOmniParticleTextureReferences(Map<String, byte[]> resources) {
        int changed = 0;
        for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
            String path = entry.getKey();
            if (!path.startsWith("assets/afomni/particles/") || !path.endsWith(".json")) continue;

            String script = new String(entry.getValue(), StandardCharsets.UTF_8);
            String migrated = script;
            for (int frame = 0; frame <= 13; frame++) {
                String suffix = frame < 10 ? "0" + frame : Integer.toString(frame);
                String texturePath = "assets/alienevo/textures/particle/flames_" + suffix + ".png";
                String legacyId = "\"afomni:flames_" + suffix + "\"";
                String safeId = "\"alienevo:flames_" + suffix + "\"";
                if (!resources.containsKey("assets/afomni/textures/particle/flames_" + suffix + ".png")
                        && resources.containsKey(texturePath)) {
                    migrated = migrated.replace(legacyId, safeId);
                }
            }

            if (!migrated.equals(script)) {
                entry.setValue(migrated.getBytes(StandardCharsets.UTF_8));
                changed++;
            }
        }
        return changed;
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
            } else if (ALIEN_EVO_FAILSAFE.equals(path)) {
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
                migrated = replaceExactScriptOccurrence(
                        path,
                        migrated,
                        "var watchItem = Item.of(itemId, 1, nbtData);",
                        "var watchItem = Item.of(itemId, 1);\n  watchItem.setNbt(nbtData);");
            } else if (ALIEN_EVO_CORE_REMOVAL.equals(path)) {
                migrated = replaceExactScriptOccurrence(
                        path,
                        migrated,
                        "var coreItem = Item.of('alienevo:prototype_core', 1, nbtData);",
                        "var coreItem = Item.of('alienevo:prototype_core', 1);\n  coreItem.setNbt(nbtData);");
            }

            if (!migrated.equals(script)) {
                entry.setValue(migrated.getBytes(StandardCharsets.UTF_8));
                changed++;
            }
        }
        return changed;
    }


    /**
     * AlienEvo 1.1.3 has 43 direct eight-argument Level.playSound calls in its
     * server-side KubeJS scripts. In 1.21.1 Rhino sees both the Holder and
     * SoundEvent overloads when the sound is passed as a JS string. Some scripts
     * also pass the sound category as a string. Resolve both arguments to their
     * exact Java enum/event types before overload selection. Keep the 3-argument
     * client player.playSound helper calls untouched.
     *
     * @return number of distinct AlienEvo script resources changed
     */
    private static int rewriteAlienEvoLevelPlaySoundCalls(Map<String, byte[]> resources) {
        int changed = 0;
        for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
            if (!isAlienEvoKubeJsScript(entry.getKey())) continue;

            String path = entry.getKey();
            String script = new String(entry.getValue(), StandardCharsets.UTF_8);
            List<ScriptRewrite> rewrites = new java.util.ArrayList<>();
            boolean needsSoundEventHelper = false;
            boolean needsSoundSourceHelper = false;
            int searchFrom = 0;
            while (true) {
                int call = script.indexOf(".playSound(", searchFrom);
                if (call < 0) break;
                int open = call + ".playSound".length();
                int close = matchingCallParen(script, open);
                if (close < 0) {
                    throw new IllegalStateException(
                            "AlienEvo resource " + path + " has an unterminated playSound call");
                }

                List<ScriptArgument> arguments = splitScriptArguments(script, open + 1, close);
                if (arguments.size() == 8
                        && "null".equals(script.substring(arguments.get(0).start(), arguments.get(0).end()).trim())) {
                    ScriptArgument sound = arguments.get(4);
                    String soundExpression = script.substring(sound.start(), sound.end()).trim();
                    if (!soundExpression.startsWith("getAlienEvoSoundEvent(")) {
                        rewrites.add(new ScriptRewrite(sound.start(), sound.end(), "getAlienEvoSoundEvent"));
                        needsSoundEventHelper = true;
                    }

                    ScriptArgument source = arguments.get(5);
                    String sourceExpression = script.substring(source.start(), source.end()).trim();
                    boolean stringSoundSource = sourceExpression.startsWith("'")
                            || sourceExpression.startsWith("\"")
                            || (!sourceExpression.isEmpty() && sourceExpression.charAt(0) == 96);
                    if (stringSoundSource && !sourceExpression.startsWith("getAlienEvoSoundSource(")) {
                        rewrites.add(new ScriptRewrite(source.start(), source.end(), "getAlienEvoSoundSource"));
                        needsSoundSourceHelper = true;
                    }
                }
                searchFrom = close + 1;
            }

            if (rewrites.isEmpty()) continue;

            // Apply argument replacements from right to left so original offsets remain valid.
            rewrites.sort((left, right) -> Integer.compare(right.start(), left.start()));
            String migrated = script;
            for (ScriptRewrite rewrite : rewrites) {
                String expression = migrated.substring(rewrite.start(), rewrite.end()).trim();
                migrated = migrated.substring(0, rewrite.start())
                        + rewrite.helperName() + "(" + expression + ")"
                        + migrated.substring(rewrite.end());
            }
            if (needsSoundEventHelper) migrated = addSoundEventHelper(migrated);
            if (needsSoundSourceHelper) migrated = addSoundSourceHelper(migrated);
            if (!migrated.equals(script)) {
                entry.setValue(migrated.getBytes(StandardCharsets.UTF_8));
                changed++;
            }
        }
        return changed;
    }

    private record ScriptArgument(int start, int end) {}
    private record ScriptRewrite(int start, int end, String helperName) {}

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

    /**
     * KubeJS 7 no longer exposes Utils.server. Resolve the server from the
     * active ability entity/player, or pass it from the command target into
     * the helper functions. These rewrites are scoped to the inspected
     * AlienEvo callbacks; do not replace Utils.server globally.
     */
    private static int rewriteServerApiAccess(Map<String, byte[]> resources) {
        int changed = 0;
        changed += rewrite(resources, "addon/alienevo/kubejs_scripts/charge_leap.js", new String[][] {
                {"Utils.server.scoreboard", "entity.server.scoreboard", "2"}
        });
        changed += rewrite(resources, "addon/alienevo/kubejs_scripts/detransform.js", new String[][] {
                {"Utils.server.scoreboard", "entity.server.scoreboard", "1"}
        });
        changed += rewrite(resources, "addon/alienevo/kubejs_scripts/rolling_mechanics.js", new String[][] {
                {"Utils.server.scoreboard", "entity.server.scoreboard", "1"}
        });
        changed += rewrite(resources, "data/alienevo/kubejs_scripts/xp.js", new String[][] {
                {"Utils.server.scoreboard", "player.server.scoreboard", "1"}
        });
        changed += rewriteLevelCommandServerAccess(resources);
        return changed;
    }

    /** The command callbacks provide a ServerPlayer, so pass its server into each helper explicitly. */
    private static int rewriteLevelCommandServerAccess(Map<String, byte[]> resources) {
        return rewrite(resources, "data/alienevo/kubejs_scripts/level_command.js", new String[][] {
                {"function maxAllAliens(playerName) {", "function maxAllAliens(playerName, server) {", "1"},
                {"function giveAlienControl(playerName) {", "function giveAlienControl(playerName, server) {", "1"},
                {"  let server = Utils.server;", "  // Legacy Connector: KubeJS 7 server is passed by the command target.", "2"},
                {"let aliensMaxed = maxAllAliens(username);", "let aliensMaxed = maxAllAliens(username, target.server);", "1"},
                {"return giveAlienControl(username);", "return giveAlienControl(username, target.server);", "1"}
        });
    }

    /**
     * KubeJS 7 dataReceived packets expose their sender as event.player.
     * AlienEvo already uses that property in its saveBeamColorData and
     * saveEnergyColorData handlers, but the palette and nickname handlers
     * still read event.entity. Migrate only those two known handlers.
     */
    private static int rewriteOmnitrixPacketPlayerAccess(Map<String, byte[]> resources) {
        byte[] original = resources.get(ALIEN_EVO_SERVER_STUFF);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        String legacy = "let player = event.entity;";
        String modern = "let player = event.player;";
        int legacyCount = occurrences(script, legacy);
        int modernCount = occurrences(script, modern);

        // The inspected file already has two modern event.player handlers
        // (beam and energy colors). Only palette and nickname are legacy;
        // after migration all four handlers use event.player.
        if (legacyCount == 0 && modernCount == 4) return 0;
        if (legacyCount != 2 || modernCount != 2) {
            throw new IllegalStateException(
                    "AlienEvo server_stuff.js has an unexpected packet-player rewrite shape: expected "
                            + "2 legacy and 2 existing modern handlers, found "
                            + legacyCount + " legacy and " + modernCount + " modern");
        }

        resources.put(ALIEN_EVO_SERVER_STUFF,
                script.replace(legacy, modern).getBytes(StandardCharsets.UTF_8));
        return 1;
    }
    /**
     * KubeJS 7 delivers client packet compound values as NBT tags. AlienEvo's
     * string properties expect Java String values, not StringTag instances;
     * StringProperty.toNBT otherwise throws while the property synchronizes
     * or the entity is saved. Normalize only the four inspected string-payload
     * handlers, preserving non-tag values unchanged.
     */
    private static int rewriteOmnitrixPacketStringValues(Map<String, byte[]> resources) {
        byte[] original = resources.get(ALIEN_EVO_SERVER_STUFF);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        String[][] replacements = {
                {"let colorValue = data.value;",
                        "let colorValue = legacyConnectorAlienEvoPacketString(data.value);", "1"},
                {"let energyColor = data.value;",
                        "let energyColor = legacyConnectorAlienEvoPacketString(data.value);", "1"},
                {"palladium.setProperty(player, propertyName, propertiesToSave[propertyName]);",
                        "palladium.setProperty(player, propertyName, legacyConnectorAlienEvoPacketString(propertiesToSave[propertyName]));", "1"},
                {"let inputText = event.data.nickname;",
                        "let inputText = legacyConnectorAlienEvoPacketString(event.data.nickname);", "1"}
        };

        boolean relevant = false;
        for (String[] replacement : replacements) {
            if (script.contains(replacement[0]) || script.contains(replacement[1])) {
                relevant = true;
                break;
            }
        }
        if (!relevant) return 0;

        if (!script.contains(ALIEN_EVO_PACKET_STRING_HELPER_MARKER)) {
            String lineEnding = script.contains("\r\n") ? "\r\n" : "\n";
            script = ALIEN_EVO_PACKET_STRING_HELPER.replace("\n", lineEnding) + script;
            resources.put(ALIEN_EVO_SERVER_STUFF, script.getBytes(StandardCharsets.UTF_8));
        }

        return rewrite(resources, ALIEN_EVO_SERVER_STUFF, replacements);
    }

    /**
     * KubeJS 7 remaps DamageSource.getEntity() to the Rhino-visible getActual()
     * method. Read the causing entity through that alias, then keep the player
     * attribution guard so environmental deaths do not award attacker XP.
     * method; using it silently breaks AlienEvo's player-kill XP progression.
     */
    private static int rewriteAlienEvoXpDeathPlayerAccess(Map<String, byte[]> resources) {
        String path = "data/alienevo/kubejs_scripts/xp.js";
        byte[] original = resources.get(path);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        String lineEnding = script.contains("\r\n") ? "\r\n" : "\n";
        String legacy = "if (source && source.player) {" + lineEnding
                + "        let player = source.player;";
        String migrated = "let player = source ? source.getActual() : null;" + lineEnding
                + "    if (player && player.isPlayer()) {";
        return rewrite(resources, path, new String[][] {{legacy, migrated, "1"}});
    }

    /**
     * Minecraft 1.21.1 changed parameterized particle arguments to data-component/SNBT syntax.
     * The addon Cannonbolt feet emitter used the 1.20.1 whitespace form, which KubeJS 7
     * rejects while converting the string argument to ParticleOptions. Preserve the block dust
     * trail with the modern block_state form and keep valid falling-dust/item fallbacks.
     */
    private static int rewriteAlienEvoBlockParticleOptions(Map<String, byte[]> resources) {
        byte[] original = resources.get(ALIEN_EVO_BLOCK_PARTICLES);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        String lineEnding = script.contains("\r\n") ? "\r\n" : "\n";
        // The shipped AlienEvo 1.1.3 script uses Windows CRLF. Match normalized
        // text so the migration works regardless of the archive's line endings.
        String normalized = script.replace("\r\n", "\n");
        String legacy = String.join("\n",
                "                    let formats = [",
                "                        \"minecraft:block \" + block.id,        // Standard format",
                "                        \"block \" + block.id,                  // Alternative format",
                "                        \"minecraft:falling_dust \" + block.id, // Falling dust format",
                "                        block.id + \"_break\",                  // Break particle format",
                "                        \"minecraft:item \" + block.id          // Item particle format",
                "                    ];",
                "");
        String migrated = String.join("\n",
                "                    let blockState = JSON.stringify(String(block.id));",
                "                    let formats = [",
                "                        `minecraft:block{block_state:${blockState}}`,",
                "                        `minecraft:falling_dust{block_state:${blockState}}`,",
                "                        `minecraft:item{item:${blockState}}`",
                "                    ];",
                "");

        int legacyCount = occurrences(normalized, legacy);
        int migratedCount = occurrences(normalized, migrated);
        if (legacyCount == 0 && migratedCount == 1) return 0;
        if (legacyCount != 1 || migratedCount != 0) {
            throw new IllegalStateException(
                    "AlienEvo resource " + ALIEN_EVO_BLOCK_PARTICLES
                            + " has an unexpected Cannonbolt block-particle rewrite shape: expected "
                            + "1 legacy occurrence, found " + legacyCount + " legacy and "
                            + migratedCount + " migrated");
        }

        String rewritten = normalized.replace(legacy, migrated);
        resources.put(ALIEN_EVO_BLOCK_PARTICLES,
                rewritten.replace("\n", lineEnding).getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    /**
     * Minecraft 1.21.1 renamed Entity#setSecondsOnFire(int) to
     * Entity#igniteForSeconds(float). Migrate every inspected AlienEvo/AFOMNI
     * script call so rolling, directional, targeted, and area damage do not
     * crash when the affected entity is a Sheep or another LivingEntity.
     */
    private static int rewriteAlienEvoSetSecondsOnFire(Map<String, byte[]> resources) {
        int changed = 0;
        changed += rewrite(resources, AFOMNI_AOE_DAMAGE, new String[][] {{
                "targets[j].setSecondsOnFire(firetick);",
                "targets[j].igniteForSeconds(firetick);",
                "1"
        }});
        changed += rewrite(resources, ALIEN_EVO_DIRECTIONAL_DAMAGE, new String[][] {{
                "targets[j].setSecondsOnFire(firetick);",
                "targets[j].igniteForSeconds(firetick);",
                "1"
        }});
        changed += rewrite(resources, ALIEN_EVO_ENTITY_DAMAGE, new String[][] {{
                "target.setSecondsOnFire(fire_seconds)",
                "target.igniteForSeconds(fire_seconds)",
                "2"
        }});
        changed += rewrite(resources, ALIEN_EVO_INFINITY, new String[][] {{
                "entities[i].setSecondsOnFire(fireSeconds);",
                "entities[i].igniteForSeconds(fireSeconds);",
                "1"
        }});
        changed += rewrite(resources, ALIEN_EVO_ROLLING_MECHANICS, new String[][] {{
                "targets[j].setSecondsOnFire(firetick);",
                "targets[j].igniteForSeconds(firetick);",
                "1"
        }});
        return changed;
    }

    /**
     * KubeJS 7 exposes CompoundTag numeric values consistently through getInt/putInt.
     * The Quick Change wheel used JavaScript bracket-property access for the same keys,
     * unlike its server Quick Change handler. Normalize playlist/slot reads and writes
     * so selected DNA is compared and stored as an integer on every path.
     */
    private static int rewriteAlienEvoSlotPersistentDataIntegers(Map<String, byte[]> resources) {
        return rewrite(resources, ALIEN_EVO_SLOT_TRANSFORM, new String[][] {
                {"let currentPlaylist = entity.persistentData[CURRENT_PLAYLIST_KEY] || DEFAULT_PLAYLIST;",
                        "let currentPlaylist = entity.persistentData.getInt(CURRENT_PLAYLIST_KEY) || DEFAULT_PLAYLIST;", "1"},
                {"let alienNumber = entity.persistentData[`alienevo.alien_${currentPlaylist}_${useSlot}`];",
                        "let alienNumber = entity.persistentData.getInt(`alienevo.alien_${currentPlaylist}_${useSlot}`);", "1"},
                {"entity.persistentData[CURRENT_ALIEN_SLOT_KEY] = useSlot;",
                        "entity.persistentData.putInt(CURRENT_ALIEN_SLOT_KEY, useSlot);", "1"},
                {"let current_playlist = entity.persistentData[CURRENT_PLAYLIST_KEY] || DEFAULT_PLAYLIST;",
                        "let current_playlist = entity.persistentData.getInt(CURRENT_PLAYLIST_KEY) || DEFAULT_PLAYLIST;", "1"},
                {"let current_slot = entity.persistentData[CURRENT_ALIEN_SLOT_KEY] || DEFAULT_SLOT;",
                        "let current_slot = entity.persistentData.getInt(CURRENT_ALIEN_SLOT_KEY) || DEFAULT_SLOT;", "1"},
                {"let selectedAlienNum = entity.persistentData[`alienevo.alien_${current_playlist}_${current_slot}`] || 0;",
                        "let selectedAlienNum = entity.persistentData.getInt(`alienevo.alien_${current_playlist}_${current_slot}`) || 0;", "1"},
                {"alien_num = entity.persistentData[`alienevo.alien_${random_playlist}_${random_slot}`] || 0;",
                        "alien_num = entity.persistentData.getInt(`alienevo.alien_${random_playlist}_${random_slot}`) || 0;", "1"},
                {"entity.persistentData[CURRENT_PLAYLIST_KEY] = random_playlist;",
                        "entity.persistentData.putInt(CURRENT_PLAYLIST_KEY, random_playlist);", "1"},
                {"entity.persistentData[CURRENT_ALIEN_SLOT_KEY] = random_slot;",
                        "entity.persistentData.putInt(CURRENT_ALIEN_SLOT_KEY, random_slot);", "1"},
                {"let alienInSlot = entity.persistentData[`alienevo.alien_${playlist}_${slot}`];",
                        "let alienInSlot = entity.persistentData.getInt(`alienevo.alien_${playlist}_${slot}`);", "1"}
        });
    }
    /**
     * AlienEvo's recharge callback skipped the final cooldown tick when the
     * scoreboard reached exactly 1. The transform ability remains locked while
     * AlienEvo.Timer is 1..3000, so that off-by-one leaves the Omnitrix disabled
     * indefinitely. Permit the final decrement to clear a one-tick cooldown.
     */
    private static int rewriteAlienEvoRechargeCooldown(Map<String, byte[]> resources) {
        return rewrite(resources, ALIEN_EVO_RECHARGE, new String[][] {{
                "if (currentTime > 1) {", "if (currentTime > 0) {", "1"
        }});
    }

    /**
     * 1.20.1 AlienEvo stores AttributeModifier.Operation values using the old
     * labels addition/multiply_base/multiply_total. Minecraft 1.21.1 renamed
     * those enum constants to add_value/add_multiplied_base/add_multiplied_total.
     * Normalize the property before passing it to KubeJS' Entity.modifyAttribute,
     * otherwise Rhino rejects the old string while ticking a stat ability.
     */
    private static int rewriteAlienEvoStatAbilityOperation(Map<String, byte[]> resources) {
        byte[] original = resources.get(ALIEN_EVO_STAT_ABILITY);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        String legacyRead = "const attribute_mod = entry.getPropertyByName('Attribute_Mod');";
        String migratedRead =
                "const attribute_mod = legacyConnectorAttributeOperation(entry.getPropertyByName('Attribute_Mod'));";
        int legacyReads = occurrences(script, legacyRead);
        int migratedReads = occurrences(script, migratedRead);

        String legacyDefault =
                ".addProperty(\"Attribute_Mod\", \"string\", \"addition\", \"addition , multiply_total , multiply_base\")";
        String migratedDefault =
                ".addProperty(\"Attribute_Mod\", \"string\", \"add_value\", "
                        + "\"add_value , add_multiplied_total , add_multiplied_base\")";
        int legacyDefaults = occurrences(script, legacyDefault);
        int migratedDefaults = occurrences(script, migratedDefault);
        boolean helperPresent = script.contains("function legacyConnectorAttributeOperation(operation) {");

        // Focused compatibility tests may include only the scoreboard portion of
        // stat_ability.js. If this script excerpt has no Attribute_Mod operation
        // markers at all, the operation migration is not relevant to this resource.
        if (legacyReads == 0 && migratedReads == 0
                && legacyDefaults == 0 && migratedDefaults == 0 && !helperPresent) {
            return 0;
        }
        if (legacyReads == 0 && migratedReads == 2
                && legacyDefaults == 0 && migratedDefaults == 1 && helperPresent) {
            return 0;
        }
        if (legacyReads != 2 || migratedReads != 0
                || legacyDefaults > 1 || migratedDefaults > 1
                || (legacyDefaults == 0 && migratedDefaults != 0)
                || (legacyDefaults == 1 && migratedDefaults != 0)) {
            throw new IllegalStateException(
                    "AlienEvo stat_ability.js has an unexpected Attribute_Mod operation migration shape: "
                            + legacyReads + " legacy reads, " + migratedReads + " migrated reads, "
                            + legacyDefaults + " legacy defaults, " + migratedDefaults + " migrated defaults");
        }

        String lineEnding = script.contains("\r\n") ? "\r\n" : "\n";
        String migrated = script.replace(legacyRead, migratedRead);
        if (legacyDefaults == 1) migrated = migrated.replace(legacyDefault, migratedDefault);
        if (!helperPresent) {
            migrated = LEGACY_ATTRIBUTE_OPERATION_HELPER.replace("\n", lineEnding) + migrated;
        }
        resources.put(ALIEN_EVO_STAT_ABILITY, migrated.getBytes(StandardCharsets.UTF_8));
        return 1;
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
        String noCooldownMarker = indent + "    if (currentTimerValue <= 1) {";
        String noObjectiveMarker = indent + "} else {" + lineEnding
                + indent + "    palladium.setProperty(entity, 'watch_state', \"default\");";
        if (script.contains(noCooldownMarker) && script.contains(noObjectiveMarker)) return script;

        String timerWrite = "entity.server.runCommandSilent(`execute as ${playerUsername} at @s run scoreboard players set @s AlienEvo.Timer ${Math.floor(currentTimerValue / 2)}`);";
        int timerWriteIndex = script.indexOf(timerWrite, declarationEnd);
        if (timerWriteIndex < 0 || occurrences(script, timerWrite) != 1) {
            throw new IllegalStateException("AlienEvo detransform.js has an unexpected timer write shape");
        }
        int timerWriteEnd = script.indexOf('\n', timerWriteIndex);
        if (timerWriteEnd < 0) timerWriteEnd = script.length();
        else timerWriteEnd++;

        String timerStatements = script.substring(declarationEnd, timerWriteEnd);
        String indentedStatements = indentScriptBlock(timerStatements, indent, "    ");
        String resetDefault = indent + "    palladium.setProperty(entity, 'watch_state', \"default\");";
        String migratedBlock = guard + lineEnding
                + indentedStatements
                + indent + "    if (currentTimerValue <= 1) {" + lineEnding
                + resetDefault + lineEnding
                + indent + "    }" + lineEnding
                + indent + "} else {" + lineEnding
                + resetDefault + lineEnding
                + indent + "}" + lineEnding;

        return script.substring(0, declarationEnd)
                + migratedBlock
                + script.substring(timerWriteEnd);
    }

    /** Adds one indentation level to each nonblank line in a script block. */
    private static String indentScriptBlock(String block, String baseIndent, String additionalIndent) {
        StringBuilder indented = new StringBuilder(block.length() + 32);
        int start = 0;
        while (start < block.length()) {
            int newline = block.indexOf('\n', start);
            int end = newline < 0 ? block.length() : newline + 1;
            String line = block.substring(start, end);
            if (!line.isBlank() && line.startsWith(baseIndent)) {
                indented.append(baseIndent).append(additionalIndent)
                        .append(line.substring(baseIndent.length()));
            } else if (!line.isBlank()) {
                indented.append(additionalIndent).append(line);
            } else {
                indented.append(line);
            }
            start = end;
        }
        return indented.toString();
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

    private static String addSoundSourceHelper(String script) {
        if (script.contains(SOUND_SOURCE_HELPER_MARKER)) return script;
        return insertAfterOmnitrixIntegerHelper(script, LEGACY_SOUND_SOURCE_HELPER);
    }

    private static String replaceExactScriptOccurrence(
            String path, String script, String legacy, String safe) {
        int legacyCount = occurrences(script, legacy);
        int safeCount = occurrences(script, safe);
        if (legacyCount == 0 && safeCount == 1) return script;
        if (legacyCount != 1 || safeCount != 0) {
            throw new IllegalStateException(
                    "AlienEvo resource " + path + " has an unexpected compatibility rewrite shape: expected "
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

            // Focused tests may include only the portion of a known script
            // relevant to a different adapter. No legacy or migrated token
            // means this particular rewrite is absent from that resource slice;
            // partial or mixed migrations still fail below.
            if (legacyCount == 0 && safeCount == 0) continue;
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
