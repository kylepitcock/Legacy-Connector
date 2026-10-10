package org.kylepitcock.mccompat.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import mccompat.v1_7_10.cpw.mods.fml.common.IWorldGenerator;
import mccompat.v1_7_10.net.minecraft.world.World;
import mccompat.v1_7_10.net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.level.WorldGenLevel;
import org.kylepitcock.mccompat.core.worldgen.LegacyWorldGenFeature;

/** Runs 1.7.10 IWorldGenerators with the exact per-chunk seeding FML used, so generation is reproducible. */
public final class WorldGenBridge {
    private record Entry(IWorldGenerator generator, int weight) {}

    private static final List<Entry> GENERATORS = new ArrayList<>();
    private static boolean hooked;

    private WorldGenBridge() {}

    public static synchronized void register(IWorldGenerator generator, int weight) {
        GENERATORS.add(new Entry(generator, weight));
        GENERATORS.sort(Comparator.comparingInt(Entry::weight));
        if (!hooked) {
            hooked = true;
            LegacyWorldGenFeature.addDecorator(WorldGenBridge::decorate);
        }
    }

    private static void decorate(WorldGenLevel level, int chunkX, int chunkZ, long worldSeed) {
        List<Entry> gens;
        synchronized (WorldGenBridge.class) {
            gens = List.copyOf(GENERATORS);
        }
        World world = World.mcc$wrap(level);
        Random fmlRandom = new Random(worldSeed);
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        long chunkSeed = (xSeed * chunkX + zSeed * chunkZ) ^ worldSeed;
        for (Entry e : gens) {
            fmlRandom.setSeed(chunkSeed);
            e.generator.generate(fmlRandom, chunkX, chunkZ, world, DUMMY, DUMMY);
        }
    }

    private static final IChunkProvider DUMMY = new IChunkProvider() {
        @Override
        public boolean chunkExists(int x, int z) {
            return true;
        }

        @Override
        public String makeString() {
            return "MCCompatChunkProvider";
        }

        @Override
        public int getLoadedChunkCount() {
            return 0;
        }
    };

    static List<IWorldGenerator> generators() {
        List<IWorldGenerator> out = new ArrayList<>();
        for (Entry e : GENERATORS) out.add(e.generator);
        return out;
    }
}
