package org.kylepitcock.legacyconnector.core.worldgen;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Places legacy mod biomes in the overworld the way a modern biome mod does: as climate parameter points of the multi-noise
 * biome source. Each legacy biome is modelled on a vanilla land biome (its "analog", chosen by the legacy family from the
 * legacy climate and type tags) and takes a share of that biome's parameter points, split along the humidity axis in
 * proportion to its legacy weight.
 * <ul>
 * <li>With TerraBlender, the points go into a TerraBlender overworld region ({@code compat.terrablender.LegacyRegion}): the vanilla
 * layout with each analog's points shared among the legacy biomes modelled on it, weighted like the other regions of the pack.</li>
 * <li>Without it, the points are added to the {@code minecraft:overworld} multi-noise parameter list (the vanilla preset the
 * overworld's biome source refers to) when the server starts, next to the vanilla biomes, which keep the rest of each point.</li>
 * </ul>
 */
public final class BiomePlacement {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Biomes");
    /**
     * Weight the vanilla analog keeps in the parameter list (no TerraBlender): a vanilla biome is in two of Forge 1.7.10's
     * {@code BiomeManager} climate lists with weight 10 each, so a legacy biome of weight 10 takes a third of its analog's points.
     */
    public static final int VANILLA_ANALOG_WEIGHT = 20;

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

    public static boolean terraBlender() {
        return ModList.get() != null && ModList.get().isLoaded("terrablender");
    }

    /** Registers the TerraBlender region when TerraBlender is installed (once, after the entries are known). */
    public static synchronized void registerTerraBlender() {
        if (terraBlenderRegistered || !terraBlender()) return;
        List<Entry> list = entries();
        if (list.isEmpty()) return;
        terraBlenderRegistered = true;
        int total = 0;
        for (Entry e : list) total += e.weight();
        // TerraBlender's vanilla region has weight 10 by default: ten legacy biomes of weight 10 make a region as common as it.
        int weight = Math.max(1, Math.min(15, Math.round(total / 10.0F)));
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
     * {@code base} with the points of every analog shared among the legacy biomes modelled on it. The humidity interval of each
     * such point is cut into consecutive pieces: the analog keeps {@code analogWeight} parts (0 = replaced completely), each legacy
     * biome gets its weight in parts. Points of other biomes are kept as they are.
     */
    public static <T> List<Pair<Climate.ParameterPoint, T>> blend(List<Pair<Climate.ParameterPoint, T>> base, List<Entry> entries,
            Function<T, ResourceKey<Biome>> keyOf, Function<ResourceKey<Biome>, T> valueOf, int analogWeight) {
        Map<ResourceKey<Biome>, List<Entry>> byAnalog = new LinkedHashMap<>();
        entries.stream().sorted(Comparator.comparing(e -> e.biome().location()))
                .forEach(e -> byAnalog.computeIfAbsent(e.analog(), k -> new ArrayList<>()).add(e));
        Map<ResourceKey<Biome>, T> values = new HashMap<>();
        List<Pair<Climate.ParameterPoint, T>> out = new ArrayList<>(base.size() + entries.size() * 64);
        for (Pair<Climate.ParameterPoint, T> pair : base) {
            ResourceKey<Biome> key = keyOf.apply(pair.getSecond());
            List<Entry> group = key == null ? null : byAnalog.get(key);
            if (group == null) {
                out.add(pair);
                continue;
            }
            Climate.ParameterPoint p = pair.getFirst();
            long min = p.humidity().min();
            long max = p.humidity().max();
            int total = analogWeight;
            for (Entry e : group) total += Math.max(1, e.weight());
            int pieces = group.size() + (analogWeight > 0 ? 1 : 0);
            if (max - min < pieces) { // too narrow to cut: the first legacy biome takes it unless the analog keeps a share
                if (analogWeight > 0) out.add(pair);
                else out.add(Pair.of(p, values.computeIfAbsent(group.get(0).biome(), valueOf)));
                continue;
            }
            long span = max - min;
            int cumulative = 0;
            long lo = min;
            if (analogWeight > 0) {
                cumulative += analogWeight;
                long hi = min + span * cumulative / total;
                out.add(Pair.of(withHumidity(p, lo, hi), pair.getSecond()));
                lo = hi;
            }
            for (int i = 0; i < group.size(); i++) {
                Entry e = group.get(i);
                cumulative += Math.max(1, e.weight());
                long hi = i == group.size() - 1 ? max : min + span * cumulative / total;
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

    /**
     * Without TerraBlender: adds the legacy biomes to the server's {@code minecraft:overworld} multi-noise parameter list (a
     * per-server registry object, rebuilt from data on every world load), before any level exists. The overworld's biome source
     * refers to that list by id, so its saved form stays {@code preset: minecraft:overworld} and the vanilla biomes keep their
     * points (reduced by the legacy biomes' shares). Returns the number of legacy biomes added.
     */
    public static int injectIntoOverworldPreset(MinecraftServer server) {
        List<Entry> list = entries();
        if (list.isEmpty()) return 0;
        try {
            var access = server.registryAccess();
            MultiNoiseBiomeSourceParameterList preset = access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                    .get(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
            if (preset == null) {
                LOG.warn("No minecraft:overworld biome parameter list; legacy biomes are not placed in the overworld");
                return 0;
            }
            var biomes = access.registryOrThrow(Registries.BIOME);
            List<Entry> present = new ArrayList<>();
            for (Entry e : list) if (biomes.getHolder(e.biome()).isPresent() && biomes.getHolder(e.analog()).isPresent()) present.add(e);
            if (present.isEmpty()) return 0;
            Set<ResourceKey<Biome>> legacy = present.stream().map(Entry::biome).collect(Collectors.toSet());
            List<Pair<Climate.ParameterPoint, Holder<Biome>>> values = preset.parameters().values();
            if (values.stream().anyMatch(p -> p.getSecond().unwrapKey().map(legacy::contains).orElse(false))) return 0; // already added
            List<Pair<Climate.ParameterPoint, Holder<Biome>>> blended = blend(values, present, h -> h.unwrapKey().orElse(null),
                    k -> biomes.getHolderOrThrow(k), VANILLA_ANALOG_WEIGHT);
            preset.parameters = new Climate.ParameterList<>(List.copyOf(blended));
            checkOverworld(server, present);
            return present.size();
        } catch (RuntimeException | LinkageError e) {
            LOG.error("Legacy biomes could not be added to the overworld biome parameters; they will not generate in the overworld", e);
            return 0;
        }
    }

    /** Warns when the overworld does not use the preset (a data pack's own list) or listed its possible biomes too early. */
    private static void checkOverworld(MinecraftServer server, List<Entry> present) {
        LevelStem stem = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
        if (stem == null) return;
        if (!(stem.generator().getBiomeSource() instanceof MultiNoiseBiomeSource source) || !source.stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD)) {
            LOG.info("The overworld does not use the minecraft:overworld biome parameters ({}); legacy biomes appear only where it does",
                    stem.generator().getBiomeSource().getClass().getSimpleName());
            return;
        }
        ResourceKey<Biome> probe = present.get(0).biome();
        if (source.possibleBiomes().stream().noneMatch(h -> h.is(probe))) {
            LOG.warn("The overworld biome source listed its biomes before the legacy biomes were added; their decoration may not run");
        }
    }
}
