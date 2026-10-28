package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

import legacyconnector.v1_12_2.net.minecraft.init.Biomes;

/**
 * Turns climate codes into biomes: weighted picks from vanilla's lists (Forge's BiomeManager defaults) plus the biomes mods added
 * with {@code BiomeManager.addBiome}; special codes become mesa plateaus, jungles and mega taigas as in 1.12.
 */
public class GenLayerBiome extends GenLayer {
    private final java.util.List<Object[]>[] biomes;
    private final legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorSettings settings;

    @SuppressWarnings("unchecked")
    public GenLayerBiome(long seed, GenLayer parent, legacyconnector.v1_12_2.net.minecraft.world.WorldType worldType,
            legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorSettings settings) {
        super(seed);
        this.parent = parent;
        this.settings = settings;
        this.biomes = new java.util.List[4];
        String[] types = {"DESERT", "WARM", "COOL", "ICY"};
        Object[][][] defaults = {
                {{Biomes.DESERT, 30}, {Biomes.SAVANNA, 20}, {Biomes.PLAINS, 10}},
                {{Biomes.FOREST, 10}, {Biomes.ROOFED_FOREST, 10}, {Biomes.EXTREME_HILLS, 10}, {Biomes.PLAINS, 10}, {Biomes.BIRCH_FOREST, 10}, {Biomes.SWAMPLAND, 10}},
                {{Biomes.FOREST, 10}, {Biomes.EXTREME_HILLS, 10}, {Biomes.TAIGA, 10}, {Biomes.PLAINS, 10}},
                {{Biomes.ICE_PLAINS, 30}, {Biomes.COLD_TAIGA, 10}}};
        for (int t = 0; t < 4; t++) {
            java.util.List<Object[]> list = new java.util.ArrayList<>();
            for (Object[] d : defaults[t]) list.add(new Object[] {d[0], d[1]});
            for (Object entry : org.kylepitcock.legacyconnector.v1_12_2.bridge.RuntimeApis1122.mcc$biomeEntries(types[t])) {
                Object biome = field(entry, "biome");
                Object weight = field(entry, "itemWeight");
                if (biome instanceof legacyconnector.v1_12_2.net.minecraft.world.biome.Biome b && weight instanceof Integer wgt && wgt > 0) list.add(new Object[] {b, wgt});
            }
            if (t == 0 && worldType == legacyconnector.v1_12_2.net.minecraft.world.WorldType.DEFAULT_1_1) {
                list.clear();
                list.add(new Object[] {Biomes.DESERT, 10});
                list.add(new Object[] {Biomes.FOREST, 10});
                list.add(new Object[] {Biomes.EXTREME_HILLS, 10});
                list.add(new Object[] {Biomes.SWAMPLAND, 10});
                list.add(new Object[] {Biomes.PLAINS, 10});
                list.add(new Object[] {Biomes.TAIGA, 10});
            }
            biomes[t] = list;
        }
    }

    private static Object field(Object o, String name) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                var f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                // superclass
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int[] in = parent.getInts(x, z, w, h);
        int[] out = IntCache.getIntCache(w * h);
        int fixed = settings == null ? -1 : settings.fixedBiome;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                initChunkSeed(j + x, i + z);
                int k = in[j + i * w];
                int special = (k & 3840) >> 8;
                k &= -3841;
                int idx = j + i * w;
                if (fixed >= 0) out[idx] = fixed;
                else if (k == 0 || isBiomeOceanic(k)) out[idx] = k == 0 ? id(Biomes.OCEAN) : k;
                else if (k == id(Biomes.MUSHROOM_ISLAND)) out[idx] = k;
                else if (k == 1) out[idx] = special > 0 ? (nextInt(3) == 0 ? id(Biomes.MESA_CLEAR_ROCK) : id(Biomes.MESA_ROCK)) : pick(0);
                else if (k == 2) out[idx] = special > 0 ? id(Biomes.JUNGLE) : pick(1);
                else if (k == 3) out[idx] = special > 0 ? id(Biomes.REDWOOD_TAIGA) : pick(2);
                else if (k == 4) out[idx] = pick(3);
                else out[idx] = id(Biomes.MUSHROOM_ISLAND);
            }
        }
        return out;
    }

    private int pick(int type) {
        java.util.List<Object[]> list = biomes[type];
        int total = 0;
        for (Object[] e : list) total += (Integer) e[1];
        if (total <= 0) return id(Biomes.PLAINS);
        long n = nextLong(total);
        for (Object[] e : list) {
            n -= (Integer) e[1];
            if (n < 0) return id((legacyconnector.v1_12_2.net.minecraft.world.biome.Biome) e[0]);
        }
        return id(Biomes.PLAINS);
    }
}
