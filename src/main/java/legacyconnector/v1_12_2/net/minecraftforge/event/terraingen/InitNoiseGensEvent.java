package legacyconnector.v1_12_2.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.HasResult;

/** Forge 1.12.2: a legacy chunk generator made its noise generators; handlers may replace them (terrain bus). */
public class InitNoiseGensEvent<T extends InitNoiseGensEvent.Context> extends legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent {
    private final Random rand;
    private final T original;
    private T newValues;

    @SuppressWarnings("unchecked")
    public InitNoiseGensEvent(World world, Random rand, T original) {
        super(world);
        this.rand = rand;
        this.original = original;
        this.newValues = (T) original.clone();
    }

    public Random getRandom() {
        return rand;
    }

    public T getOriginal() {
        return original;
    }

    public T getNewValues() {
        return newValues;
    }

    public static class Context {
        private legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves lperlin1, lperlin2, perlin, scale, depth;

        public Context(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves lperlin1, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves lperlin2, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves perlin, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves scale, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves depth) {
            this.lperlin1 = lperlin1;
            this.lperlin2 = lperlin2;
            this.perlin = perlin;
            this.scale = scale;
            this.depth = depth;
        }

        public legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves getLPerlin1() { return lperlin1; }
        public legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves getLPerlin2() { return lperlin2; }
        public legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves getPerlin() { return perlin; }
        public legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves getScale() { return scale; }
        public legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves getDepth() { return depth; }
        public void getLPerlin1(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves v) { lperlin1 = v; }
        public void getLPerlin2(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves v) { lperlin2 = v; }
        public void getPerlin(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves v) { perlin = v; }
        public void getScale(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves v) { scale = v; }
        public void getDepth(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves v) { depth = v; }

        @Override
        public Context clone() {
            return new Context(lperlin1, lperlin2, perlin, scale, depth);
        }
    }

    public static class ContextOverworld extends Context {
        private legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin height;
        private legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves forest;

        public ContextOverworld(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves lperlin1, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves lperlin2, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves perlin, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin height, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves scale, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves depth, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves forest) {
            super(lperlin1, lperlin2, perlin, scale, depth);
            this.height = height;
            this.forest = forest;
        }

        @Override
        public ContextOverworld clone() {
            return new ContextOverworld(getLPerlin1(), getLPerlin2(), getPerlin(), height, getScale(), getDepth(), forest);
        }

        public legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin getHeight() { return height; }
        public legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves getForest() { return forest; }
        public void getHeight(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin v) { height = v; }
        public void getForest(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves v) { forest = v; }
    }

    public static class ContextEnd extends Context {
        private legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorSimplex island;

        public ContextEnd(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves lperlin1, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves lperlin2, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves perlin, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves scale, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves depth, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorSimplex island) {
            super(lperlin1, lperlin2, perlin, scale, depth);
            this.island = island;
        }

        @Override
        public ContextEnd clone() {
            return new ContextEnd(getLPerlin1(), getLPerlin2(), getPerlin(), getScale(), getDepth(), island);
        }

        public legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorSimplex getIsland() { return island; }
        public void getIsland(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorSimplex v) { island = v; }
    }

    public static class ContextHell extends Context {
        private legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves perlin2, perlin3;

        public ContextHell(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves lperlin1, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves lperlin2, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves perlin, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves perlin2, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves perlin3, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves scale, legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves depth) {
            super(lperlin1, lperlin2, perlin, scale, depth);
            this.perlin2 = perlin2;
            this.perlin3 = perlin3;
        }

        @Override
        public ContextHell clone() {
            return new ContextHell(getLPerlin1(), getLPerlin2(), getPerlin(), perlin2, perlin3, getScale(), getDepth());
        }

        public legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves getPerlin2() { return perlin2; }
        public legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves getPerlin3() { return perlin3; }
        public void getPerlin2(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves v) { perlin2 = v; }
        public void getPerlin3(legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorOctaves v) { perlin3 = v; }
    }
}
