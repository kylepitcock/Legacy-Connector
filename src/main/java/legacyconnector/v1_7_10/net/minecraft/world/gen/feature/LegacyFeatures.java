package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.EnumPlantType;
import legacyconnector.v1_7_10.net.minecraftforge.common.IPlantable;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/** Helpers the ported 1.7.10 feature generators share (not a legacy class). */
final class LegacyFeatures {
    private LegacyFeatures() {}

    /** A sapling as a plantable: the vanilla sapling when it is one, else a plains plant (grass, dirt, farmland). */
    private static final IPlantable SAPLING = new IPlantable() {
        @Override
        public EnumPlantType getPlantType(IBlockAccess world, int x, int y, int z) {
            return EnumPlantType.Plains;
        }

        @Override
        public Block getPlant(IBlockAccess world, int x, int y, int z) {
            return Blocks.sapling;
        }

        @Override
        public int getPlantMetadata(IBlockAccess world, int x, int y, int z) {
            return 0;
        }
    };

    static IPlantable sapling() {
        return Blocks.sapling instanceof IPlantable p ? p : SAPLING;
    }

    /** Whether a tree may grow from the block (Forge: {@code canSustainPlant} for a sapling). */
    static boolean sustainsTree(World world, int x, int y, int z) {
        Block soil = world.getBlock(x, y, z);
        return soil != null && soil.canSustainPlant(world, x, y, z, ForgeDirection.UP, sapling());
    }

    /** A tree grew on the block: its {@code onPlantGrow} (vanilla grass and farmland turn to dirt, as Forge's default did). */
    static void plantGrew(World world, int x, int y, int z, int sourceX, int sourceY, int sourceZ) {
        Block soil = world.getBlock(x, y, z);
        if (soil == null) return;
        if (soil == Blocks.grass || soil == Blocks.farmland) {
            world.setBlock(x, y, z, Blocks.dirt, 0, 2);
            return;
        }
        soil.onPlantGrow(world, x, y, z, sourceX, sourceY, sourceZ);
    }

    static boolean airOrLeaves(World world, int x, int y, int z) {
        Block b = world.getBlock(x, y, z);
        return b == null || b.isAir(world, x, y, z) || b.isLeaves(world, x, y, z);
    }

    /**
     * Places a liquid source (a spring) and lets it start flowing: a vanilla liquid gets its fluid tick, a mod liquid its own update,
     * run at once as 1.7.10 did with {@code scheduledUpdatesAreImmediate}.
     */
    static void spring(World world, Block liquid, int x, int y, int z, java.util.Random random) {
        if (!world.setBlock(x, y, z, liquid, 0, 2)) return;
        if (liquid.mcc$vanilla != null) {
            var level = world.mcc$level();
            var pos = new net.minecraft.core.BlockPos(x, y, z);
            var fluid = level.getFluidState(pos);
            if (!fluid.isEmpty()) level.scheduleTick(pos, fluid.getType(), 0);
            return;
        }
        world.scheduledUpdatesAreImmediate = true;
        try {
            liquid.updateTick(world, x, y, z, random);
        } catch (RuntimeException e) {
            // a liquid that cannot update during generation just sits there
        } finally {
            world.scheduledUpdatesAreImmediate = false;
        }
    }
}
