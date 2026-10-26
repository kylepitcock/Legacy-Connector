package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Still water or lava, ported from 1.7.10: a neighbour change turns it back into its flowing block (the block with the previous
 * id, as in 1.7.10); still lava sets fire to flammable blocks around it on random ticks.
 */
public class BlockStaticLiquid extends BlockLiquid {
    public BlockStaticLiquid(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockStaticLiquid(Material material) {
        super(material);
        setTickRandomly(material == Material.lava);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        super.onNeighborBlockChange(world, x, y, z, neighbor);
        if (world.getBlock(x, y, z) == this) setNotStationary(world, x, y, z);
    }

    /** Turns the still block into the flowing one so it can spread again. */
    private void setNotStationary(World world, int x, int y, int z) {
        Block flowing = Block.getBlockById(Block.getIdFromBlock(this) - 1);
        if (!(flowing instanceof BlockDynamicLiquid) || flowing.getMaterial() != blockMaterial) return;
        world.setBlock(x, y, z, flowing, world.getBlockMetadata(x, y, z), 2);
        world.scheduleBlockUpdate(x, y, z, flowing, tickRate(world));
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (blockMaterial != Material.lava) return;
        int tries = random.nextInt(3);
        for (int i = 0; i < tries; ++i) {
            x += random.nextInt(3) - 1;
            ++y;
            z += random.nextInt(3) - 1;
            Block block = world.getBlock(x, y, z);
            if (block.getMaterial() == Material.air) {
                if (isFlammable(world, x - 1, y, z) || isFlammable(world, x + 1, y, z) || isFlammable(world, x, y, z - 1) || isFlammable(world, x, y, z + 1)
                        || isFlammable(world, x, y - 1, z) || isFlammable(world, x, y + 1, z)) {
                    world.setBlock(x, y, z, Blocks.fire);
                    return;
                }
            } else if (block.getMaterial().blocksMovement()) {
                return;
            }
        }
        if (tries == 0) {
            int cx = x;
            int cz = z;
            for (int i = 0; i < 3; ++i) {
                x = cx + random.nextInt(3) - 1;
                z = cz + random.nextInt(3) - 1;
                if (world.isAirBlock(x, y + 1, z) && isFlammable(world, x, y, z)) world.setBlock(x, y + 1, z, Blocks.fire);
            }
        }
    }

    private boolean isFlammable(World world, int x, int y, int z) {
        return world.getBlock(x, y, z).getMaterial().getCanBurn();
    }
}
