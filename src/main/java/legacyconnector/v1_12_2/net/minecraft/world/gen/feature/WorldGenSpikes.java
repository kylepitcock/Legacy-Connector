package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 obsidian pillar of the End with its crystal (the modern game's spike feature builds the one spike set here). */
public class WorldGenSpikes extends WorldGenerator {
    private boolean crystalInvulnerable;
    private EndSpike spike;
    private BlockPos beamTarget;

    public WorldGenSpikes() {
        super(false);
    }

    public void setSpike(EndSpike spikeIn) {
        this.spike = spikeIn;
    }

    public void setCrystalInvulnerable(boolean invulnerable) {
        this.crystalInvulnerable = invulnerable;
    }

    public void setBeamTarget(BlockPos pos) {
        this.beamTarget = pos;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        if (spike == null) throw new IllegalStateException("Decoration requires priming with a spike");
        var config = new net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration(crystalInvulnerable, java.util.List.of(spike.mcc$modern()),
                beamTarget == null ? null : beamTarget.mcc$modern());
        // the modern feature builds the spikes whose centre lies in the origin's chunk
        BlockPos origin = new BlockPos(spike.getCenterX(), position == null ? 45 : position.getY(), spike.getCenterZ());
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.placeFeature(worldIn,
                net.minecraft.world.level.levelgen.feature.Feature.END_SPIKE, config, rand, origin);
    }

    /** One End pillar: centre, radius, height and whether iron bars guard its crystal. */
    public static class EndSpike {
        private final int centerX;
        private final int centerZ;
        private final int radius;
        private final int height;
        private final boolean guarded;
        private final AxisAlignedBB topBoundingBox;
        private final net.minecraft.world.level.levelgen.feature.SpikeFeature.EndSpike mcc$modern;

        public EndSpike(int centerXIn, int centerZIn, int radiusIn, int heightIn, boolean guardedIn) {
            this(new net.minecraft.world.level.levelgen.feature.SpikeFeature.EndSpike(centerXIn, centerZIn, radiusIn, heightIn, guardedIn));
        }

        private EndSpike(net.minecraft.world.level.levelgen.feature.SpikeFeature.EndSpike modern) {
            this.mcc$modern = modern;
            this.centerX = modern.getCenterX();
            this.centerZ = modern.getCenterZ();
            this.radius = modern.getRadius();
            this.height = modern.getHeight();
            this.guarded = modern.isGuarded();
            this.topBoundingBox = new AxisAlignedBB(centerX - radius, 0.0D, centerZ - radius, centerX + radius, 256.0D, centerZ + radius);
        }

        public static EndSpike mcc$of(net.minecraft.world.level.levelgen.feature.SpikeFeature.EndSpike modern) {
            return modern == null ? null : new EndSpike(modern);
        }

        public net.minecraft.world.level.levelgen.feature.SpikeFeature.EndSpike mcc$modern() {
            return mcc$modern;
        }

        public boolean doesStartInChunk(BlockPos pos) {
            int i = this.centerX - this.radius;
            int j = this.centerZ - this.radius;
            return pos.getX() == (i & -16) && pos.getZ() == (j & -16);
        }

        public int getCenterX() {
            return centerX;
        }

        public int getCenterZ() {
            return centerZ;
        }

        public int getRadius() {
            return radius;
        }

        public int getHeight() {
            return height;
        }

        public boolean isGuarded() {
            return guarded;
        }

        public AxisAlignedBB getTopBoundingBox() {
            return topBoundingBox;
        }
    }
}
