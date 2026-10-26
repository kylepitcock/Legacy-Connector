package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.MapColor;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityEndPortal;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The end portal, ported from 1.7.10: a thin glowing layer that sends entities touching it to the End. Placed outside the
 * overworld it removes itself, unless {@link #field_149948_a} is set (the dragon's exit portal sets it while building).
 */
public class BlockEndPortal extends BlockContainer {
    /** Set while a portal is being built on purpose: {@link #onBlockAdded} keeps it in any dimension. */
    public static boolean field_149948_a;

    public BlockEndPortal(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockEndPortal(Material material) {
        super(material);
        setLightLevel(1.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityEndPortal();
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F);
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return side == 0 && super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (!world.isRemote && Entity.mcc$get$ridingEntity(entity) == null && Entity.mcc$get$riddenByEntity(entity) == null) entity.travelToDimension(1);
    }

    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        double px = x + random.nextFloat();
        double py = y + 0.8F;
        double pz = z + random.nextFloat();
        world.spawnParticle("smoke", px, py, pz, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (!field_149948_a && World.mcc$get$provider(world).dimensionId != 0) world.setBlockToAir(x, y, z);
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon("portal");
    }

    @Override
    public MapColor getMapColor(int meta) {
        return MapColor.obsidianColor;
    }
}
