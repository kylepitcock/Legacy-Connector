package org.kylepitcock.mccompat.v1_7_10;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import mccompat.v1_7_10.net.minecraft.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.PackType;
import org.kylepitcock.mccompat.core.pack.GeneratedPack;
import org.kylepitcock.mccompat.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.RecipeBridge;

/**
 * Generated data for 1.7.10 content: recipes and ore dictionary tags, plus mining tags derived from legacy
 * {@code setHarvestLevel(tool, level)} so modern tools mine legacy blocks at the right speed and only drop items
 * when the legacy harvest level is met.
 */
public final class Data1710 implements GeneratedPack.Contributor {
    private static final String[] TIERS = {null, "minecraft:needs_stone_tool", "minecraft:needs_iron_tool", "minecraft:needs_diamond_tool"};

    @Override
    public void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        RecipeBridge.generate(out);
        org.kylepitcock.mccompat.v1_7_10.bridge.LegacyBiomes.contribute(out);
        org.kylepitcock.mccompat.v1_7_10.bridge.LegacyWorldPresets.contributeData(out);
        org.kylepitcock.mccompat.v1_7_10.bridge.LegacyEntitySpawns.contribute(out);
        org.kylepitcock.mccompat.v1_7_10.dimension.LegacyDimensions.contribute(out);
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) org.kylepitcock.mccompat.compat.curios.CuriosBaubles.data(out);
        Map<String, Set<String>> tags = new LinkedHashMap<>();
        for (Block b : BlockBridge.modBlocks()) {
            if (b.mcc$modern == null) continue;
            String id = BuiltInRegistries.BLOCK.getKey(b.mcc$modern).toString();
            String tool = b.getHarvestTool(0);
            int level = b.getHarvestLevel(0);
            if (tool == null) {
                // Legacy blocks without a declared tool fell back to material rules: rock/iron need a pickaxe.
                if (!b.getMaterial().isToolNotRequired()) tool = "pickaxe";
                else continue;
            }
            String mineable = switch (tool) {
                case "pickaxe" -> "minecraft:mineable/pickaxe";
                case "axe" -> "minecraft:mineable/axe";
                case "shovel" -> "minecraft:mineable/shovel";
                case "hoe" -> "minecraft:mineable/hoe";
                default -> null;
            };
            if (mineable != null) tags.computeIfAbsent(mineable, k -> new LinkedHashSet<>()).add(id);
            if (level >= 1) tags.computeIfAbsent(TIERS[Math.min(level, 3)], k -> new LinkedHashSet<>()).add(id);
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

    @Override
    public String toString() {
        return "1.7.10 data";
    }
}
