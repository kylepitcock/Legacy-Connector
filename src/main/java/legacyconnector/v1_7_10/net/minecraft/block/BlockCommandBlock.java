package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.command.server.CommandBlockLogic;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityCommandBlock;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The command block (1.7.10 {@code BlockCommandBlock}): runs its command one tick after redstone power arrives (metadata bit 0 is
 * the powered state) and reports its success count to comparators. The wrapper of the vanilla block keeps the modern behaviour.
 */
public class BlockCommandBlock extends BlockContainer {
    public BlockCommandBlock(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockCommandBlock() {
        super(Material.iron);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCommandBlock();
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (mcc$vanilla != null) {
            super.onNeighborBlockChange(world, x, y, z, neighbor);
            return;
        }
        if (world.isRemote) return;
        boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        boolean wasPowered = (meta & 1) != 0;
        if (powered && !wasPowered) {
            world.setBlockMetadataWithNotify(x, y, z, meta | 1, 4);
            world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
        } else if (!powered && wasPowered) {
            world.setBlockMetadataWithNotify(x, y, z, meta & -2, 4);
        }
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (mcc$vanilla != null) {
            super.updateTick(world, x, y, z, random);
            return;
        }
        if (world.getTileEntity(x, y, z) instanceof TileEntityCommandBlock command) {
            command.func_145993_a().func_145755_a(world);
            world.func_147453_f(x, y, z, this);
        }
    }

    @Override
    public int tickRate(World world) {
        return mcc$vanilla != null ? super.tickRate(world) : 1;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (mcc$vanilla != null) return super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
        if (world.getTileEntity(x, y, z) instanceof TileEntityCommandBlock command) player.func_146095_a(command.func_145993_a());
        return true;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        if (mcc$vanilla != null) return super.getComparatorInputOverride(world, x, y, z, side);
        return world.getTileEntity(x, y, z) instanceof TileEntityCommandBlock command ? command.func_145993_a().getSuccessCount() : 0;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        if (mcc$vanilla != null) return;
        if (world.getTileEntity(x, y, z) instanceof TileEntityCommandBlock command) {
            CommandBlockLogic logic = command.func_145993_a();
            if (stack != null && stack.hasDisplayName()) logic.func_145754_b(stack.getDisplayName());
        }
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }
}
