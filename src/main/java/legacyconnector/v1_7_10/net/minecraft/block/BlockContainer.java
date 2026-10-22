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
        return null;
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
}
