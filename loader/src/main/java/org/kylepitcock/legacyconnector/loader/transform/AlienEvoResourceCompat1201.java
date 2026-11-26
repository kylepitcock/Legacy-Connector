package org.kylepitcock.legacyconnector.loader.transform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Evidence-based resource repairs for AlienEvo, including its 1.21.1
 * unshaded-entity shader migration, malformed Kineceleran animation cleanup,
 * optional Ad Astra tag entry, and hidden test-power cleanup.
 */
final class AlienEvoResourceCompat1201 {
    static final String ORPHAN_CODE_POWER =
            "data/alienevo/palladium/powers/code_power_test.json";
    static final String EVOASTRA_DAMAGE_TAG =
            "data/alienevo/tags/damage_type/evoastra.json";
    static final String ADDON_RESOURCE_ROOT_MARKER = "addon/.mcassetsroot";
    static final String KINECELERAN_ANIMATION =
            "assets/alienevo/animations/aliens/kineceleran/kineceleran.animation.json";
    static final String UNSHADED_ENTITY_SHADER_JSON =
            "assets/minecraft/shaders/core/alienevo_rendertype_entity_unshaded.json";
    static final String UNSHADED_ENTITY_SHADER_VERTEX =
            "assets/minecraft/shaders/core/alienevo_rendertype_entity_unshaded.vsh";
    static final String LEGACY_UNSHADED_FOG_DISTANCE =
            "vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);";
    static final String MODERN_UNSHADED_FOG_DISTANCE =
            "vertexDistance = fog_distance(Position, FogShape);";
    private static final String LEGACY_VIEW_ROTATION_UNIFORM = "uniform mat3 IViewRotMat;";

    private AlienEvoResourceCompat1201() {}

    /**
     * Remove only the exact hidden test power when its two unregistered types
     * and the power itself have no references from any other resource/class.
     * A future AlienEvo build that makes it real or references it is preserved.
     */
    static boolean removeUnreferencedHiddenCodePower(
            Map<String, byte[]> resources, Map<String, byte[]> classes) {
        byte[] original = resources.get(ORPHAN_CODE_POWER);
        if (original == null) return false;

        JsonObject power;
        try {
            JsonElement parsed = JsonParser.parseString(new String(original, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return false;
            power = parsed.getAsJsonObject();
        } catch (RuntimeException ignored) {
            return false;
        }

        if (!power.has("hidden") || !power.get("hidden").isJsonPrimitive()
                || !power.get("hidden").getAsBoolean()) return false;

        JsonObject abilities = power.has("abilities") && power.get("abilities").isJsonObject()
                ? power.getAsJsonObject("abilities") : null;
        if (abilities == null
                || !"alienevo:generate_random".equals(abilityType(abilities, "generate_code"))
                || !"alienevo:check_code".equals(abilityType(abilities, "check_code"))) {
            return false;
        }

        if (containsOutside(resources, ORPHAN_CODE_POWER, "code_power_test")
                || containsOutside(resources, ORPHAN_CODE_POWER, "alienevo:generate_random")
                || containsOutside(resources, ORPHAN_CODE_POWER, "alienevo:check_code")
                || contains(classes, "code_power_test")
                || contains(classes, "alienevo:generate_random")
                || contains(classes, "alienevo:check_code")) {
            return false;
        }

        resources.remove(ORPHAN_CODE_POWER);
        return true;
    }

    /**
     * The tag provides an optional cross-mod damage type, not a mandatory
     * AlienEvo dependency. Preserve the entry when Ad Astra is installed, but
     * let NeoForge skip it cleanly when the registry entry is unavailable.
     */
    static boolean makeAdAstraDamageTypeOptional(Map<String, byte[]> resources) {
        byte[] original = resources.get(EVOASTRA_DAMAGE_TAG);
        if (original == null) return false;
        try {
            JsonElement parsed = JsonParser.parseString(new String(original, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return false;
            JsonObject root = parsed.getAsJsonObject();
            if (!root.has("values") || !root.get("values").isJsonArray()) return false;
            JsonArray values = root.getAsJsonArray("values");
            boolean changed = false;
            for (int i = 0; i < values.size(); i++) {
                JsonElement value = values.get(i);
                if (value.isJsonPrimitive() && value.getAsString().equals("ad_astra:oxygen")) {
                    JsonObject optional = new JsonObject();
                    optional.addProperty("id", "ad_astra:oxygen");
                    optional.addProperty("required", false);
                    values.set(i, optional);
                    changed = true;
                }
            }
            if (changed) resources.put(EVOASTRA_DAMAGE_TAG, root.toString().getBytes(StandardCharsets.UTF_8));
            return changed;
        } catch (RuntimeException ignored) {
            return false;
        }
    }


    /**
     * AlienEvo dev.24 has one malformed GeckoLib keyframe: kick_stop / armorHead
     * places the easing name in all three position vector components. Easing
     * belongs on the keyframe's "easing" property, not in a numeric vector.
     * Remove only that unusable position channel and preserve the valid rotation
     * channel and all other animations.
     */
    static boolean repairKineceleranKickStopPositionVector(Map<String, byte[]> resources) {
        byte[] original = resources.get(KINECELERAN_ANIMATION);
        if (original == null) return false;

        try {
            JsonElement parsed = JsonParser.parseString(new String(original, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return false;
            JsonObject root = parsed.getAsJsonObject();
            JsonObject animations = getObject(root, "animations");
            JsonObject kickStop = getObject(animations, "animation.kineceleran.kick_stop");
            JsonObject bones = getObject(kickStop, "bones");
            JsonObject armorHead = getObject(bones, "armorHead");
            JsonObject position = getObject(armorHead, "position");
            if (position == null || position.entrySet().size() != 1
                    || !position.has("vector") || !position.get("vector").isJsonArray()) return false;

            JsonArray vector = position.getAsJsonArray("vector");
            if (vector.size() != 3) return false;
            for (JsonElement component : vector) {
                if (!component.isJsonPrimitive() || !component.getAsJsonPrimitive().isString()
                        || !"easeInElastic".equalsIgnoreCase(component.getAsString())) return false;
            }

            armorHead.remove("position");
            resources.put(KINECELERAN_ANIMATION, root.toString().getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }


    /**
     * AlienEvo's 1.20.1 unshaded entity shader uses the old fog-distance
     * overload and IViewRotMat transform. In 1.21.1, entity Position is already
     * camera-relative and fog_distance accepts (vec3, int). Rewrite the vertex
     * source and its shader JSON together so the removed uniform is not bound.
     *
     * This is deliberately fail-closed: only the exact dev.24 shader pair and
     * expected IViewRotMat declaration/uniform are changed.
     */
    static boolean repairUnshadedEntityShaderFor121(Map<String, byte[]> resources) {
        byte[] jsonBytes = resources.get(UNSHADED_ENTITY_SHADER_JSON);
        byte[] vertexBytes = resources.get(UNSHADED_ENTITY_SHADER_VERTEX);
        if (jsonBytes == null || vertexBytes == null) return false;

        String vertex = new String(vertexBytes, StandardCharsets.UTF_8);
        if (countOccurrences(vertex, LEGACY_UNSHADED_FOG_DISTANCE) != 1
                || countOccurrences(vertex, LEGACY_VIEW_ROTATION_UNIFORM) != 1
                || countOccurrences(vertex, "IViewRotMat") != 2
                || countOccurrences(vertex, "fog_distance(") != 1) {
            return false;
        }

        JsonObject shaderJson;
        try {
            JsonElement parsed = JsonParser.parseString(new String(jsonBytes, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return false;
            shaderJson = parsed.getAsJsonObject();
        } catch (RuntimeException ignored) {
            return false;
        }

        if (!shaderJson.has("vertex") || !shaderJson.get("vertex").isJsonPrimitive()
                || !"alienevo_rendertype_entity_unshaded".equals(shaderJson.get("vertex").getAsString())
                || !shaderJson.has("uniforms") || !shaderJson.get("uniforms").isJsonArray()) {
            return false;
        }

        JsonArray uniforms = shaderJson.getAsJsonArray("uniforms");
        int viewRotationIndex = -1;
        for (int i = 0; i < uniforms.size(); i++) {
            JsonElement element = uniforms.get(i);
            if (!element.isJsonObject()) continue;
            JsonObject uniform = element.getAsJsonObject();
            if (!uniform.has("name") || !uniform.get("name").isJsonPrimitive()
                    || !"IViewRotMat".equals(uniform.get("name").getAsString())) {
                continue;
            }
            if (viewRotationIndex >= 0
                    || !uniform.has("type") || !uniform.get("type").isJsonPrimitive()
                    || !"matrix3x3".equals(uniform.get("type").getAsString())
                    || !uniform.has("count") || !uniform.get("count").isJsonPrimitive()
                    || !uniform.get("count").getAsJsonPrimitive().isNumber()
                    || uniform.get("count").getAsInt() != 9) {
                return false;
            }
            viewRotationIndex = i;
        }
        if (viewRotationIndex < 0) return false;

        String migratedVertex = vertex
                .replace(LEGACY_UNSHADED_FOG_DISTANCE, MODERN_UNSHADED_FOG_DISTANCE)
                .replace(LEGACY_VIEW_ROTATION_UNIFORM, "");
        if (migratedVertex.contains("IViewRotMat")
                || countOccurrences(migratedVertex, MODERN_UNSHADED_FOG_DISTANCE) != 1) {
            return false;
        }

        uniforms.remove(viewRotationIndex);
        resources.put(UNSHADED_ENTITY_SHADER_JSON,
                shaderJson.toString().getBytes(StandardCharsets.UTF_8));
        resources.put(UNSHADED_ENTITY_SHADER_VERTEX,
                migratedVertex.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(token, from)) >= 0) {
            count++;
            from += token.length();
        }
        return count;
    }

    private static JsonObject getObject(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) return null;
        return parent.getAsJsonObject(key);
    }

    /**
     * AlienEvo stores most client assets under a nonstandard {@code addon/}
     * root. Vanilla's 1.21.1 pack loader expects a marker at that root when
     * discovering it as a resource namespace.
     */
    static boolean ensureAddonResourceRootMarker(Map<String, byte[]> resources) {
        boolean hasAddonRoot = resources.keySet().stream()
                .anyMatch(path -> path.startsWith("addon/") && !path.equals(ADDON_RESOURCE_ROOT_MARKER));
        if (!hasAddonRoot || resources.containsKey(ADDON_RESOURCE_ROOT_MARKER)) return false;
        resources.put(ADDON_RESOURCE_ROOT_MARKER, new byte[0]);
        return true;
    }



    /**
     * Repairs resource identifiers and schemas verified against AlienEvo dev.24
     * and Palladium 4.5.10. This is intentionally scoped to the exact files and
     * bytecode-era identifiers observed in the 2026-11-26 runtime diagnostics.
     *
     * <p>Some dev.24 power types are referenced by JSON but are not registered by
     * the supplied AlienEvo JAR. Those individual ability entries are removed
     * rather than mapped to unrelated implementations that would change their
     * behavior silently.</p>
     */
    static int repairDev24ResourceErrors(Map<String, byte[]> resources) {
        int changed = 0;
        changed += repairCodeTabletLoot(resources);
        changed += repairPrototypeCoreTag(resources);
        changed += repairPrototypeOmnitrixRecipe(resources);
        changed += repairAerophibianWatchConditions(resources);
        changed += repairUnsupportedPowerTypes(resources);
        changed += repairLegacyIceFunctions(resources);
        changed += repairPyroniteNovaFunction(resources);
        changed += repairMissingVisualResourceAssets(resources);
        // Vanilla 1.21.1 discovers resources below the nonstandard addon/ root
        // only when the root marker is present in the translated resource pack.
        changed += ensureAddonResourceRootMarker(resources) ? 1 : 0;
        return changed;
    }

    private static int repairCodeTabletLoot(Map<String, byte[]> resources) {
        String path = "data/alienevo/loot_tables/archaeology/codes_rare.json";
        JsonObject root = parseObject(resources.get(path));
        if (root == null || !root.has("pools") || !root.get("pools").isJsonArray()) return 0;
        JsonArray pools = root.getAsJsonArray("pools");
        boolean changed = false;
        for (JsonElement poolElement : pools) {
            if (!poolElement.isJsonObject()) continue;
            JsonObject pool = poolElement.getAsJsonObject();
            if (!pool.has("entries") || !pool.get("entries").isJsonArray()) continue;
            JsonArray entries = pool.getAsJsonArray("entries");
            JsonArray repaired = new JsonArray();
            boolean replaced = false;
            for (JsonElement entryElement : entries) {
                if (entryElement.isJsonObject()) {
                    JsonObject entry = entryElement.getAsJsonObject();
                    if ("minecraft:item".equals(string(entry, "type"))
                            && "alienevo:code_tablet".equals(string(entry, "name"))) {
                        for (String item : new String[] {
                                "alienevo:code_tablet_mastercontrol_on",
                                "alienevo:code_tablet_mastercontrol_off",
                                "alienevo:code_tablet_selfdestruct_on",
                                "alienevo:code_tablet_selfdestruct_off"
                        }) {
                            JsonObject replacement = new JsonObject();
                            replacement.addProperty("type", "minecraft:item");
                            replacement.addProperty("name", item);
                            repaired.add(replacement);
                        }
                        replaced = true;
                        changed = true;
                        continue;
                    }
                }
                repaired.add(entryElement.deepCopy());
            }
            if (replaced) pool.add("entries", repaired);
        }
        if (!changed) return 0;
        resources.put(path, root.toString().getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    private static int repairPrototypeCoreTag(Map<String, byte[]> resources) {
        String path = "data/alienevo/tags/items/prototype_cores.json";
        JsonObject root = parseObject(resources.get(path));
        if (root == null || !root.has("values") || !root.get("values").isJsonArray()) return 0;
        JsonArray values = root.getAsJsonArray("values");
        boolean changed = false;
        for (int i = 0; i < values.size(); i++) {
            JsonElement value = values.get(i);
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                    && "alienevo:prototype_core".equals(value.getAsString())) {
                values.set(i, new JsonPrimitive("alienevo:prototype_omnitrix_core"));
                changed = true;
            }
        }
        if (!changed) return 0;
        resources.put(path, root.toString().getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    private static int repairPrototypeOmnitrixRecipe(Map<String, byte[]> resources) {
        String path = "data/alienevo/recipes/prototype_omnitrix.json";
        JsonObject root = parseObject(resources.get(path));
        if (root == null || !root.has("result") || !root.get("result").isJsonObject()) return 0;
        JsonObject result = root.getAsJsonObject("result");
        if (result.has("id") || !result.has("item")
                || !"alienevo:prototype_omnitrix".equals(string(result, "item"))) return 0;
        result.add("id", result.remove("item"));
        resources.put(path, root.toString().getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    /**
     * The JAR has watch_state (enum values DEFAULT/RECALIBRATED), not watch_type.
     * The prototype-only base layer becomes the unconditional fallback; the AF
     * texture and glow layer use the actual RECALIBRATED watch state.
     */
    private static int repairAerophibianWatchConditions(Map<String, byte[]> resources) {
        String path = "assets/alienevo/palladium/render_layers/aliens/alien_34/aerophibian.json";
        JsonObject root = parseObject(resources.get(path));
        if (root == null || !root.has("layers") || !root.get("layers").isJsonArray()) return 0;
        JsonArray layers = root.getAsJsonArray("layers");
        if (layers.size() != 3) return 0;
        boolean changed = false;
        for (int i = 0; i < layers.size(); i++) {
            if (!layers.get(i).isJsonObject()) return 0;
            JsonObject layer = layers.get(i).getAsJsonObject();
            if (!layer.has("conditions") || !layer.get("conditions").isJsonArray()) return 0;
            JsonArray conditions = layer.getAsJsonArray("conditions");
            if (conditions.size() != 1 || !conditions.get(0).isJsonObject()) return 0;
            JsonObject condition = conditions.get(0).getAsJsonObject();
            if (!"alienevo:watch_type".equals(string(condition, "type"))) return 0;
            String value = string(condition, "value");
            if (i == 0 && "prototype".equals(value)) {
                layer.remove("conditions");
                changed = true;
            } else if ((i == 1 || i == 2) && "recal".equals(value)) {
                condition.addProperty("type", "alienevo:watch_state");
                condition.remove("value");
                condition.addProperty("state", "RECALIBRATED");
                changed = true;
            } else {
                return 0;
            }
        }
        if (!changed) return 0;
        resources.put(path, root.toString().getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    private static int repairUnsupportedPowerTypes(Map<String, byte[]> resources) {
        int changed = 0;
        changed += repairPower(resources,
                "data/alienevo_aliens/palladium/powers/dragonoid.json",
                Map.of(
                        "infinity", "alienevo:infinity",
                        "fire_shield_particle", "alienevo:particle_ring"));
        changed += repairPower(resources,
                "data/alienevo_aliens/palladium/powers/vaxasaurian.json",
                Map.of("leap", "afomni:vax_leap"));
        changed += repairPower(resources,
                "data/alienevo_aliens/palladium/powers/astrobot.json",
                Map.of(
                        "laser_jump", "alienevo:astrojump",
                        "astro_laser_damage", "alienevo:astro_laser_damage",
                        "punch_ability_ground", "alienevo:astro_punch_damage",
                        "punch_ability_air", "alienevo:astro_punch_air_damage",
                        "testanim", "alienevo:astro_chargepunch"));
        changed += repairPower(resources,
                "data/alienevo_aliens/palladium/powers/kryptonian.json", Map.of());
        changed += repairPower(resources,
                "data/alienevo/palladium/powers/galaxy_aurora.json",
                Map.of("infinity", "alienevo:infinity"));
        return changed;
    }

    private static int repairPower(Map<String, byte[]> resources, String path,
                                   Map<String, String> unsupportedAbilities) {
        JsonObject root = parseObject(resources.get(path));
        if (root == null || !root.has("abilities") || !root.get("abilities").isJsonObject()) return 0;
        String before = root.toString();
        rewritePowerIdentifiers(root);
        JsonObject abilities = root.getAsJsonObject("abilities");
        for (Map.Entry<String, String> unsupported : unsupportedAbilities.entrySet()) {
            JsonElement candidate = abilities.get(unsupported.getKey());
            if (candidate != null && candidate.isJsonObject()
                    && unsupported.getValue().equals(string(candidate.getAsJsonObject(), "type"))) {
                abilities.remove(unsupported.getKey());
            }
        }
        if (path.endsWith("/galaxy_aurora.json")) {
            replaceUnsupportedEntityDamageWithCommand(abilities);
        }
        String after = root.toString();
        if (before.equals(after)) return 0;
        resources.put(path, after.getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    /** Namespace aliases confirmed by the AlienEvo dev.24 registries. */
    private static void rewritePowerIdentifiers(JsonElement element) {
        if (element == null) return;
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (String key : new java.util.ArrayList<>(object.keySet())) {
                JsonElement value = object.get(key);
                if (value != null && value.isJsonPrimitive()
                        && value.getAsJsonPrimitive().isString()) {
                    String source = value.getAsString();
                    String mapped = switch (source) {
                        case "afomni:is_mainhand" -> "alienevo:is_mainhand";
                        case "afomni:aoe_damage" -> "alienevo:aoe_damage";
                        case "alienevo:screenshake" -> "alienevo:screen_shake";
                        default -> source;
                    };
                    if (!mapped.equals(source)) object.addProperty(key, mapped);
                } else {
                    rewritePowerIdentifiers(value);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonElement value = array.get(i);
                if (value != null && value.isJsonPrimitive()
                        && value.getAsJsonPrimitive().isString()) {
                    String source = value.getAsString();
                    String mapped = switch (source) {
                        case "afomni:is_mainhand" -> "alienevo:is_mainhand";
                        case "afomni:aoe_damage" -> "alienevo:aoe_damage";
                        case "alienevo:screenshake" -> "alienevo:screen_shake";
                        default -> source;
                    };
                    if (!mapped.equals(source)) array.set(i, new JsonPrimitive(mapped));
                } else {
                    rewritePowerIdentifiers(value);
                }
            }
        }
    }

    /**
     * The referenced AFOmni entity_damage ability is absent from this AlienEvo
     * JAR. Preserve the heal key and its enabling condition using Palladium's
     * server-side CommandAbility to renew the same regeneration effect while
     * the source ability remains enabled.
     */
    private static void replaceUnsupportedEntityDamageWithCommand(JsonObject abilities) {
        JsonElement current = abilities.get("heal_regen");
        if (current == null || !current.isJsonObject()) return;
        JsonObject old = current.getAsJsonObject();
        if (!"alienevo:entity_damage".equals(string(old, "type"))) return;
        JsonObject replacement = new JsonObject();
        replacement.addProperty("type", "palladium:command");
        for (String key : new String[] {
                "hidden", "hidden_in_bar", "list_index", "gui_position",
                "title", "icon", "description", "conditions"
        }) {
            if (old.has(key)) replacement.add(key, old.get(key).deepCopy());
        }
        JsonArray commands = new JsonArray();
        commands.add("effect give @s minecraft:regeneration 5 5 true");
        replacement.add("commands", commands);
        abilities.add("heal_regen", replacement);
    }

    private static int repairLegacyIceFunctions(Map<String, byte[]> resources) {
        int changed = 0;
        changed += rewriteTextResource(resources,
                "data/afomni/functions/creation/upgrade_fade.mcfunction",
                new String[][] {
                        {"afomni:packed_ice", "alienevo:packed_ice_layer"},
                        {"afomni:blue_ice", "alienevo:blue_ice_layer"},
                        {"afomni:ice", "alienevo:ice_layer"}
                });
        changed += rewriteTextResource(resources,
                "data/afomni/functions/creation/place_ice_fade.mcfunction",
                new String[][] {
                        {"afomni:blue_ice", "alienevo:blue_ice_layer"},
                        {"afomni:ice", "alienevo:ice_layer"}
                });
        changed += rewriteTextResource(resources,
                "data/alienevo/functions/abilities/stack_ice.mcfunction",
                new String[][] {
                        {"alienevo:ice_block", "alienevo:ice_layer[layers=8]"},
                        {"alienevo:ice14", "alienevo:ice_layer[layers=7]"},
                        {"alienevo:ice12", "alienevo:ice_layer[layers=6]"},
                        {"alienevo:ice10", "alienevo:ice_layer[layers=5]"},
                        {"alienevo:ice8", "alienevo:ice_layer[layers=4]"},
                        {"alienevo:ice6", "alienevo:ice_layer[layers=3]"},
                        {"alienevo:ice4", "alienevo:ice_layer[layers=2]"},
                        {"alienevo:ice", "alienevo:ice_layer[layers=1]"}
                });
        return changed;
    }

    private static int repairPyroniteNovaFunction(Map<String, byte[]> resources) {
        return rewriteTextResource(resources,
                "data/alienevo/functions/abilities/pyronite_nova.mcfunction",
                new String[][] {
                        {
                                "execute if score @s Pyronite.Nova matches 35 run fireparticle ^ ^2 ^ 6500 0 0 0 0.3",
                                "execute if score @s Pyronite.Nova matches 35 run particle minecraft:flame ^ ^2 ^ 0 0 0 0.3 6500"
                        },

                });
    }

    private static int rewriteTextResource(Map<String, byte[]> resources, String path, String[][] replacements) {
        byte[] original = resources.get(path);
        if (original == null) return 0;
        String source = new String(original, StandardCharsets.UTF_8);
        String result = source;
        for (String[] pair : replacements) {
            if (pair.length != 2) continue;
            // Match a complete resource ID only; e.g. rewriting alienevo:ice must
            // not corrupt the generated alienevo:ice_layer[state] identifiers.
            String expression = java.util.regex.Pattern.quote(pair[0])
                    + "(?![a-zA-Z0-9_./-])";
            result = java.util.regex.Pattern.compile(expression)
                    .matcher(result)
                    .replaceAll(java.util.regex.Matcher.quoteReplacement(pair[1]));
        }
        if (result.equals(source)) return 0;
        resources.put(path, result.getBytes(StandardCharsets.UTF_8));
        return 1;
    }



    /**
     * Repairs missing dev.24 visual assets only when the inspected AlienEvo
     * crystal models are present. The layer block geometry mirrors IceLayerBlock
     * exactly: one 2*layers/16 slab on the side named by its FACING state.
     */
    static int repairMissingVisualResourceAssets(Map<String, byte[]> resources) {
        if (!resources.containsKey("assets/alienevo/models/block/crystal.json")
                || !resources.containsKey("assets/alienevo/models/block/crystal_smooth.json")) {
            return 0;
        }

        int changed = 0;
        changed += repairIceLayerModels(resources);
        changed += repairCrystalBlockTextures(resources);
        changed += addMissingParticleTextureAliases(resources);
        changed += addMissingPrototypeSwitchSound(resources);
        changed += addMissingDiamondBlockItemModel(resources);
        return changed;
    }

    private static int repairIceLayerModels(Map<String, byte[]> resources) {
        String[][] blocks = {
                {"ice_layer", "minecraft:block/ice"},
                {"packed_ice_layer", "minecraft:block/packed_ice"},
                {"blue_ice_layer", "minecraft:block/blue_ice"}
        };
        String[] facings = {"down", "up", "north", "south", "east", "west"};
        String[] faces = {"north", "east", "south", "west", "up", "down"};
        int changed = 0;

        for (String[] block : blocks) {
            String blockName = block[0];
            String texture = block[1];
            String statePath = "assets/alienevo/blockstates/" + blockName + ".json";
            JsonObject stateRoot = parseObject(resources.get(statePath));
            if (stateRoot == null) stateRoot = new JsonObject();
            if (stateRoot.has("multipart")) continue;

            JsonObject variants;
            if (stateRoot.has("variants")) {
                if (!stateRoot.get("variants").isJsonObject()) continue;
                variants = stateRoot.getAsJsonObject("variants");
            } else {
                variants = new JsonObject();
            }
            boolean stateChanged = false;

            for (String facing : facings) {
                for (int layers = 1; layers <= 8; layers++) {
                    String variantKey = "facing=" + facing + ",layers=" + layers;
                    String modelId = "alienevo:block/" + blockName + "/" + facing + "_" + layers;
                    if (!variants.has(variantKey)) {
                        JsonObject variant = new JsonObject();
                        variant.addProperty("model", modelId);
                        variants.add(variantKey, variant);
                        stateChanged = true;
                    }

                    String modelPath = "assets/alienevo/models/block/" + blockName
                            + "/" + facing + "_" + layers + ".json";
                    if (!resources.containsKey(modelPath)) {
                        resources.put(modelPath, iceLayerModel(texture, facing, layers, faces)
                                .toString().getBytes(StandardCharsets.UTF_8));
                        changed++;
                    }
                }
            }

            if (stateChanged) {
                stateRoot.add("variants", variants);
                resources.put(statePath, stateRoot.toString().getBytes(StandardCharsets.UTF_8));
                changed++;
            }

            String itemPath = "assets/alienevo/models/item/" + blockName + ".json";
            if (!resources.containsKey(itemPath)) {
                JsonObject item = new JsonObject();
                item.addProperty("parent", "alienevo:block/" + blockName + "/up_8");
                resources.put(itemPath, item.toString().getBytes(StandardCharsets.UTF_8));
                changed++;
            }
        }
        return changed;
    }

    private static JsonObject iceLayerModel(
            String texture, String facing, int layers, String[] faces) {
        int thickness = layers * 2;
        int[] from;
        int[] to;
        switch (facing) {
            case "up" -> {
                from = new int[] {0, 0, 0};
                to = new int[] {16, thickness, 16};
            }
            case "down" -> {
                from = new int[] {0, 16 - thickness, 0};
                to = new int[] {16, 16, 16};
            }
            case "south" -> {
                from = new int[] {0, 0, 0};
                to = new int[] {16, 16, thickness};
            }
            case "north" -> {
                from = new int[] {0, 0, 16 - thickness};
                to = new int[] {16, 16, 16};
            }
            case "east" -> {
                from = new int[] {0, 0, 0};
                to = new int[] {thickness, 16, 16};
            }
            case "west" -> {
                from = new int[] {16 - thickness, 0, 0};
                to = new int[] {16, 16, 16};
            }
            default -> throw new IllegalArgumentException("Unknown ice-layer facing: " + facing);
        }

        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture);
        textures.addProperty("particle", texture);
        model.add("textures", textures);

        JsonObject element = new JsonObject();
        element.add("from", jsonVector(from));
        element.add("to", jsonVector(to));
        JsonObject modelFaces = new JsonObject();
        for (String faceName : faces) {
            JsonObject face = new JsonObject();
            face.add("uv", jsonVector(new int[] {0, 0, 16, 16}));
            face.addProperty("texture", "#all");
            if (iceLayerFaceTouchesBlockBoundary(facing, layers, faceName)) {
                face.addProperty("cullface", faceName);
            }
            modelFaces.add(faceName, face);
        }
        element.add("faces", modelFaces);
        JsonArray elements = new JsonArray();
        elements.add(element);
        model.add("elements", elements);
        return model;
    }

    private static JsonArray jsonVector(int[] values) {
        JsonArray array = new JsonArray();
        for (int value : values) array.add(value);
        return array;
    }

    private static boolean iceLayerFaceTouchesBlockBoundary(String facing, int layers, String face) {
        return switch (facing) {
            case "up" -> "down".equals(face) || (layers == 8 && "up".equals(face));
            case "down" -> "up".equals(face) || (layers == 8 && "down".equals(face));
            case "south" -> "north".equals(face) || (layers == 8 && "south".equals(face));
            case "north" -> "south".equals(face) || (layers == 8 && "north".equals(face));
            case "east" -> "west".equals(face) || (layers == 8 && "east".equals(face));
            case "west" -> "east".equals(face) || (layers == 8 && "west".equals(face));
            default -> false;
        };
    }

    private static int repairCrystalBlockTextures(Map<String, byte[]> resources) {
        int changed = 0;
        String[][] models = {
                {"assets/alienevo/models/block/crystal.json", "normal"},
                {"assets/alienevo/models/block/crystal_smooth.json", "smooth"}
        };
        for (String[] model : models) {
            String path = model[0];
            JsonObject root = parseObject(resources.get(path));
            if (root == null || !root.has("textures") || !root.get("textures").isJsonObject()) continue;
            JsonObject textures = root.getAsJsonObject("textures");
            boolean modified = false;
            for (String key : new java.util.ArrayList<>(textures.keySet())) {
                JsonElement value = textures.get(key);
                if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) continue;
                String original = value.getAsString();
                String replacement = null;
                if (original.startsWith("alienevo:block/diamond/side")) {
                    replacement = "alienevo:block/diamond_block_2";
                } else if (original.startsWith("alienevo:block/diamond/up_")) {
                    replacement = "alienevo:block/diamond_block_up";
                } else if ("alienevo:block/diamond/diamond_block".equals(original)) {
                    replacement = "alienevo:block/diamond_block";
                }
                if (replacement != null
                        && resources.containsKey("assets/" + replacement.substring(0, replacement.indexOf(':'))
                        + "/textures/" + replacement.substring(replacement.indexOf(':') + 1) + ".png")) {
                    textures.addProperty(key, replacement);
                    modified = true;
                }
            }
            if (modified) {
                resources.put(path, root.toString().getBytes(StandardCharsets.UTF_8));
                changed++;
            }
        }
        return changed;
    }

    private static int addMissingParticleTextureAliases(Map<String, byte[]> resources) {
        int changed = 0;
        for (int i = 0; i < 5; i++) {
            String index = Integer.toString(i);
            changed += copyResourceIfMissing(resources,
                    "assets/afomni/textures/particle/sonic_boom_" + index + ".png",
                    "assets/afomni/textures/particle/blast_" + index + ".png");
        }
        for (int i = 0; i < 14; i++) {
            String index = String.format(java.util.Locale.ROOT, "%02d", i);
            changed += copyResourceIfMissing(resources,
                    "assets/alienevo/textures/particle/flames_" + index + ".png",
                    "assets/alienevo/textures/particle/hb_flames_" + index + ".png");
        }
        return changed;
    }

    private static int addMissingPrototypeSwitchSound(Map<String, byte[]> resources) {
        return copyResourceIfMissing(resources,
                "assets/alienevo/sounds/prototype_switch3.ogg",
                "assets/alienevo/sounds/prototype_switch1.ogg");
    }

    private static int addMissingDiamondBlockItemModel(Map<String, byte[]> resources) {
        String path = "assets/alienevo/models/item/diamond_block.json";
        if (resources.containsKey(path)) return 0;
        JsonObject item = new JsonObject();
        item.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", "alienevo:block/diamond_block");
        item.add("textures", textures);
        resources.put(path, item.toString().getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    private static int copyResourceIfMissing(
            Map<String, byte[]> resources, String sourcePath, String targetPath) {
        if (resources.containsKey(targetPath)) return 0;
        byte[] source = resources.get(sourcePath);
        if (source == null) return 0;
        resources.put(targetPath, source.clone());
        return 1;
    }

    private static JsonObject parseObject(byte[] data) {
        if (data == null) return null;
        try {
            JsonElement parsed = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) return null;
        return object.get(key).getAsString();
    }

    private static String abilityType(JsonObject abilities, String id) {
        if (!abilities.has(id) || !abilities.get(id).isJsonObject()) return null;
        JsonObject ability = abilities.getAsJsonObject(id);
        return ability.has("type") && ability.get("type").isJsonPrimitive()
                ? ability.get("type").getAsString() : null;
    }

    private static boolean containsOutside(Map<String, byte[]> values, String excludedPath, String token) {
        byte[] needle = token.getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<String, byte[]> entry : values.entrySet()) {
            if (!entry.getKey().equals(excludedPath)
                    && (entry.getKey().contains(token) || contains(entry.getValue(), needle))) return true;
        }
        return false;
    }

    private static boolean contains(Map<String, byte[]> values, String token) {
        byte[] needle = token.getBytes(StandardCharsets.UTF_8);
        for (byte[] value : values.values()) if (contains(value, needle)) return true;
        return false;
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        if (needle.length == 0 || haystack.length < needle.length) return false;
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }
}
