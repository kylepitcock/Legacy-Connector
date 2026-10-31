package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overworld biome source of the world presets of legacy mod world types ({@code legacyconnector:legacy_layers}, see
 * {@link LegacyWorldPresets}): the multi-noise source of the vanilla parameter list (so terrain, oceans, rivers, caves and other
 * mods' biomes, including TerraBlender regions, stay), with the mod world type's own 1.7.10 biome layers choosing the land
 * biomes, the way a modern mod's world preset brings its own biome source. Only worlds created with that preset use it.
 */
public class LegacyLayeredBiomeSource extends MultiNoiseBiomeSource {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Biomes");
    public static final MapCodec<LegacyLayeredBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            MultiNoiseBiomeSourceParameterList.CODEC.fieldOf("preset").forGetter(s -> s.preset),
            Codec.STRING.fieldOf("world_type").forGetter(s -> s.worldType),
            RegistryOps.retrieveGetter(Registries.BIOME)).apply(i, LegacyLayeredBiomeSource::new));

    private final Holder<MultiNoiseBiomeSourceParameterList> preset;
    private final String worldType;
    /** The biome registry the source was decoded with (legacy biomes are looked up, so a removed mod does not break the save). */
    private final HolderGetter<Biome> biomes;
    private volatile LegacyBiomeLayers layers;

    public LegacyLayeredBiomeSource(Holder<MultiNoiseBiomeSourceParameterList> preset, String worldType, HolderGetter<Biome> biomes) {
        super(Either.right(preset));
        this.preset = preset;
        this.worldType = worldType;
        this.biomes = biomes;
    }

    public String mcc$worldType() {
        return worldType;
    }

    /** Set when the server starts (null when it stops or the layers fail). */
    void mcc$bind(LegacyBiomeLayers layers) {
        this.layers = layers;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        List<Holder<Biome>> legacy = new ArrayList<>();
        try {
            for (BiomeGenBase b : LegacyBiomes.modBiomes()) biomes.get(ResourceKey.create(Registries.BIOME, b.mcc$modernId())).ifPresent(legacy::add);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Legacy biomes of world type {} could not be listed: {}", worldType, e.toString());
        }
        return Stream.concat(super.collectPossibleBiomes(), legacy.stream()).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> modern = super.getNoiseBiome(x, y, z, sampler);
        LegacyBiomeLayers l = layers;
        if (l == null || modern == null) return modern;
        try {
            Holder<Biome> legacy = l.replace(modern, x, z);
            return legacy != null ? legacy : modern;
        } catch (RuntimeException | LinkageError e) {
            // Never break chunk generation: the multi-noise biomes for the rest of the session.
            layers = null;
            LOG.error("Biome layers of world type {} failed; using the multi-noise biomes", worldType, e);
            return modern;
        }
    }
}
