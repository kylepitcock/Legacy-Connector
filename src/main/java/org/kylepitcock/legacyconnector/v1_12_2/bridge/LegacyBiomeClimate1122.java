package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import legacyconnector.v1_12_2.net.minecraftforge.common.BiomeDictionary;
import legacyconnector.v1_12_2.net.minecraftforge.common.BiomeDictionary.Type;
import legacyconnector.v1_12_2.net.minecraftforge.common.BiomeManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.kylepitcock.legacyconnector.core.worldgen.BiomePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.12.2 mod biomes in the default overworld, placed the way 1.7.10's are (wiki World-Generation): a biome a mod put in a
 * {@code BiomeManager} climate list (GenLayerBiome drew the overworld's biomes from those lists) becomes a climate point next to
 * a vanilla analog, through TerraBlender's region or the native multi-noise overlay. Weight = the sum of its entry weights.
 * Biomes in no list belong to the mod's dimension or world type and are not placed.
 */
public final class LegacyBiomeClimate1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Biomes1122");
    private static boolean registered;
    private static volatile String lastLogged = "";

    private LegacyBiomeClimate1122() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        BiomePlacement.setEntries(LegacyBiomeClimate1122::entries);
        LegacyBiomes1122.registerDecoration();
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> bind(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> BiomePlacement.clearNative());
    }

    private static void bind(MinecraftServer server) {
        BiomePlacement.clearNative();
        try {
            var stem = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
            if (stem == null || BiomePlacement.hasTerraBlenderRegion()) return;
            if (stem.generator().getBiomeSource() instanceof MultiNoiseBiomeSource source && BiomePlacement.bindNative(server, source)) {
                LOG.info("1.12.2 mod biomes placed through the native multi-noise overlay");
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.error("1.12.2 biome placement could not be set up; mod biomes may not generate in the overworld", e);
        }
    }

    public static List<BiomePlacement.Entry> entries() {
        List<BiomePlacement.Entry> out = new ArrayList<>();
        List<String> unplaced = new ArrayList<>();
        for (Biome b : LegacyBiomes1122.modBiomes()) {
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
                ResourceKey<net.minecraft.world.level.biome.Biome> analog = weight > 0 ? analogFor(b, lists) : null;
                if (analog == null) {
                    unplaced.add(String.valueOf(BiomeBridge1122.modernId(b)));
                    continue;
                }
                out.add(new BiomePlacement.Entry(ResourceKey.create(Registries.BIOME, BiomeBridge1122.modernId(b)), analog, weight));
            } catch (RuntimeException | LinkageError e) {
                unplaced.add(BiomeBridge1122.modernId(b) + " (" + e + ")");
            }
        }
        String summary = out.size() + "|" + unplaced;
        if (!summary.equals(lastLogged)) {
            lastLogged = summary;
            StringBuilder sb = new StringBuilder();
            for (var e : out) sb.append(sb.isEmpty() ? "" : ", ").append(e.biome().location()).append(" -> ").append(e.analog().location().getPath()).append(' ').append(e.weight());
            if (!out.isEmpty()) LOG.info("1.12.2 biome climate placement (biome -> vanilla analog, weight): {}", sb);
            if (!unplaced.isEmpty()) LOG.info("1.12.2 biomes in no BiomeManager list (left to their dimension or world type): {}", unplaced);
        }
        return out;
    }

    static ResourceKey<net.minecraft.world.level.biome.Biome> analogFor(Biome b, Set<BiomeManager.BiomeType> lists) {
        Set<Type> t = new HashSet<>();
        try {
            for (Type type : BiomeDictionary.getTypes(b)) if (type != null) t.add(type);
        } catch (RuntimeException | LinkageError ignored) {
            // no tags: climate values only
        }
        float temp = b.getDefaultTemperature();
        float rain = b.getRainfall();
        int trees = b.decorator == null ? 0 : b.decorator.treesPerChunk;
        boolean wooded = t.contains(Type.FOREST) || t.contains(Type.CONIFEROUS) || t.contains(Type.JUNGLE) || trees >= 2;
        boolean land = t.contains(Type.PLAINS) || t.contains(Type.FOREST) || t.contains(Type.HILLS) || t.contains(Type.MOUNTAIN);
        if (t.contains(Type.NETHER) || t.contains(Type.END) || t.contains(Type.VOID)) return null;
        if (!land && (t.contains(Type.OCEAN) || t.contains(Type.RIVER) || t.contains(Type.BEACH) || b.getBaseHeight() < -0.5F)) return null;
        if (t.contains(Type.MUSHROOM)) return Biomes.MUSHROOM_FIELDS;
        if (t.contains(Type.MESA)) return wooded || trees > 0 ? Biomes.WOODED_BADLANDS : Biomes.BADLANDS;
        if (t.contains(Type.SNOWY) || temp < 0.15F || b.getEnableSnow() || (lists.contains(BiomeManager.BiomeType.ICY) && lists.size() == 1)) {
            if (t.contains(Type.MOUNTAIN)) return Biomes.GROVE;
            return wooded ? Biomes.SNOWY_TAIGA : Biomes.SNOWY_PLAINS;
        }
        if (t.contains(Type.JUNGLE) || (temp >= 0.95F && rain >= 0.8F)) return Biomes.JUNGLE;
        if (t.contains(Type.SWAMP) || (rain >= 0.85F && temp >= 0.5F && b.getBaseHeight() <= 0.0F)) return Biomes.SWAMP;
        if (t.contains(Type.SAVANNA)) return Biomes.SAVANNA;
        boolean sandy = t.contains(Type.SANDY);
        if ((sandy && (t.contains(Type.HOT) || t.contains(Type.DRY))) || (temp >= 1.0F && rain <= 0.2F)) return sandy ? Biomes.DESERT : Biomes.SAVANNA;
        if (t.contains(Type.MOUNTAIN) || b.getBaseHeight() >= 1.0F) return wooded ? Biomes.WINDSWEPT_FOREST : Biomes.WINDSWEPT_HILLS;
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
