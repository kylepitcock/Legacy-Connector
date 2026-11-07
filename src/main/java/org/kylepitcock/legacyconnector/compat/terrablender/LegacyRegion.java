package org.kylepitcock.legacyconnector.compat.terrablender;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.kylepitcock.legacyconnector.core.worldgen.BiomePlacement;
import org.slf4j.LoggerFactory;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.Regions;

/**
 * The legacy biomes as a TerraBlender overworld region, the way modern biome mods (Biomes O' Plenty, Regions Unexplored) add
 * theirs: the vanilla climate layout in which each legacy biome takes the parameter points of the vanilla biome it is modelled
 * on (shared by weight when several legacy biomes have the same analog). TerraBlender picks between this region, the vanilla
 * one and other mods' regions by the region weights.
 *
 * <p>TerraBlender is a required dependency (embedded as jar-in-jar, a standalone install wins).
 */
public final class LegacyRegion extends Region {
    public static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_overworld");

    private LegacyRegion(int weight) {
        super(NAME, RegionType.OVERWORLD, weight);
    }

    public static void register(int weight) {
        Regions.register(new LegacyRegion(weight));
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        try {
            List<BiomePlacement.Entry> entries = BiomePlacement.entries().stream()
                    .filter(e -> registry.containsKey(e.biome()) && registry.containsKey(e.analog())).toList();
            List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> points =
                    BiomePlacement.blend(BiomePlacement.vanillaOverworld(), entries, k -> k, k -> k, org.kylepitcock.legacyconnector.core.LegacyConfig.legacyBiomeFrequency());
            points.forEach(mapper);
        } catch (RuntimeException | LinkageError e) {
            // Must never break world loading: fall back to the plain vanilla layout.
            LoggerFactory.getLogger("LegacyConnector/Biomes").error("Legacy TerraBlender region failed; it uses the vanilla layout", e);
            BiomePlacement.vanillaOverworld().forEach(mapper);
        }
    }
}
