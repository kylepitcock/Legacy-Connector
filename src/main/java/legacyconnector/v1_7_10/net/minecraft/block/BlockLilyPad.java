package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityBoat;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The lily pad: a flat (1/64 high) plant with a solid top that floats on still water; boats pass through it. */
public class BlockLilyPad extends BlockBush {
    public BlockLilyPad(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockLilyPad() {
        float half = 0.5F;
        float height = 0.015625F;
        setBlockBounds(0.5F - half, 0.0F, 0.5F - half, 0.5F + half, height, 0.5F + half);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabDecorations);
    }

    @Override
    public int getRenderType() {
        return 23;
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
        if (!(entity instanceof EntityBoat)) super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
    }

    @Override
    public int getBlockColor() {
        return 2129968;
    }

    @Override
    public int getRenderColor(int meta) {
        return 2129968;
    }

    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return 2129968;
    }

    @Override
    protected boolean canPlaceBlockOn(Block block) {
        return block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.water;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return y >= 0 && y < 256 && world.getBlock(x, y - 1, z).getMaterial() == Material.water && world.getBlockMetadata(x, y - 1, z) == 0;
    }
}
