package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A block that has a tile entity. */
public abstract class BlockContainer extends Block implements ITileEntityProvider {
    protected BlockContainer(Material material, legacyconnector.v1_12_2.net.minecraft.block.material.MapColor color) {
        super(material, color);
        this.hasTileEntity = true;
    }

    protected BlockContainer(Material material) {
        super(material);
        this.hasTileEntity = true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    /** Block events go to the tile entity (chest lids, note blocks). */
    @Override
    public boolean eventReceived(IBlockState state, World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, int id, int param) {
        TileEntity te = world.getTileEntity(pos);
        return te != null && te.receiveClientEvent(id, param);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return createNewTileEntity(world, getMetaFromState(state));
    }
}
