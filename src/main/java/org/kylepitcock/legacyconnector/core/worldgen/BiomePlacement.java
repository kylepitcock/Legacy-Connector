package org.kylepitcock.legacyconnector.core.worldgen;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Places legacy mod biomes in the overworld the way a modern biome mod does: as a TerraBlender region (TerraBlender is a required
 * dependency, embedded as jar-in-jar; it is the one placement path, so Terralith, Tectonic and other worldgen mods decide the layout).
 * Each legacy biome is modelled on a vanilla land biome (its "analog", chosen by the legacy family from the
 * legacy climate and type tags) and takes a share of that biome's parameter points, split along the humidity axis in
 * proportion to its legacy weight.
 * The region ({@code compat.terrablender.LegacyRegion}) is the vanilla layout with each analog's points shared among the legacy
 * biomes modelled on it, weighted like the other regions of the pack.
 */
public final class BiomePlacement {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Biomes");

    /** A legacy biome, the vanilla biome whose climate points it shares, and its legacy generation weight (> 0). */
    public record Entry(ResourceKey<Biome> biome, ResourceKey<Biome> analog, int weight) {}

    private static volatile Supplier<List<Entry>> entries = List::of;
    private static boolean terraBlenderRegistered;

    private BiomePlacement() {}

    /** Installed by a legacy family once its biomes and their weights are known (after postInit). */
    public static synchronized void setEntries(Supplier<List<Entry>> supplier) {
        entries = supplier;
    }

    /** The legacy biomes to place in the overworld (empty when there are none or they cannot be read). */
    public static List<Entry> entries() {
        try {
            List<Entry> list = entries.get();
            return list == null ? List.of() : list;
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Legacy biome placement data could not be read: {}", e.toString());
            return List.of();
        }
    }

    private static boolean commonSetupDone;

    /**
     * TerraBlender's regions may only be registered from common setup (enqueued work): touching {@code terrablender.api.Regions}
     * earlier runs its static setup before TerraBlender's config is loaded, which fails and breaks the class for every mod (the
     * legacy biomes registered it right after legacy loading, during the registry events, and VanillaBackport crashed in ATM10).
     */
    public static void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            synchronized (BiomePlacement.class) {
                commonSetupDone = true;
            }
            registerTerraBlender();
        });
    }

    /** Registers the TerraBlender region (once, from common setup, after the entries are known). */
    public static synchronized void registerTerraBlender() {
        if (terraBlenderRegistered || !commonSetupDone) return;
        List<Entry> list = entries();
        if (list.isEmpty() || org.kylepitcock.legacyconnector.core.LegacyConfig.legacyBiomeFrequency() <= 0) return;
        terraBlenderRegistered = true;
        int total = 0;
        for (Entry e : list) total += e.weight();
        // TerraBlender's vanilla region has weight 10 by default: legacy biomes of total weight 30 make a region as common as it.
        int weight = Math.max(1, Math.min(12, Math.round((float) (total / 3.0F * org.kylepitcock.legacyconnector.core.LegacyConfig.legacyBiomeFrequency()))));
        try {
            org.kylepitcock.legacyconnector.compat.terrablender.LegacyRegion.register(weight);
            LOG.info("Legacy biomes: TerraBlender overworld region registered (weight {}, {} biome(s))", weight, list.size());
        } catch (RuntimeException | LinkageError e) {
            LOG.error("TerraBlender region for legacy biomes could not be registered; legacy biomes will not generate in the overworld", e);
        }
    }

    /** The vanilla overworld climate layout (parameter points to biome keys), independent of any registry. */
    public static List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> vanillaOverworld() {
        return MultiNoiseBiomeSourceParameterList.knownPresets().get(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD).values();
    }

    /**
     * Weight the vanilla analog keeps in each of its parameter points: a vanilla biome is in two of Forge 1.7.10's
     * {@code BiomeManager} climate lists with weight 10 each, so a legacy biome of weight 10 takes a third of its analog's points.
     */
    public static final int ANALOG_WEIGHT = 20;

    /**
     * {@code base} with the points of every analog shared among the legacy biomes modelled on it. The humidity interval of each
     * such point is cut into consecutive pieces: the analog keeps {@code ANALOG_WEIGHT} parts, each legacy biome gets its weight
     * (times {@code frequency}) in parts, and the pieces alternate analog / legacy / analog / legacy... so two legacy biomes of
     * the same analog are never neighbours. Points of other biomes are kept as they are.
     */
    public static <T> List<Pair<Climate.ParameterPoint, T>> blend(List<Pair<Climate.ParameterPoint, T>> base, List<Entry> entries,
            Function<T, ResourceKey<Biome>> keyOf, Function<ResourceKey<Biome>, T> valueOf, double frequency) {
        Map<ResourceKey<Biome>, List<Entry>> byAnalog = new LinkedHashMap<>();
        entries.stream().sorted(Comparator.comparing(e -> e.biome().location()))
                .forEach(e -> byAnalog.computeIfAbsent(e.analog(), k -> new ArrayList<>()).add(e));
        Map<ResourceKey<Biome>, T> values = new HashMap<>();
        List<Pair<Climate.ParameterPoint, T>> out = new ArrayList<>(base.size() + entries.size() * 64);
        for (Pair<Climate.ParameterPoint, T> pair : base) {
            ResourceKey<Biome> key = keyOf.apply(pair.getSecond());
            List<Entry> group = key == null ? null : byAnalog.get(key);
            if (group == null || frequency <= 0) {
                out.add(pair);
                continue;
            }
            Climate.ParameterPoint p = pair.getFirst();
            long min = p.humidity().min();
            long max = p.humidity().max();
            int n = group.size();
            if (max - min < 2L * n + 1) { // too narrow to cut: the analog keeps it
                out.add(pair);
                continue;
            }
            // the analog's weight is split into n+1 gaps around the legacy pieces
            double legacyTotal = 0;
            for (Entry e : group) legacyTotal += Math.max(1, e.weight()) * frequency;
            double gap = ANALOG_WEIGHT / (double) (n + 1);
            double total = legacyTotal + ANALOG_WEIGHT;
            long span = max - min;
            double cumulative = 0;
            long lo = min;
            for (int i = 0; i <= n; i++) {
                cumulative += gap;
                long hi = i == n ? max : Math.max(lo + 1, min + (long) (span * cumulative / total));
                out.add(Pair.of(withHumidity(p, lo, hi), pair.getSecond()));
                lo = hi;
                if (i == n) break;
                Entry e = group.get(i);
                cumulative += Math.max(1, e.weight()) * frequency;
                hi = Math.max(lo + 1, min + (long) (span * cumulative / total));
                if (hi > max) hi = max;
                out.add(Pair.of(withHumidity(p, lo, hi), values.computeIfAbsent(e.biome(), valueOf)));
                lo = hi;
            }
        }
        return out;
    }

    private static Climate.ParameterPoint withHumidity(Climate.ParameterPoint p, long min, long max) {
        return new Climate.ParameterPoint(p.temperature(), new Climate.Parameter(min, max), p.continentalness(), p.erosion(), p.depth(),
                p.weirdness(), p.offset());
    }
}
