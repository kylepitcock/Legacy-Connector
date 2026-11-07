package org.kylepitcock.legacyconnector.compat.patchouli;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.BlockBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * Turns the ids 1.12 book text and Patchouli API calls use (items as {@code mod:item:meta}, blocks as {@code mod:block[prop=value]}) into the modern ones
 * the layer registered, using the legacy registries. Every method falls back to the input when the id is unknown (the id may already be a modern one).
 */
public final class Ids1122 {
    private Ids1122() {}

    /** {@code mod:item}, {@code mod:item:meta}, optionally followed by {@code #count} and {@code {nbt}} (1.12 Patchouli's stack syntax). */
    public static String item(String spec) {
        ItemStack stack = stack(spec);
        if (stack == null || stack.isEmpty()) return spec;
        var modern = Stacks1122.toModern(stack);
        if (modern.isEmpty()) return spec;
        String id = BuiltInRegistries.ITEM.getKey(modern.getItem()).toString();
        String suffix = spec.substring(Math.min(spec.length(), splitEnd(spec)));
        // "#count" stays; legacy NBT ("{...}") is passed as custom data
        int brace = suffix.indexOf('{');
        String count = suffix;
        String nbt = "";
        if (brace >= 0) {
            count = suffix.substring(0, brace);
            nbt = suffix.substring(brace);
        }
        int hash = count.indexOf('#');
        String countPart = hash >= 0 ? count.substring(hash) : "";
        if (!nbt.isEmpty()) id = id + "[minecraft:custom_data=" + nbt.replaceAll("#.*$", "") + "]";
        return id + countPart;
    }

    /** Where the {@code mod:item[:meta]} part of a stack string ends. */
    private static int splitEnd(String spec) {
        int end = spec.length();
        for (char c : new char[] {'#', '{'}) {
            int i = spec.indexOf(c);
            if (i >= 0 && i < end) end = i;
        }
        return end;
    }

    /** The legacy stack a 1.12 Patchouli stack string names, or null. */
    public static ItemStack stack(String spec) {
        if (spec == null || spec.isBlank()) return null;
        String name = spec.substring(0, splitEnd(spec)).trim();
        int meta = 0;
        int count = 1;
        String[] parts = name.split(":");
        if (parts.length == 3 && parts[2].matches("\\d+|\\*")) {
            meta = parts[2].equals("*") ? 32767 : Integer.parseInt(parts[2]);
            name = parts[0] + ":" + parts[1];
        } else if (parts.length > 3) {
            return null;
        }
        int hash = spec.indexOf('#');
        if (hash >= 0) {
            int end = spec.indexOf('{', hash);
            try {
                count = Integer.parseInt(spec.substring(hash + 1, end < 0 ? spec.length() : end).trim());
            } catch (NumberFormatException e) {
                count = 1;
            }
        }
        Item item;
        try {
            item = Registries1122.ITEMS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(name));
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
        if (item == null) return null;
        return new ItemStack(item, count, meta == 32767 ? 0 : meta);
    }

    /** A 1.12 block string ({@code mod:block}, {@code mod:block:meta}, {@code mod:block[prop=value,...]}, {@code *}, {@code _}) as a modern one. */
    public static String block(String spec) {
        if (spec == null) return null;
        String s = spec.trim();
        if (s.isEmpty() || s.equals("*") || s.equals("_") || s.startsWith("#")) return spec;
        String name = s;
        String props = null;
        int br = s.indexOf('[');
        if (br >= 0) {
            name = s.substring(0, br);
            int close = s.lastIndexOf(']');
            props = s.substring(br + 1, close < 0 ? s.length() : close);
        }
        int meta = -1;
        String[] parts = name.split(":");
        if (parts.length == 3 && parts[2].matches("\\d+")) {
            meta = Integer.parseInt(parts[2]);
            name = parts[0] + ":" + parts[1];
        }
        try {
            Block block = Registries1122.BLOCKS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(name));
            if (block == null) return spec;
            IBlockState state = meta >= 0 ? block.getStateFromMeta(meta) : block.getDefaultState();
            if (props != null && !props.isBlank()) {
                for (String kv : props.split(",")) {
                    int eq = kv.indexOf('=');
                    if (eq < 0) continue;
                    String key = kv.substring(0, eq).trim();
                    String value = kv.substring(eq + 1).trim();
                    for (IProperty<?> p : block.getBlockState().getProperties()) {
                        if (!p.getName().equals(key)) continue;
                        state = withValue(state, p, value);
                    }
                }
            }
            return format(StatesBridge1122.toModern(state));
        } catch (RuntimeException | LinkageError e) {
            return spec;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IBlockState withValue(IBlockState state, IProperty p, String value) {
        var parsed = p.parseValue(value);
        return parsed.isPresent() ? state.withProperty(p, (Comparable) parsed.get()) : state;
    }

    /** {@code mod:block[a=b,c=d]} (without brackets for the default state's block when it has no properties). */
    public static String format(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (state.getValues().isEmpty()) return id;
        StringBuilder sb = new StringBuilder(id).append('[');
        boolean first = true;
        for (var entry : state.getValues().entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(entry.getKey().getName()).append('=').append(valueName(entry.getKey(), entry.getValue()));
        }
        return sb.append(']').toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String valueName(net.minecraft.world.level.block.state.properties.Property p, Comparable value) {
        return p.getName(value);
    }

    /** A modern block of a legacy one (its default state's). */
    public static net.minecraft.world.level.block.Block modern(Block block) {
        return block == null ? null : BlockBridge1122.modern(block);
    }

    /** {@code mod:name} of an entity with the id a 1.12 mod registered under; the modern id the layer gave it when known. */
    public static String entity(String id) {
        return Ids.normalizePath(id);
    }

    /** A recipe or other resource id as the modern namespace allows it. */
    public static String resource(String id) {
        int c = id.indexOf(':');
        String ns = c < 0 ? "minecraft" : id.substring(0, c);
        String path = c < 0 ? id : id.substring(c + 1);
        return Ids.normalizePath(ns) + ":" + Ids.normalizePath(path);
    }

    /** The modern id for a resource location. */
    public static ResourceLocation rl(String id) {
        return ResourceLocation.parse(resource(id));
    }
}
