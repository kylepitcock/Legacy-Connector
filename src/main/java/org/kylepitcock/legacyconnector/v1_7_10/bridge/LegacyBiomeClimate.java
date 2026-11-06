package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraftforge.common.BiomeDictionary;
import legacyconnector.v1_7_10.net.minecraftforge.common.BiomeDictionary.Type;
import legacyconnector.v1_7_10.net.minecraftforge.common.BiomeManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.kylepitcock.legacyconnector.core.worldgen.BiomePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Climate placement of 1.7.10 mod biomes for {@link BiomePlacement}: which vanilla land biome each one is modelled on (from its
 * Forge {@code BiomeDictionary} tags, the {@code BiomeManager} climate lists it is in, temperature, rainfall, height and tree
 * count) and its weight (the sum of its {@code BiomeManager} entry weights, as GenLayerBiome drew from those lists). Mod biomes in
 * no {@code BiomeManager} list never generated in a 1.7.10 default overworld (they belong to the mod's dimension or world type)
 * and are not placed.
 */
public final class LegacyBiomeClimate {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Biomes");
    private static volatile String lastLogged = "";

    private LegacyBiomeClimate() {}

    public static List<BiomePlacement.Entry> entries() {
        List<BiomePlacement.Entry> out = new ArrayList<>();
        List<String> unplaced = new ArrayList<>();
        for (BiomeGenBase b : LegacyBiomes.modBiomes()) {
            try {
                Set<BiomeManager.BiomeType> lists = EnumSet.noneOf(BiomeManager.BiomeType.class);
                int weight = 0;
                for (BiomeManager.BiomeType type : BiomeManager.BiomeType.values()) {
                    for (BiomeManager.BiomeEntry e : BiomeManager.getBiomes(type)) {
                        if (e.biome != b) continue;
                        weight += Math.max(0, e.itemWeight);
                        lists.add(type);
                    }
                }
                ResourceKey<Biome> analog = weight > 0 ? analogFor(b, lists) : null;
                if (analog == null) {
                    unplaced.add(b.mcc$modernId().toString());
                    continue;
                }
                out.add(new BiomePlacement.Entry(ResourceKey.create(Registries.BIOME, b.mcc$modernId()), analog, weight));
            } catch (RuntimeException | LinkageError e) {
                unplaced.add(b.mcc$modernId() + " (" + e + ")");
            }
        }
        spreadAnalogs(out);
        String summary = describe(out) + "|" + unplaced;
        if (!summary.equals(lastLogged)) { // once per distinct result, not per server start or region query
            lastLogged = summary;
            if (!out.isEmpty()) LOG.info("Legacy biome climate placement (biome -> vanilla analog, weight): {}", describe(out));
            if (!unplaced.isEmpty()) LOG.info("Legacy biomes in no BiomeManager list (left to their dimension or world type): {}", unplaced);
        }
        return out;
    }

    /** Vanilla biomes of the same family that can stand in for an analog already taken by another legacy biome. */
    private static final java.util.Map<ResourceKey<Biome>, List<ResourceKey<Biome>>> SIBLINGS = java.util.Map.of(
            Biomes.FOREST, List.of(Biomes.BIRCH_FOREST, Biomes.FLOWER_FOREST, Biomes.DARK_FOREST),
            Biomes.PLAINS, List.of(Biomes.SUNFLOWER_PLAINS),
            Biomes.TAIGA, List.of(Biomes.OLD_GROWTH_PINE_TAIGA),
            Biomes.JUNGLE, List.of(Biomes.SPARSE_JUNGLE),
            Biomes.SWAMP, List.of(Biomes.MANGROVE_SWAMP),
            Biomes.SNOWY_PLAINS, List.of(Biomes.SNOWY_TAIGA));

    /**
     * Distinct-analog preference: legacy biomes of one mod share their analogs easily (Magical Forest and Tainted Land are both
     * forests), which stacks them in the same climate patch. The heavier biome keeps the analog, the others move to an unused sibling
     * of that vanilla biome when there is one, so legacy biomes spread across the climate map.
     */
    static void spreadAnalogs(List<BiomePlacement.Entry> entries) {
        entries.sort(java.util.Comparator.comparingInt((BiomePlacement.Entry e) -> -e.weight()).thenComparing(e -> e.biome().location()));
        java.util.Set<ResourceKey<Biome>> used = new java.util.HashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            BiomePlacement.Entry e = entries.get(i);
            ResourceKey<Biome> analog = e.analog();
            if (!used.add(analog)) {
                for (ResourceKey<Biome> alt : SIBLINGS.getOrDefault(analog, List.of())) {
                    if (used.add(alt)) {
                        entries.set(i, new BiomePlacement.Entry(e.biome(), alt, e.weight()));
                        break;
                    }
                }
            }
        }
    }

    private static String describe(List<BiomePlacement.Entry> entries) {
        StringBuilder sb = new StringBuilder();
        for (BiomePlacement.Entry e : entries) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(e.biome().location()).append(" -> ").append(e.analog().location().getPath()).append(' ').append(e.weight());
        }
        return sb.toString();
    }

    /**
     * The vanilla overworld land biome whose climate a legacy biome shares, or null for a water, shore, Nether or End biome (the
     * legacy layers left water and shores to the terrain too). Dictionary tags decide first, then temperature and rainfall, then
     * the BiomeManager climate list.
     */
    static ResourceKey<Biome> analogFor(BiomeGenBase b, Set<BiomeManager.BiomeType> lists) {
        Set<Type> t = EnumSet.noneOf(Type.class);
        try {
            for (Type type : BiomeDictionary.getTypesForBiome(b)) if (type != null) t.add(type);
        } catch (RuntimeException | LinkageError ignored) {
            // no tags: climate values only
        }
        float temp = b.temperature;
        float rain = b.rainfall;
        int trees = b.theBiomeDecorator == null ? 0 : b.theBiomeDecorator.treesPerChunk;
        boolean wooded = t.contains(Type.FOREST) || t.contains(Type.CONIFEROUS) || t.contains(Type.JUNGLE) || trees >= 2;
        boolean land = t.contains(Type.PLAINS) || t.contains(Type.FOREST) || t.contains(Type.HILLS) || t.contains(Type.MOUNTAIN);
        if (t.contains(Type.NETHER) || t.contains(Type.END)) return null;
        if (!land && (t.contains(Type.OCEAN) || t.contains(Type.RIVER) || t.contains(Type.BEACH) || b.rootHeight < -0.5F)) return null;
        if (t.contains(Type.MUSHROOM)) return Biomes.MUSHROOM_FIELDS;
        if (t.contains(Type.MESA)) return wooded || trees > 0 ? Biomes.WOODED_BADLANDS : Biomes.BADLANDS;
        if (t.contains(Type.SNOWY) || t.contains(Type.FROZEN) || temp < 0.15F || b.getEnableSnow() || (lists.contains(BiomeManager.BiomeType.ICY) && lists.size() == 1)) {
            if (t.contains(Type.MOUNTAIN)) return Biomes.GROVE;
            return wooded ? Biomes.SNOWY_TAIGA : Biomes.SNOWY_PLAINS;
        }
        if (t.contains(Type.JUNGLE) || (temp >= 0.95F && rain >= 0.8F)) return Biomes.JUNGLE;
        if (t.contains(Type.SWAMP) || (rain >= 0.85F && temp >= 0.5F && b.rootHeight <= 0.0F)) return Biomes.SWAMP;
        if (t.contains(Type.SAVANNA)) return Biomes.SAVANNA;
        boolean sandy = t.contains(Type.SANDY) || t.contains(Type.DESERT) || b.topBlock == Blocks.sand;
        if ((sandy && (t.contains(Type.HOT) || t.contains(Type.DRY))) || (temp >= 1.0F && rain <= 0.2F)) return sandy ? Biomes.DESERT : Biomes.SAVANNA;
        if (t.contains(Type.MOUNTAIN) || b.rootHeight >= 1.0F) return wooded ? Biomes.WINDSWEPT_FOREST : Biomes.WINDSWEPT_HILLS;
        if (t.contains(Type.CONIFEROUS) || (wooded && (t.contains(Type.COLD) || temp <= 0.3F))) {
            return t.contains(Type.DENSE) ? Biomes.OLD_GROWTH_SPRUCE_TAIGA : Biomes.TAIGA;
        }
        if (t.contains(Type.SPOOKY) || (t.contains(Type.DENSE) && wooded)) return Biomes.DARK_FOREST;
        if (wooded) return Biomes.FOREST;
        if (lists.contains(BiomeManager.BiomeType.DESERT) && lists.size() == 1) return temp >= 1.0F ? Biomes.SAVANNA : Biomes.PLAINS;
        if (temp <= 0.3F || (lists.contains(BiomeManager.BiomeType.COOL) && lists.size() == 1)) return trees > 0 ? Biomes.TAIGA : Biomes.PLAINS;
        return Biomes.PLAINS;
    }
}
