package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraftforge.common.BiomeManager;

/** Turns climate codes into biomes, picking weighted entries from Forge's BiomeManager lists. */
public class GenLayerBiome extends GenLayer {
    @SuppressWarnings("unchecked")
    private final List<BiomeManager.BiomeEntry>[] biomes = new ArrayList[BiomeManager.BiomeType.values().length];

    public GenLayerBiome(long seed, GenLayer parent, WorldType worldType) {
        super(seed);
        this.parent = parent;
        for (BiomeManager.BiomeType type : BiomeManager.BiomeType.values()) {
            biomes[type.ordinal()] = new ArrayList<>(BiomeManager.getBiomes(type));
        }
        if (worldType == WorldType.DEFAULT_1_1) {
            biomes[BiomeManager.BiomeType.DESERT.ordinal()].clear();
            biomes[BiomeManager.BiomeType.DESERT.ordinal()].add(new BiomeManager.BiomeEntry(BiomeGenBase.desert, 10));
        }
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int[] in = parent.getInts(x, z, w, h);
        int[] out = IntCache.getIntCache(w * h);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                initChunkSeed(j + x, i + z);
                int k = in[j + i * w];
                int special = (k & 3840) >> 8;
                k &= -3841;
                if (isBiomeOceanic(k)) out[j + i * w] = k;
                else if (k == BiomeGenBase.mushroomIsland.biomeID) out[j + i * w] = k;
                else if (k == 1) out[j + i * w] = special > 0 ? (nextInt(3) == 0 ? BiomeGenBase.mesaPlateau.biomeID : BiomeGenBase.mesaPlateau_F.biomeID)
                        : getWeightedBiomeEntry(BiomeManager.BiomeType.DESERT).biome.biomeID;
                else if (k == 2) out[j + i * w] = special > 0 ? BiomeGenBase.jungle.biomeID : getWeightedBiomeEntry(BiomeManager.BiomeType.WARM).biome.biomeID;
                else if (k == 3) out[j + i * w] = special > 0 ? BiomeGenBase.megaTaiga.biomeID : getWeightedBiomeEntry(BiomeManager.BiomeType.COOL).biome.biomeID;
                else if (k == 4) out[j + i * w] = getWeightedBiomeEntry(BiomeManager.BiomeType.ICY).biome.biomeID;
                else out[j + i * w] = BiomeGenBase.mushroomIsland.biomeID;
            }
        }
        return out;
    }

    protected BiomeManager.BiomeEntry getWeightedBiomeEntry(BiomeManager.BiomeType type) {
        // Mods add entries for biomes they disabled in their config (biome == null): those never generate.
        List<BiomeManager.BiomeEntry> list = new java.util.ArrayList<>(biomes[type.ordinal()].size());
        for (BiomeManager.BiomeEntry e : biomes[type.ordinal()]) if (e != null && e.biome != null && e.itemWeight > 0) list.add(e);
        if (list.isEmpty()) return new BiomeManager.BiomeEntry(BiomeGenBase.plains, 1);
        int total = WeightedRandom.getTotalWeight(list);
        int pick = nextInt(Math.max(1, total));
        for (BiomeManager.BiomeEntry e : list) {
            pick -= e.itemWeight;
            if (pick < 0) return e;
        }
        return list.get(0);
    }
}
