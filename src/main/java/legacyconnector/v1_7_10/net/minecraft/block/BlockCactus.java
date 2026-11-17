package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Cactus (1.7.10 {@code BlockCactus}): grows up to three high on sand, hurts what touches it, breaks when a neighbour is solid.
 * The vanilla wrapper keeps the modern cactus' own behaviour; this logic runs for a mod's cactus subclass.
 */
public class BlockCactus extends Block {
    private IIcon field_150041_a;
    private IIcon field_150040_b;

    public BlockCactus(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockCactus() {
        super(Material.cactus);
        setTickRandomly(true);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabDecorations);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (mcc$vanilla != null) {
            super.updateTick(world, x, y, z, random);
            return;
        }
        if (!world.isAirBlock(x, y + 1, z)) return;
        int height = 1;
        while (world.getBlock(x, y - height, z) == this) ++height;
        if (height >= 3) return;
        int age = world.getBlockMetadata(x, y, z);
        if (age == 15) {
            world.setBlock(x, y + 1, z, this);
            world.setBlockMetadataWithNotify(x, y, z, 0, 4);
            onNeighborBlockChange(world, x, y + 1, z, this);
        } else {
            world.setBlockMetadataWithNotify(x, y, z, age + 1, 4);
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        if (mcc$vanilla != null) return super.getCollisionBoundingBoxFromPool(world, x, y, z);
        float inset = 0.0625F;
        return AxisAlignedBB.getBoundingBox(x + inset, y, z + inset, x + 1 - inset, y + 1 - inset, z + 1 - inset);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        if (mcc$vanilla != null) return super.getSelectedBoundingBoxFromPool(world, x, y, z);
        float inset = 0.0625F;
        return AxisAlignedBB.getBoundingBox(x + inset, y, z + inset, x + 1 - inset, y + 1, z + 1 - inset);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (field_150041_a == null) return super.getIcon(side, meta);
        return side == 1 ? field_150041_a : side == 0 ? field_150040_b : blockIcon;
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName() + "_side");
        field_150041_a = register.registerIcon(getTextureName() + "_top");
        field_150040_b = register.registerIcon(getTextureName() + "_bottom");
    }

    @Override
    public boolean isOpaqueCube() {
        return mcc$vanilla != null ? super.isOpaqueCube() : false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return mcc$vanilla != null ? super.renderAsNormalBlock() : false;
    }

    @Override
    public int getRenderType() {
        return mcc$vanilla != null ? super.getRenderType() : 13;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        if (mcc$vanilla != null) return super.canPlaceBlockAt(world, x, y, z);
        return super.canPlaceBlockAt(world, x, y, z) && canBlockStay(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (mcc$vanilla != null) {
            super.onNeighborBlockChange(world, x, y, z, neighbor);
            return;
        }
        if (!canBlockStay(world, x, y, z)) world.breakBlock(x, y, z, true);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        if (mcc$vanilla != null) return super.canBlockStay(world, x, y, z);
        if (world.getBlock(x - 1, y, z).getMaterial().isSolid()) return false;
        if (world.getBlock(x + 1, y, z).getMaterial().isSolid()) return false;
        if (world.getBlock(x, y, z - 1).getMaterial().isSolid()) return false;
        if (world.getBlock(x, y, z + 1).getMaterial().isSolid()) return false;
        Block below = world.getBlock(x, y - 1, z);
        return below == Blocks.cactus || below == Blocks.sand;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (mcc$vanilla != null) {
            super.onEntityCollidedWithBlock(world, x, y, z, entity);
            return;
        }
        entity.attackEntityFrom(DamageSource.cactus, 1.0F);
    }
}
