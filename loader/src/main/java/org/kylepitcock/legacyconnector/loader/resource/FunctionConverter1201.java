package org.kylepitcock.legacyconnector.loader.resource;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the 1.20.1 command dialect used by datapack functions and Palladium
 * command properties to the 1.21.1 command dialect. Rewrites are deliberately
 * limited to syntax proven in inspected legacy resources; unknown SNBT shapes
 * are left intact for diagnostics rather than guessed at.
 */
public final class FunctionConverter1201 {
    private static final Pattern LEGACY_PARTICLE =
            Pattern.compile("\\bparticle\\s+(minecraft:dust|dust|block|item)\\b");
    private static final Pattern EMPTY_STACK =
            Pattern.compile("\\{id:[\\\"']?([a-zA-Z0-9_./:-]+)[\\\"']?,Count:0(?:b)?\\}");
    private static final Pattern STACK_WITH_CUSTOM_MODEL_DATA =
            Pattern.compile("\\{id:[\\\"']?([a-zA-Z0-9_./:-]+)[\\\"']?,Count:(\\d+),tag:\\{CustomModelData:(\\d+)\\}\\}");
    private static final Pattern ATTRIBUTE_ENTRY =
            Pattern.compile("Attributes:\\[\\{Name:([^,]+),Base:([^}]+)\\}\\]");
    private static final Pattern ENCHANTED_ITEM =
            Pattern.compile("([a-zA-Z0-9_./:-]+)\\{Enchantments:\\[\\{lvl:(\\d+),id:([a-zA-Z0-9_./:-]+)\\}\\]\\}");

    private FunctionConverter1201() {}

    public static final class Stats {
        public int changedFiles;
        public int rewrittenCommands;
        public final Map<String, Integer> categories = new LinkedHashMap<>();
        void hit(String category) { categories.merge(category, 1, Integer::sum); }
    }

    /** Converts every .mcfunction resource, preserving path and line endings. */
    public static Map<String, byte[]> convertAll(Map<String, byte[]> entries, Stats stats) {
        Map<String, byte[]> out = new LinkedHashMap<>(entries);
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            if (!entry.getKey().endsWith(".mcfunction")) continue;
            String source = new String(entry.getValue(), StandardCharsets.UTF_8);
            String separator = source.contains("\r\n") ? "\r\n" : "\n";
            String[] lines = source.split("\\r?\\n", -1);
            StringBuilder migrated = new StringBuilder(source.length() + 128);
            int rewritten = 0;
            for (int i = 0; i < lines.length; i++) {
                String next = convertCommand(lines[i]);
                if (!next.equals(lines[i])) rewritten++;
                migrated.append(next);
                if (i + 1 < lines.length) migrated.append(separator);
            }
            String result = migrated.toString();
            if (!result.equals(source)) {
                out.put(entry.getKey(), result.getBytes(StandardCharsets.UTF_8));
                stats.changedFiles++;
                stats.rewrittenCommands += rewritten;
                stats.hit("command_lines");
            }
        }
        return out;
    }

    /** Translate inspected particle option forms and item/entity NBT fields. */
    public static String convertCommand(String command) {
        if (command == null || command.isEmpty()) return command;
        String result = command.replace("minecraft:grass", "minecraft:short_grass");
        result = convertParticle(result);
        result = convertItemStacks(result);
        result = convertEntityNbt(result);
        result = convertEnchantedItemArgument(result);
        return result;
    }

    private static String convertParticle(String command) {
        Matcher match = LEGACY_PARTICLE.matcher(command);
        if (!match.find()) return command;
        String kind = match.group(1);
        String[] args = command.substring(match.end()).trim().split("\\s+");
        if (args.length == 1 && args[0].isEmpty()) {
            throw new IllegalStateException("Incomplete legacy particle command: " + command);
        }

        String modernType;
        int consumed;
        if (kind.equals("minecraft:dust") || kind.equals("dust")) {
            if (args.length < 12) throw new IllegalStateException("Incomplete legacy dust particle command: " + command);
            modernType = "minecraft:dust{color:[" + args[0] + "," + args[1] + "," + args[2]
                    + "],scale:" + args[3] + "}";
            consumed = 4;
        } else if (kind.equals("block")) {
            if (args.length < 10) throw new IllegalStateException("Incomplete legacy block particle command: " + command);
            modernType = "minecraft:block{block_state:\"" + resourceId(args[0]) + "\"}";
            consumed = 1;
        } else {
            if (args.length < 10) throw new IllegalStateException("Incomplete legacy item particle command: " + command);
            modernType = "minecraft:item{item:" + itemStack(args[0]) + "}";
            consumed = 1;
        }

        StringBuilder suffix = new StringBuilder();
        for (int i = consumed; i < args.length; i++) {
            if (suffix.length() > 0) suffix.append(' ');
            suffix.append(args[i]);
        }
        return command.substring(0, match.start()) + "particle " + modernType
                + (suffix.length() == 0 ? "" : " " + suffix);
    }

    private static String itemStack(String source) {
        int brace = source.indexOf('{');
        String id = brace < 0 ? source : source.substring(0, brace);
        String customModelData = null;
        if (brace >= 0) {
            if (!source.endsWith("}")) throw new IllegalStateException("Unrecognized legacy item particle stack: " + source);
            String nbt = source.substring(brace + 1, source.length() - 1);
            Matcher cmd = Pattern.compile("CustomModelData:(\\d+)(?:[bBsSlL])?").matcher(nbt);
            if (!cmd.matches()) throw new IllegalStateException("Unrecognized legacy item particle NBT: " + source);
            customModelData = cmd.group(1);
        }
        String itemId = resourceId(id);
        StringBuilder stack = new StringBuilder("{id:\"").append(itemId).append("\"");
        if (customModelData != null) {
            stack.append(",components:{\"minecraft:custom_model_data\":").append(customModelData).append("}");
        }
        return stack.append("}").toString();
    }

    private static String resourceId(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static String convertItemStacks(String source) {
        Matcher stack = STACK_WITH_CUSTOM_MODEL_DATA.matcher(source);
        StringBuffer out = new StringBuffer();
        while (stack.find()) {
            String id = resourceId(stack.group(1));
            String value = "{id:\"" + id + "\",count:" + stack.group(2)
                    + ",components:{\"minecraft:custom_model_data\":" + stack.group(3) + "}}";
            stack.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        stack.appendTail(out);
        source = out.toString();

        Matcher empty = EMPTY_STACK.matcher(source);
        out = new StringBuffer();
        while (empty.find()) empty.appendReplacement(out, Matcher.quoteReplacement("{}"));
        empty.appendTail(out);
        source = out.toString();

        return source.replaceAll("tag:\\{CustomModelData:(\\d+)\\}",
                "components:{\"minecraft:custom_model_data\":$1}");
    }

    private static String convertEntityNbt(String source) {
        source = source.replace("ActiveEffects:", "active_effects:")
                .replace("Id:14b", "id:\"minecraft:invisibility\"")
                .replace("Amplifier:", "amplifier:")
                .replace("Duration:", "duration:")
                .replace("ShowParticles:", "show_particles:");

        Matcher attributes = ATTRIBUTE_ENTRY.matcher(source);
        StringBuffer out = new StringBuffer();
        while (attributes.find()) {
            String name = attributes.group(1).trim();
            String id = name.startsWith("generic.") ? "minecraft:" + name.substring("generic.".length())
                    : resourceId(name);
            String value = "attributes:[{id:\"" + id + "\",base:" + attributes.group(2) + "}]";
            attributes.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        attributes.appendTail(out);
        return out.toString();
    }

    private static String convertEnchantedItemArgument(String source) {
        Matcher item = ENCHANTED_ITEM.matcher(source);
        StringBuffer out = new StringBuffer();
        while (item.find()) {
            String id = resourceId(item.group(1));
            String enchantment = resourceId(item.group(3));
            String value = id + "[minecraft:enchantments={levels:{\"" + enchantment + "\":" + item.group(2) + "}}]";
            item.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        item.appendTail(out);
        return out.toString();
    }
}
