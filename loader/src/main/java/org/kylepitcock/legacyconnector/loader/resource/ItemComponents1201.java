package org.kylepitcock.legacyconnector.loader.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.kylepitcock.legacyconnector.loader.resource.Snbt1201.ArrayTag;
import org.kylepitcock.legacyconnector.loader.resource.Snbt1201.Compound;
import org.kylepitcock.legacyconnector.loader.resource.Snbt1201.ListTag;
import org.kylepitcock.legacyconnector.loader.resource.Snbt1201.Num;
import org.kylepitcock.legacyconnector.loader.resource.Snbt1201.Str;
import org.kylepitcock.legacyconnector.loader.resource.Snbt1201.Tag;

/**
 * The structured form of the 1.20.1 to 1.21.1 item data migration: an item stack's NBT ({@code {Damage, display, Enchantments, StoredEnchantments,
 * CustomModelData, Unbreakable, HideFlags, AttributeModifiers, BlockEntityTag, ...}}) split into the data components 1.21.1 reads and the mod's own
 * keys, which go to {@code minecraft:custom_data} (as the game's own upgrade does). Used by {@link DataConverter1201} for recipe results and
 * ingredients, advancement icons, loot functions ({@code set_nbt} to {@code set_components}) and item predicates.
 *
 * <p>Portions adapted from Retromod (https://github.com/Bownlux/Retromod), MIT License, Copyright (c) 2026 Bownlux: the split of an item's NBT by
 * who owns each key, the banner pattern table, the attribute modifier and HideFlags mapping ({@code resources/ItemStackComponents.java},
 * {@code ItemStackDataMigrator.java}), reduced to the 1.21.1 target and re-checked against the 1.21.1 jars. A key that cannot be carried over exactly
 * stays in the custom data instead of failing the whole stack.
 */
final class ItemComponents1201 {
    private ItemComponents1201() {}

    /** The components for the keys Minecraft owns, the keys left for {@code minecraft:custom_data}, and the Minecraft keys that could not be carried over. */
    record Split(JsonObject components, Compound customData, List<String> unmapped) {}

    /** Keys every item had, which the game's own upgrade moved to components. */
    private static final Set<String> GENERIC_KEYS = Set.of("Damage", "RepairCost", "Unbreakable", "HideFlags", "display", "Enchantments",
            "StoredEnchantments", "ench", "AttributeModifiers", "CanDestroy", "CanPlaceOn", "CustomModelData", "Trim", "BlockEntityTag", "BlockStateTag",
            "EntityTag", "DebugProperty", "CustomPotionColor", "CustomPotionEffects");

    /** Keys Minecraft read only on some of its own items; on a mod's item they are the mod's own. */
    private static final Set<String> VANILLA_ITEM_KEYS = Set.of("Potion", "Charged", "ChargedProjectiles", "Items", "Decorations", "map", "effects",
            "pages", "filtered_pages", "author", "generation", "resolved", "title", "filtered_title", "SkullOwner", "LodestonePos", "LodestoneDimension",
            "LodestoneTracked", "Explosion", "Fireworks", "instrument", "BucketVariantTag", "Recipes", "Variant");

    private static final String[] DYE_COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
            "purple", "blue", "brown", "green", "red", "black"};

    /** Banner pattern codes to the pattern ids of 1.21.1 (checked against its {@code data/minecraft/banner_pattern}). */
    private static final Map<String, String> BANNER_PATTERNS = pairs(
            "b", "base", "bl", "square_bottom_left", "br", "square_bottom_right", "tl", "square_top_left", "tr", "square_top_right",
            "bs", "stripe_bottom", "ts", "stripe_top", "ls", "stripe_left", "rs", "stripe_right", "cs", "stripe_center", "ms", "stripe_middle",
            "drs", "stripe_downright", "dls", "stripe_downleft", "ss", "small_stripes", "cr", "cross", "sc", "straight_cross", "bt", "triangle_bottom",
            "tt", "triangle_top", "bts", "triangles_bottom", "tts", "triangles_top", "ld", "diagonal_left", "rd", "diagonal_up_right",
            "lud", "diagonal_up_left", "rud", "diagonal_right", "mc", "circle", "mr", "rhombus", "vh", "half_vertical", "hh", "half_horizontal",
            "vhr", "half_vertical_right", "hhb", "half_horizontal_bottom", "bo", "border", "cbo", "curly_border", "gra", "gradient", "gru", "gradient_up",
            "bri", "bricks", "glb", "globe", "cre", "creeper", "sku", "skull", "flo", "flower", "moj", "mojang", "pig", "piglin");

    /** The numeric mob effect ids of 1.20.1 (potion effects of stacks and tipped arrows were numbers). */
    private static final String[] EFFECT_IDS = {null, "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health", "instant_damage",
            "jump_boost", "nausea", "regeneration", "resistance", "fire_resistance", "water_breathing", "invisibility", "blindness", "night_vision",
            "hunger", "weakness", "poison", "wither", "health_boost", "absorption", "saturation", "glowing", "levitation", "luck", "unluck",
            "slow_falling", "conduit_power", "dolphins_grace", "bad_omen", "hero_of_the_village", "darkness"};

    /** The attribute names 1.16 replaced, and the one 1.20.5 renamed, to their 1.21.1 ids. */
    private static final Map<String, String> ATTRIBUTE_RENAMES = pairs(
            "generic.maxHealth", "generic.max_health", "generic.followRange", "generic.follow_range",
            "generic.knockbackResistance", "generic.knockback_resistance", "generic.movementSpeed", "generic.movement_speed",
            "generic.flyingSpeed", "generic.flying_speed", "generic.attackDamage", "generic.attack_damage", "generic.attackKnockback",
            "generic.attack_knockback", "generic.attackSpeed", "generic.attack_speed", "generic.armorToughness", "generic.armor_toughness",
            "horse.jumpStrength", "generic.jump_strength", "horse.jump_strength", "generic.jump_strength",
            "zombie.spawnReinforcements", "zombie.spawn_reinforcements");

    /** The attributes of 1.21.1 (read from its {@code Attributes} class). */
    private static final Set<String> ATTRIBUTES = Set.of("generic.armor", "generic.armor_toughness", "generic.attack_damage", "generic.attack_knockback",
            "generic.attack_speed", "generic.burning_time", "generic.explosion_knockback_resistance", "generic.fall_damage_multiplier",
            "generic.flying_speed", "generic.follow_range", "generic.gravity", "generic.jump_strength", "generic.knockback_resistance", "generic.luck",
            "generic.max_absorption", "generic.max_health", "generic.movement_efficiency", "generic.movement_speed", "generic.oxygen_bonus",
            "generic.safe_fall_distance", "generic.scale", "generic.step_height", "generic.water_movement_efficiency", "player.block_break_speed",
            "player.block_interaction_range", "player.entity_interaction_range", "player.mining_efficiency", "player.sneaking_speed",
            "player.submerged_mining_speed", "player.sweeping_damage_ratio", "zombie.spawn_reinforcements");

    /** The ids 1.21 gave the two modifiers a weapon's own damage and speed use. */
    private static final Map<String, String> BASE_MODIFIER_IDS = Map.of(
            "cb3f55d3-645c-4f38-a497-9c13a33db5cf", "minecraft:base_attack_damage",
            "fa233e1c-4180-4865-b01b-bcce9785aca3", "minecraft:base_attack_speed");

    private static final String[] MODIFIER_OPERATIONS = {"add_value", "add_multiplied_base", "add_multiplied_total"};
    private static final Set<String> MODIFIER_SLOTS = Set.of("mainhand", "offhand", "head", "chest", "legs", "feet");

    private static Map<String, String> pairs(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return Map.copyOf(m);
    }

    /** Whether Minecraft reads this top-level key on some item (so it is not a mod's own data). */
    static boolean ownedByMinecraft(String key) {
        return GENERIC_KEYS.contains(key) || VANILLA_ITEM_KEYS.contains(key);
    }

    // ---- the split ----------------------------------------------------------------------------------------------------------------

    /**
     * Splits the NBT of a stack of {@code itemId} (null when the item is not known). Returns null when a key only some of Minecraft's items read, such as
     * {@code Potion}, appears and the item is not known: it could be Minecraft's or the mod's own, and the caller keeps the NBT as it is.
     */
    static Split split(String itemId, Compound tag) {
        boolean vanillaItem = itemId != null && (itemId.startsWith("minecraft:") || itemId.indexOf(':') < 0);
        JsonObject components = new JsonObject();
        Map<String, Tag> custom = new LinkedHashMap<>();
        List<String> unmapped = new ArrayList<>();
        long hideFlags = 0;
        for (Map.Entry<String, Tag> entry : tag.entries().entrySet()) {
            String key = entry.getKey();
            Tag value = entry.getValue();
            if (itemId == null && VANILLA_ITEM_KEYS.contains(key)) return null;
            boolean owned = GENERIC_KEYS.contains(key) || vanillaItem && VANILLA_ITEM_KEYS.contains(key);
            if (!owned) {
                custom.put(key, value);
                continue;
            }
            if (key.equals("HideFlags")) {
                if (value instanceof Num n && n.asLong() != null) hideFlags = n.asLong();
                else {
                    custom.put(key, value);
                    unmapped.add(key);
                }
                continue;
            }
            if (!convert(key, value, components)) {
                custom.put(key, value);
                unmapped.add(key);
            }
        }
        if (hideFlags != 0) hide(hideFlags, components);
        return new Split(components, new Compound(new LinkedHashMap<>(custom)), unmapped);
    }

    /** The components of a stack with the mod's own keys as {@code minecraft:custom_data}, or null when the stack cannot be split without knowing its item. */
    static JsonObject toComponents(String itemId, Compound tag) {
        Split split = split(itemId, tag);
        if (split == null) return null;
        JsonObject out = split.components();
        if (!split.customData().entries().isEmpty()) out.addProperty("minecraft:custom_data", Snbt1201.write(split.customData()));
        return out;
    }

    private static boolean convert(String key, Tag value, JsonObject components) {
        switch (key) {
            case "Damage", "RepairCost" -> {
                if (!(value instanceof Num n) || n.asLong() == null) return false;
                // zero is the default, and a damage component on an item that cannot break fails to load
                if (n.asLong() > 0) components.addProperty(key.equals("Damage") ? "minecraft:damage" : "minecraft:repair_cost", n.asLong());
                return true;
            }
            case "Unbreakable" -> {
                if (!(value instanceof Num n) || n.asLong() == null) return false;
                if (n.asLong() != 0) components.add("minecraft:unbreakable", new JsonObject());
                return true;
            }
            case "CustomModelData" -> {
                if (!(value instanceof Num n) || n.asLong() == null) return false;
                components.addProperty("minecraft:custom_model_data", n.asLong());
                return true;
            }
            case "display" -> {
                return value instanceof Compound display && convertDisplay(display, components);
            }
            case "Enchantments", "StoredEnchantments" -> {
                JsonObject levels = enchantmentLevels(value);
                if (levels == null) return false;
                if (levels.size() > 0) {
                    JsonObject holder = new JsonObject();
                    holder.add("levels", levels);
                    components.add(key.equals("Enchantments") ? "minecraft:enchantments" : "minecraft:stored_enchantments", holder);
                }
                return true;
            }
            case "Potion" -> {
                if (!(value instanceof Str potion) || !isIdentifier(potion.value())) return false;
                potionContents(components).addProperty("potion", namespaced(potion.value()));
                return true;
            }
            case "CustomPotionColor" -> {
                if (!(value instanceof Num n) || n.asLong() == null) return false;
                potionContents(components).addProperty("custom_color", n.asLong());
                return true;
            }
            case "CustomPotionEffects" -> {
                JsonArray effects = potionEffects(value);
                if (effects == null) return false;
                if (effects.size() > 0) potionContents(components).add("custom_effects", effects);
                return true;
            }
            case "AttributeModifiers" -> {
                JsonArray modifiers = attributeModifiers(value);
                if (modifiers == null) return false;
                components.add("minecraft:attribute_modifiers", modifiers);
                return true;
            }
            case "CanDestroy", "CanPlaceOn" -> {
                JsonObject predicate = adventurePredicate(value);
                if (predicate == null) return false;
                components.add(key.equals("CanDestroy") ? "minecraft:can_break" : "minecraft:can_place_on", predicate);
                return true;
            }
            case "BlockStateTag" -> {
                if (!(value instanceof Compound state)) return false;
                JsonObject props = new JsonObject();
                for (Map.Entry<String, Tag> e : state.entries().entrySet()) {
                    if (e.getValue() instanceof Str s) props.addProperty(e.getKey(), s.value());
                    else if (e.getValue() instanceof Num n) props.addProperty(e.getKey(), n.type() == 'b' ? String.valueOf(n.value().signum() != 0) : n.value().toPlainString());
                    else return false;
                }
                components.add("minecraft:block_state", props);
                return true;
            }
            case "EntityTag" -> {
                // entity_data names the entity type in its own "id"
                if (!(value instanceof Compound entity) || entity.string("id") == null || !isIdentifier(entity.string("id"))) return false;
                components.addProperty("minecraft:entity_data", Snbt1201.write(entity));
                return true;
            }
            case "BlockEntityTag" -> {
                if (!(value instanceof Compound blockEntity)) return false;
                if (Set.of("id", "Patterns", "Base").containsAll(blockEntity.entries().keySet())
                        && (blockEntity.get("Patterns") != null || blockEntity.get("Base") != null)) {
                    return bannerData(blockEntity, components);
                }
                // block_entity_data names the block entity type in its own "id"; without it the item's block would have to be looked up
                if (blockEntity.string("id") == null || !isIdentifier(blockEntity.string("id"))) return false;
                components.addProperty("minecraft:block_entity_data", Snbt1201.write(blockEntity));
                return true;
            }
            default -> {
                // Trim, ench (1.12 numeric enchantments), DebugProperty and the item specific keys other than Potion
                return false;
            }
        }
    }

    private static JsonObject potionContents(JsonObject components) {
        if (components.get("minecraft:potion_contents") instanceof JsonObject o) return o;
        JsonObject contents = new JsonObject();
        components.add("minecraft:potion_contents", contents);
        return contents;
    }

    private static boolean convertDisplay(Compound display, JsonObject components) {
        JsonObject staged = new JsonObject();
        for (Map.Entry<String, Tag> field : display.entries().entrySet()) {
            switch (field.getKey()) {
                case "Name" -> {
                    if (!(field.getValue() instanceof Str name)) return false;
                    staged.add("minecraft:custom_name", text(name.value()));
                }
                case "Lore" -> {
                    if (!(field.getValue() instanceof ListTag lines)) return false;
                    JsonArray lore = new JsonArray();
                    for (Tag line : lines.items()) {
                        if (!(line instanceof Str text)) return false;
                        lore.add(text(text.value()));
                    }
                    staged.add("minecraft:lore", lore);
                }
                case "color" -> {
                    if (!(field.getValue() instanceof Num n) || n.asLong() == null) return false;
                    JsonObject color = new JsonObject();
                    color.addProperty("rgb", n.asLong());
                    staged.add("minecraft:dyed_color", color);
                }
                case "MapColor" -> {
                    if (!(field.getValue() instanceof Num n) || n.asLong() == null) return false;
                    staged.addProperty("minecraft:map_color", n.asLong());
                }
                default -> {
                    return false;
                }
            }
        }
        staged.entrySet().forEach(e -> components.add(e.getKey(), e.getValue()));
        return true;
    }

    /** A custom name or lore line: 1.20.1 stored JSON text in the string; text that is not JSON is literal. */
    private static JsonElement text(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.isEmpty() && "{[\"".indexOf(trimmed.charAt(0)) >= 0) {
            try {
                JsonElement parsed = JsonParser.parseString(trimmed);
                if (parsed.isJsonObject() || parsed.isJsonArray() || parsed.isJsonPrimitive() && parsed.getAsJsonPrimitive().isString()) return parsed;
            } catch (RuntimeException plain) {
                // literal text
            }
        }
        return new JsonPrimitive(raw);
    }

    /** {@code [{id, lvl}]} as an id to level map; levels outside 1 to 255 are clamped, a numeric id (1.12) cannot be carried over. */
    private static JsonObject enchantmentLevels(Tag value) {
        if (!(value instanceof ListTag list)) return null;
        JsonObject levels = new JsonObject();
        for (Tag item : list.items()) {
            if (!(item instanceof Compound e) || e.string("id") == null || !isIdentifier(e.string("id")) || !(e.get("lvl") instanceof Num lvl) || lvl.asLong() == null) {
                return null;
            }
            long level = Math.min(255, lvl.asLong());
            if (level >= 1) levels.addProperty(namespaced(e.string("id")), level);
        }
        return levels;
    }

    private static JsonArray potionEffects(Tag value) {
        if (!(value instanceof ListTag list)) return null;
        JsonArray out = new JsonArray();
        for (Tag item : list.items()) {
            if (!(item instanceof Compound e)) return null;
            String id = null;
            if (e.get("Id") instanceof Num n && n.asLong() != null && n.asLong() > 0 && n.asLong() < EFFECT_IDS.length) id = "minecraft:" + EFFECT_IDS[(int) (long) n.asLong()];
            else if (e.get("Id") instanceof Str s && isIdentifier(s.value())) id = namespaced(s.value());
            if (id == null) return null;
            JsonObject effect = new JsonObject();
            effect.addProperty("id", id);
            if (e.get("Amplifier") instanceof Num n && n.asLong() != null) effect.addProperty("amplifier", Math.max(0, Math.min(255, n.asLong())));
            if (e.get("Duration") instanceof Num n && n.asLong() != null) effect.addProperty("duration", n.asLong());
            if (e.get("Ambient") instanceof Num n) effect.addProperty("ambient", n.value().signum() != 0);
            if (e.get("ShowParticles") instanceof Num n) effect.addProperty("show_particles", n.value().signum() != 0);
            if (e.get("ShowIcon") instanceof Num n) effect.addProperty("show_icon", n.value().signum() != 0);
            out.add(effect);
        }
        return out;
    }

    private static JsonArray attributeModifiers(Tag value) {
        if (!(value instanceof ListTag list)) return null;
        JsonArray out = new JsonArray();
        for (Tag item : list.items()) {
            JsonObject modifier = item instanceof Compound old ? attributeModifier(old) : null;
            if (modifier == null) return null;
            out.add(modifier);
        }
        return out;
    }

    private static JsonObject attributeModifier(Compound old) {
        if (!Set.of("AttributeName", "Name", "Amount", "Operation", "UUID", "UUIDMost", "UUIDLeast", "Slot").containsAll(old.entries().keySet())) return null;
        String attribute = attributeId(old.string("AttributeName"));
        java.util.UUID uuid = modifierUuid(old);
        Long operation = old.get("Operation") == null ? Long.valueOf(0) : old.get("Operation") instanceof Num n ? n.asLong() : null;
        String slot = old.string("Slot");
        if (attribute == null || uuid == null || !(old.get("Amount") instanceof Num amount) || operation == null || operation < 0
                || operation >= MODIFIER_OPERATIONS.length || old.get("Slot") != null && (slot == null || !MODIFIER_SLOTS.contains(slot))) {
            return null;
        }
        JsonObject modifier = new JsonObject();
        modifier.addProperty("type", attribute);
        modifier.addProperty("id", BASE_MODIFIER_IDS.getOrDefault(uuid.toString(), "legacy:" + uuid));
        modifier.add("amount", new JsonPrimitive(amount.value()));
        modifier.addProperty("operation", MODIFIER_OPERATIONS[(int) (long) operation]);
        if (slot != null) modifier.addProperty("slot", slot);
        return modifier;
    }

    private static String attributeId(String name) {
        if (name == null) return null;
        String mapped = DataConverter1201.mapAttributeId(name);
        if (!mapped.equals(name)) return mapped; // forge: attributes
        String path = name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
        path = ATTRIBUTE_RENAMES.getOrDefault(path, path);
        if (ATTRIBUTES.contains(path)) return "minecraft:" + path;
        // another mod's attribute keeps its id; an unknown unprefixed name is a typo that 1.21.1 would reject
        return name.indexOf(':') > 0 && !name.startsWith("minecraft:") ? name : null;
    }

    private static java.util.UUID modifierUuid(Compound old) {
        if (old.get("UUID") instanceof ArrayTag ints && ints.kind() == 'I' && ints.values().size() == 4) {
            long[] p = new long[4];
            for (int i = 0; i < 4; i++) {
                Long part = ints.values().get(i).asLong();
                if (part == null) return null;
                p[i] = part & 0xffffffffL;
            }
            return new java.util.UUID(p[0] << 32 | p[1], p[2] << 32 | p[3]);
        }
        if (old.get("UUIDMost") instanceof Num most && old.get("UUIDLeast") instanceof Num least && most.asLong() != null && least.asLong() != null) {
            return new java.util.UUID(most.asLong(), least.asLong());
        }
        return null;
    }

    /** {@code CanDestroy}/{@code CanPlaceOn}: a list of block ids or tags. A block with state or NBT in the string cannot be carried over. */
    private static JsonObject adventurePredicate(Tag value) {
        if (!(value instanceof ListTag list) || list.items().isEmpty()) return null;
        JsonArray predicates = new JsonArray();
        for (Tag item : list.items()) {
            if (!(item instanceof Str s) || s.value().indexOf('[') >= 0 || s.value().indexOf('{') >= 0) return null;
            JsonObject blocks = new JsonObject();
            blocks.addProperty("blocks", s.value());
            predicates.add(blocks);
        }
        JsonObject out = new JsonObject();
        out.add("predicates", predicates);
        return out;
    }

    private static boolean bannerData(Compound blockEntity, JsonObject components) {
        JsonObject staged = new JsonObject();
        Tag patterns = blockEntity.get("Patterns");
        if (patterns != null) {
            if (!(patterns instanceof ListTag list)) return false;
            JsonArray out = new JsonArray();
            for (Tag item : list.items()) {
                if (!(item instanceof Compound layer) || layer.entries().size() != 2) return false;
                String id = layer.string("Pattern") == null ? null : BANNER_PATTERNS.get(layer.string("Pattern"));
                String color = dyeColor(layer.get("Color"));
                if (id == null || color == null) return false;
                JsonObject entry = new JsonObject();
                entry.addProperty("color", color);
                entry.addProperty("pattern", "minecraft:" + id);
                out.add(entry);
            }
            staged.add("minecraft:banner_patterns", out);
        }
        if (blockEntity.get("Base") != null) {
            String color = dyeColor(blockEntity.get("Base"));
            if (color == null) return false;
            staged.addProperty("minecraft:base_color", color);
        }
        staged.entrySet().forEach(e -> components.add(e.getKey(), e.getValue()));
        return true;
    }

    private static String dyeColor(Tag value) {
        Long index = value instanceof Num n ? n.asLong() : null;
        return index == null || index < 0 || index > 15 ? null : DYE_COLORS[(int) (long) index];
    }

    /**
     * The tooltip lines HideFlags hid: 1.21.1 puts {@code show_in_tooltip: false} on the component (enchantments 1, attribute modifiers 2,
     * unbreakable 4, can_break 8, can_place_on 16, dyed color 64), and the additional lines (32) are the {@code hide_additional_tooltip} component.
     */
    private static void hide(long flags, JsonObject components) {
        hideIn(flags, 1, "minecraft:enchantments", components);
        hideIn(flags, 2, "minecraft:attribute_modifiers", components);
        hideIn(flags, 4, "minecraft:unbreakable", components);
        hideIn(flags, 8, "minecraft:can_break", components);
        hideIn(flags, 16, "minecraft:can_place_on", components);
        hideIn(flags, 64, "minecraft:dyed_color", components);
        if ((flags & 32) != 0) {
            components.add("minecraft:hide_additional_tooltip", new JsonObject());
            hideIn(32, 32, "minecraft:stored_enchantments", components);
        }
    }

    private static void hideIn(long flags, int bit, String component, JsonObject components) {
        if ((flags & bit) == 0 || !components.has(component)) return;
        JsonElement value = components.get(component);
        if (value.isJsonArray()) { // a bare list of attribute modifiers takes the object form
            JsonObject wrapped = new JsonObject();
            wrapped.add("modifiers", value);
            value = wrapped;
        }
        if (value.isJsonObject()) {
            value.getAsJsonObject().addProperty("show_in_tooltip", false);
            components.add(component, value);
        }
    }

    private static boolean isIdentifier(String id) {
        return id.matches("(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+");
    }

    private static String namespaced(String id) {
        return id.indexOf(':') < 0 ? "minecraft:" + id : id;
    }
}
