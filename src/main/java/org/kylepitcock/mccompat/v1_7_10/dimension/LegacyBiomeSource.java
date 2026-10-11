package org.kylepitcock.mccompat.v1_7_10.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import mccompat.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import mccompat.v1_7_10.net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.kylepitcock.mccompat.v1_7_10.bridge.LegacyBiomes;
import org.kylepitcock.mccompat.v1_7_10.bridge.LegacyEntitySpawns;

/** Biomes of a mod dimension: its provider's legacy chunk manager, mapped to the modern biomes of the legacy ones. */
public class LegacyBiomeSource extends BiomeSource {
    public static final MapCodec<LegacyBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("dimension").forGetter(s -> s.dimension),
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(s -> s.biomes)).apply(i, LegacyBiomeSource::new));

    private final int dimension;
    private final HolderSet<Biome> biomes;
    private volatile Map<Integer, Holder<Biome>> byLegacyId;
    private volatile WorldChunkManager manager;

    public LegacyBiomeSource(int dimension, HolderSet<Biome> biomes) {
        this.dimension = dimension;
        this.biomes = biomes;
    }

    /** Modern ids of every biome a legacy biome can generate as (vanilla ones and the mods'). */
    public static List<String> allBiomeIds() {
        List<String> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (BiomeGenBase b : LegacyBiomes.modBiomes()) {
            String id = b.mcc$modernId().toString();
            if (seen.add(id)) out.add(id);
        }
        for (BiomeGenBase b : BiomeGenBase.getBiomeGenArray()) {
            if (b == null || b.mcc$isModBiome()) continue;
            ResourceLocation id = LegacyEntitySpawns.modernBiome(b);
            if (id != null && seen.add(id.toString())) out.add(id.toString());
        }
        return out;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    private Map<Integer, Holder<Biome>> byLegacyId() {
        Map<Integer, Holder<Biome>> map = byLegacyId;
        if (map == null) {
            Map<ResourceLocation, Holder<Biome>> byKey = new HashMap<>();
            for (Holder<Biome> h : biomes) h.unwrapKey().ifPresent(k -> byKey.put(k.location(), h));
            map = new HashMap<>();
            for (BiomeGenBase b : BiomeGenBase.getBiomeGenArray()) {
                if (b == null) continue;
                ResourceLocation id = LegacyEntitySpawns.modernBiome(b);
                Holder<Biome> h = id == null ? null : byKey.get(id);
                if (h != null) map.put(b.biomeID, h);
            }
            byLegacyId = map;
        }
        return map;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        WorldChunkManager m = manager;
        if (m == null) {
            var provider = LegacyDimensions.provider(dimension);
            if (provider != null) manager = m = provider.worldChunkMgr;
        }
        Map<Integer, Holder<Biome>> map = byLegacyId();
        Holder<Biome> fallback = map.get(1); // plains
        if (fallback == null) fallback = biomes.stream().findFirst().orElseThrow();
        if (m == null) return fallback;
        BiomeGenBase biome;
        synchronized (m) { // legacy layers keep per-call state
            biome = m.getBiomeGenAt((qx << 2) + 2, (qz << 2) + 2);
        }
        Holder<Biome> h = biome == null ? null : map.get(biome.biomeID);
        return h == null ? fallback : h;
    }
}
