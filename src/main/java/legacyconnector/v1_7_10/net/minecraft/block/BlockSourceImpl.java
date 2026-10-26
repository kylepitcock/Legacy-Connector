package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.dispenser.IBlockSource;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The block a dispenser behaviour runs for (mods build one to run dispenser behaviours from their own blocks). */
public class BlockSourceImpl implements IBlockSource {
    private final World worldObj;
    private final int xPos;
    private final int yPos;
    private final int zPos;

    public BlockSourceImpl(World world, int x, int y, int z) {
        this.worldObj = world;
        this.xPos = x;
        this.yPos = y;
        this.zPos = z;
    }

    @Override
    public World getWorld() {
        return worldObj;
    }

    @Override
    public double getX() {
        return xPos + 0.5D;
    }

    @Override
    public double getY() {
        return yPos + 0.5D;
    }

    @Override
    public double getZ() {
        return zPos + 0.5D;
    }

    @Override
    public int getXInt() {
        return xPos;
    }

    @Override
    public int getYInt() {
        return yPos;
    }

    @Override
    public int getZInt() {
        return zPos;
    }

    @Override
    public int getBlockMetadata() {
        return worldObj.getBlockMetadata(xPos, yPos, zPos);
    }

    @Override
    public TileEntity getBlockTileEntity() {
        return worldObj.getTileEntity(xPos, yPos, zPos);
    }
}
