package org.kylepitcock.legacyconnector.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.junit.jupiter.api.Test;

class BiomePlacementTest {
    private static final ResourceKey<Biome> PLAINS =
            ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("plains"));
    private static final ResourceKey<Biome> FOREST =
            ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("forest"));
    private static final ResourceKey<Biome> LEGACY_PLAINS =
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("test", "legacy_plains"));

    private static Climate.ParameterPoint point(long humidityMin, long humidityMax) {
        Climate.Parameter zero = new Climate.Parameter(0, 0);
        return new Climate.ParameterPoint(zero, new Climate.Parameter(humidityMin, humidityMax),
                zero, zero, zero, zero, 0L);
    }

    private static List<Pair<Climate.ParameterPoint, String>> base() {
        return List.of(
                Pair.of(point(-100, 100), "minecraft:plains"),
                Pair.of(point(-100, 100), "minecraft:forest"));
    }

    private static Map<String, ResourceKey<Biome>> keyMap() {
        return Map.of("minecraft:plains", PLAINS, "minecraft:forest", FOREST);
    }

    @Test
    void overlaysLegacyBiomeOnItsAnalogAndPreservesUnrelatedEntries() {
        List<Pair<Climate.ParameterPoint, String>> base = base();
        var blended = BiomePlacement.blend(base, List.of(new BiomePlacement.Entry(LEGACY_PLAINS, PLAINS, 10)),
                keyMap()::get, key -> key.location().toString(), 1.0);

        assertEquals(4, blended.size());
        assertEquals(2, blended.stream().filter(p -> p.getSecond().equals("minecraft:plains")).count());
        assertEquals(1, blended.stream().filter(p -> p.getSecond().equals("test:legacy_plains")).count());
        assertSame(base.get(1), blended.get(3), "unrelated biome entries should be preserved without rewriting");
        assertEquals("minecraft:forest", blended.get(3).getSecond());
    }

    @Test
    void zeroFrequencyLeavesTheNativeClimateListUnchanged() {
        List<Pair<Climate.ParameterPoint, String>> base = base();
        var blended = BiomePlacement.blend(base, List.of(new BiomePlacement.Entry(LEGACY_PLAINS, PLAINS, 10)),
                keyMap()::get, key -> key.location().toString(), 0.0);

        assertEquals(base, blended);
        assertSame(base.get(0), blended.get(0));
        assertSame(base.get(1), blended.get(1));
    }

    @Test
    void narrowClimateIntervalsRemainUntouchedWhenTheyCannotBeSplitSafely() {
        var narrow = List.of(Pair.of(point(0, 2), "minecraft:plains"));
        var blended = BiomePlacement.blend(narrow, List.of(new BiomePlacement.Entry(LEGACY_PLAINS, PLAINS, 10)),
                keyMap()::get, key -> key.location().toString(), 1.0);

        assertEquals(narrow, blended);
        assertSame(narrow.get(0), blended.get(0));
        assertTrue(blended.stream().noneMatch(p -> p.getSecond().equals("test:legacy_plains")));
    }
}
