package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.world.World;

public abstract class BlockContainer extends Block implements ITileEntityProvider {
    public BlockContainer(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.isBlockContainer = true;
    }

    protected BlockContainer(Material material) {
        super(material);
        this.isBlockContainer = true;
    }

    /** Concrete in the shim only so generated vanilla shells compile; mod subclasses override it as before. */
    @Override
    public legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity createNewTileEntity(World world, int meta) {
        // a 1.6.4 subclass overrides createNewTileEntity(World), which has no meta argument
        legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity legacy = mcc$createNewTileEntity1(this, world);
        // a 1.2.5 subclass overrides getBlockEntity(metadata) or the no-argument factory
        return legacy != null ? legacy : getBlockEntity(meta);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        super.breakBlock(world, x, y, z, block, meta);
        world.removeTileEntity(x, y, z);
    }

    @Override
    public boolean onBlockEventReceived(World world, int x, int y, int z, int id, int param) {
        super.onBlockEventReceived(world, x, y, z, id, param);
        var te = world.getTileEntity(x, y, z);
        return te != null && te.receiveClientEvent(id, param);
    }

    // Members125 batch 1

    /** 1.2.5's no-argument block entity factory (Feather {@code createBlockEntity}); mod blocks override it. */
    public legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity mcc125$createBlockEntity() {
        return null;
    }

    /** 1.2.5 {@code BlockContainer.getBlockEntity(metadata)}: the entity for a metadata value; defaults to the no-argument factory. */
    public legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity getBlockEntity(int metadata) {
        return mcc125$createBlockEntity();
    }
}
