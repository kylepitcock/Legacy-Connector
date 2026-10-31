package org.kylepitcock.legacyconnector.v1_7_10;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.PackType;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;

/**
 * Generated data for 1.7.10 content (recipes and ore dictionary tags are the recipe pack: {@code RecipeBridge}): mining tags derived from legacy
 * {@code setHarvestLevel(tool, level)} so modern tools mine legacy blocks at the right speed and only drop items
 * when the legacy harvest level is met.
 */
public final class Data1710 implements GeneratedPack.Contributor {
    private static final String[] TIERS = {null, "minecraft:needs_stone_tool", "minecraft:needs_iron_tool", "minecraft:needs_diamond_tool"};

    @Override
    public void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyBiomes.contribute(out);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyWorldPresets.contributeData(out);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyEntitySpawns.contribute(out);
        org.kylepitcock.legacyconnector.v1_7_10.dimension.LegacyDimensions.contribute(out);
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) org.kylepitcock.legacyconnector.compat.curios.CuriosBaubles.data(out);
        Map<String, Set<String>> tags = new LinkedHashMap<>();
        for (Block b : BlockBridge.modBlocks()) {
            if (b.mcc$modern == null) continue;
            tagBlock(b, BuiltInRegistries.BLOCK.getKey(b.mcc$modern).toString(), deepId(b), 0, tags);
            // flattened subtype blocks (BlockFlattening1710): the harvest tool of their own metadata
            if (b.mcc$modern instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter a) {
                for (var member : a.members()) {
                    try {
                        tagBlock(b, BuiltInRegistries.BLOCK.getKey(member).toString(), null, member.keyMeta(), tags);
                    } catch (RuntimeException | LinkageError e) {
                        // a harvest rule that needs a world: the subtype block has no tool tags
                    }
                }
            }
        }
        for (var e : tags.entrySet()) {
            String[] rl = e.getKey().split(":");
            JsonObject json = new JsonObject();
            json.addProperty("replace", false);
            JsonArray values = new JsonArray();
            e.getValue().forEach(values::add);
            json.add("values", values);
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/" + rl[0] + "/tags/block/" + rl[1] + ".json", () -> bytes);
        }
    }

    /** The mineable and tool tier tags of one modern block of a legacy block, from the legacy harvest rule of {@code meta}. */
    private static void tagBlock(Block b, String id, String deepId, int meta, Map<String, Set<String>> tags) {
        {
            String tool = b.getHarvestTool(meta);
            int level = b.getHarvestLevel(meta);
            if (tool == null) {
                // Legacy blocks without a declared tool fell back to material rules: rock/iron need a pickaxe.
                if (!b.getMaterial().isToolNotRequired()) tool = "pickaxe";
                else return;
            }
            String mineable = switch (tool) {
                case "pickaxe" -> "minecraft:mineable/pickaxe";
                case "axe" -> "minecraft:mineable/axe";
                case "shovel" -> "minecraft:mineable/shovel";
                case "hoe" -> "minecraft:mineable/hoe";
                default -> null;
            };
            if (mineable != null) {
                Set<String> set = tags.computeIfAbsent(mineable, k -> new LinkedHashSet<>());
                set.add(id);
                if (deepId != null) set.add(deepId);
            }
            if (level >= 1) {
                Set<String> set = tags.computeIfAbsent(TIERS[Math.min(level, 3)], k -> new LinkedHashSet<>());
                set.add(id);
                if (deepId != null) set.add(deepId);
            }
        }
    }

    private static String deepId(Block b) {
        if (!(b.mcc$modern instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter a) || a.deepVariant() == null) return null;
        return BuiltInRegistries.BLOCK.getKey(a.deepVariant()).toString();
    }

    @Override
    public String toString() {
        return "1.7.10 data";
    }
}
