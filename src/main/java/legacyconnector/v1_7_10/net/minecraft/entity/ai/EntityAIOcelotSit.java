package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.BlockBed;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityChest;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Vanilla ocelots sitting on chests/beds; kept so the ocelot task list has its vanilla shape. The goal itself never starts (the modern ocelot has its own
 * tasks); {@link #func_151486_a} is the vanilla test of a place worth sitting on, for mods that reuse it.
 */
public class EntityAIOcelotSit extends EntityAIBase {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIOcelotSit(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot o, float speed) {
        this(o, (double) speed);
    }

    public EntityAIOcelotSit(EntityOcelot ocelot, double speed) {
        setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }

    /** Whether an ocelot may sit at the position: free air above and an unused chest, a lit furnace, or the foot of a bed below. */
    protected boolean func_151486_a(World world, int x, int y, int z) {
        if (!world.isAirBlock(x, y + 1, z)) return false;
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        if (block == Blocks.chest) {
            TileEntity tile = world.getTileEntity(x, y, z);
            return tile instanceof TileEntityChest chest && chest.numPlayersUsing < 1;
        }
        if (block == Blocks.lit_furnace) return true;
        return block == Blocks.bed && !BlockBed.isBlockHeadOfBed(meta);
    }
}
