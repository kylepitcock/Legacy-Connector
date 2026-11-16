package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A sign (1.7.10 {@code BlockSign}), for mods that register their own sign blocks: {@code new BlockSign(MySignTile.class, true)} is the
 * standing one (metadata 0-15 is the rotation), {@code false} the wall one (metadata 2-5 is the wall it hangs on). It has a tile entity of the
 * given class, drops the sign item and breaks when its support goes.
 */
public class BlockSign extends BlockContainer {
    private final Class<? extends TileEntity> field_149968_a;
    private final boolean field_149967_b;

    public BlockSign(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_149968_a = legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntitySign.class;
        this.field_149967_b = info.legacyName().contains("standing");
    }

    protected BlockSign(Class<? extends TileEntity> tileClass, boolean standing) {
        super(Material.wood);
        this.field_149968_a = tileClass;
        this.field_149967_b = standing;
        float half = 0.25F;
        setBlockBounds(0.5F - half, 0.0F, 0.5F - half, 0.5F + half, 1.0F, 0.5F + half);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 0);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        if (field_149967_b) return;
        int meta = world.getBlockMetadata(x, y, z);
        float low = 0.28125F;
        float high = 0.78125F;
        float inner = 0.0F;
        float outer = 1.0F;
        float thick = 0.125F;
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        if (meta == 2) setBlockBounds(inner, low, 1.0F - thick, outer, high, 1.0F);
        if (meta == 3) setBlockBounds(inner, low, 0.0F, outer, high, thick);
        if (meta == 4) setBlockBounds(1.0F - thick, low, inner, 1.0F, high, outer);
        if (meta == 5) setBlockBounds(0.0F, low, inner, thick, high, outer);
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        try {
            return field_149968_a.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Items.sign;
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return Items.sign;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        boolean lost;
        if (field_149967_b) {
            lost = !world.getBlock(x, y - 1, z).getMaterial().isSolid();
        } else {
            int meta = world.getBlockMetadata(x, y, z);
            lost = meta == 2 && !world.getBlock(x, y, z + 1).getMaterial().isSolid()
                    || meta == 3 && !world.getBlock(x, y, z - 1).getMaterial().isSolid()
                    || meta == 4 && !world.getBlock(x + 1, y, z).getMaterial().isSolid()
                    || meta == 5 && !world.getBlock(x - 1, y, z).getMaterial().isSolid();
        }
        if (lost) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
        super.onNeighborBlockChange(world, x, y, z, neighbor);
    }
}
