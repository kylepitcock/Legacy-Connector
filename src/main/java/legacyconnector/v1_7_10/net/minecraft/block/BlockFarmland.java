package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.IPlantable;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/**
 * Farmland, ported from 1.7.10: metadata is its moisture (7 near water, counting down when dry); dry farmland without a crop
 * turns back to dirt, as does farmland trampled by a fall or covered by a solid block.
 */
public class BlockFarmland extends Block {
    /** Wet top. */
    private IIcon field_149823_b;
    /** Dry top. */
    private IIcon field_149824_a;

    public BlockFarmland(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockFarmland() {
        super(Material.ground);
        setTickRandomly(true);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.9375F, 1.0F);
        setLightOpacity(255);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
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
    public IIcon getIcon(int side, int meta) {
        if (field_149824_a == null && mcc$vanilla != null) return super.getIcon(side, meta);
        if (side == 1) return meta > 0 ? field_149823_b : field_149824_a;
        return Blocks.dirt.getBlockTextureFromSide(side);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (!func_149821_m(world, x, y, z) && !world.canLightningStrikeAt(x, y + 1, z)) {
            int meta = world.getBlockMetadata(x, y, z);
            if (meta > 0) world.setBlockMetadataWithNotify(x, y, z, meta - 1, 2);
            else if (!func_149822_e(world, x, y, z)) world.setBlock(x, y, z, Blocks.dirt);
        } else {
            world.setBlockMetadataWithNotify(x, y, z, 7, 2);
        }
    }

    @Override
    public void onFallenUpon(World world, int x, int y, int z, Entity entity, float fallDistance) {
        if (world.isRemote || world.rand.nextFloat() >= fallDistance - 0.5F) return;
        if (!(entity instanceof EntityPlayer) && !world.getGameRules().getGameRuleBooleanValue("mobGriefing")) return;
        world.setBlock(x, y, z, Blocks.dirt);
    }

    /** Whether a plant this farmland sustains grows on it. */
    private boolean func_149822_e(World world, int x, int y, int z) {
        Block above = world.getBlock(x, y + 1, z);
        return above instanceof IPlantable plant && canSustainPlant(world, x, y, z, ForgeDirection.UP, plant);
    }

    /** Whether there is water within four blocks horizontally, at this level or one above. */
    private boolean func_149821_m(World world, int x, int y, int z) {
        for (int dx = x - 4; dx <= x + 4; ++dx) {
            for (int dy = y; dy <= y + 1; ++dy) {
                for (int dz = z - 4; dz <= z + 4; ++dz) {
                    if (world.getBlock(dx, dy, dz).getMaterial() == Material.water) return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        super.onNeighborBlockChange(world, x, y, z, neighbor);
        if (world.getBlock(x, y + 1, z).getMaterial().isSolid()) world.setBlock(x, y, z, Blocks.dirt);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Blocks.dirt.getItemDropped(0, random, fortune);
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(Blocks.dirt);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        field_149823_b = register.registerIcon(getTextureName() + "_wet");
        field_149824_a = register.registerIcon(getTextureName() + "_dry");
    }
}
