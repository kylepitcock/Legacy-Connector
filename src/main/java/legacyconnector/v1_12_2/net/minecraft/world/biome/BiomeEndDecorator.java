package legacyconnector.v1_12_2.net.minecraft.world.biome;

import legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenSpikes;

/** 1.12.2 decorator of the End: the obsidian spikes around the central island (their layout is the modern game's for the seed). */
public class BiomeEndDecorator extends BiomeDecorator {
    protected WorldGenSpikes spikeGen = new WorldGenSpikes();

    /** The ten spikes of the world's End (the modern layout for the world seed, which 1.12 shuffled the same way). */
    public static WorldGenSpikes.EndSpike[] getSpikesForWorld(legacyconnector.v1_12_2.net.minecraft.world.World world) {
        net.minecraft.world.level.WorldGenLevel level = org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.level(world);
        if (level == null) {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            level = server == null ? null : server.overworld();
        }
        if (level == null) return new WorldGenSpikes.EndSpike[0];
        var modern = net.minecraft.world.level.levelgen.feature.SpikeFeature.getSpikesForLevel(level);
        WorldGenSpikes.EndSpike[] out = new WorldGenSpikes.EndSpike[modern.size()];
        for (int i = 0; i < out.length; i++) out[i] = WorldGenSpikes.EndSpike.mcc$of(modern.get(i));
        return out;
    }

    @Override
    protected void genDecorations(Biome biomeIn, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random random) {
        for (WorldGenSpikes.EndSpike spike : getSpikesForWorld(worldIn)) {
            if (spike.doesStartInChunk(this.chunkPos)) {
                this.spikeGen.setSpike(spike);
                this.spikeGen.generate(worldIn, random, new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(spike.getCenterX(), 45, spike.getCenterZ()));
            }
        }
    }
}
