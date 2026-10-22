package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/** Farmland crops: eight growth stages in the metadata, grown by random ticks, drops by stage. */
public class BlockCrops extends BlockBush implements IGrowable {
    private IIcon[] icons;

    public BlockCrops(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockCrops() {
        setTickRandomly(true);
        float f = 0.5F;
        setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, 0.25F, 0.5F + f);
        setCreativeTab(null);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        disableStats();
    }

    @Override
    protected boolean canPlaceBlockOn(Block block) {
        return block == Blocks.farmland;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        super.updateTick(world, x, y, z, random);
        if (world.getBlockLightValue(x, y + 1, z) >= 9) {
            int meta = world.getBlockMetadata(x, y, z);
            if (meta < 7) {
                float growth = func_149864_n(world, x, y, z);
                if (random.nextInt((int) (25.0F / growth) + 1) == 0) {
                    ++meta;
                    world.setBlockMetadataWithNotify(x, y, z, meta, 2);
                }
            }
        }
    }

    public void fertilize(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z) + MathHelper.getRandomIntegerInRange(world.rand, 2, 5);
        if (meta > 7) meta = 7;
        world.setBlockMetadataWithNotify(x, y, z, meta, 2);
    }

    private float func_149864_n(World world, int x, int y, int z) {
        float f = 1.0F;
        Block north = world.getBlock(x, y, z - 1), south = world.getBlock(x, y, z + 1);
        Block west = world.getBlock(x - 1, y, z), east = world.getBlock(x + 1, y, z);
        Block nw = world.getBlock(x - 1, y, z - 1), ne = world.getBlock(x + 1, y, z - 1);
        Block se = world.getBlock(x + 1, y, z + 1), sw = world.getBlock(x - 1, y, z + 1);
        boolean xRow = west == this || east == this;
        boolean zRow = north == this || south == this;
        boolean diagonal = nw == this || ne == this || se == this || sw == this;
        for (int i = x - 1; i <= x + 1; ++i) {
            for (int j = z - 1; j <= z + 1; ++j) {
                float f1 = 0.0F;
                Block soil = world.getBlock(i, y - 1, j);
                if (soil.canSustainPlant(world, i, y - 1, j, ForgeDirection.UP, this)) {
                    f1 = 1.0F;
                    if (world.getBlockMetadata(i, y - 1, j) > 0 && soil == Blocks.farmland) f1 = 3.0F;
                }
                if (i != x || j != z) f1 /= 4.0F;
                f += f1;
            }
        }
        if (diagonal || xRow && zRow) f /= 2.0F;
        return f;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (meta < 0 || meta > 7) meta = 7;
        return icons == null ? null : icons[meta];
    }

    @Override
    public int getRenderType() {
        return 6;
    }

    protected Item getSeed() {
        return Items.wheat_seeds;
    }

    protected Item getCrop() {
        return Items.wheat;
    }

    @Override
    public void dropBlockAsItemWithChance(World world, int x, int y, int z, int meta, float chance, int fortune) {
        super.dropBlockAsItemWithChance(world, x, y, z, meta, chance, 0);
        if (!world.isRemote && meta >= 7) {
            for (int i = 0; i < 3 + fortune; ++i) {
                if (world.rand.nextInt(15) <= meta) {
                    dropBlockAsItem(world, x, y, z, new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(getSeed(), 1, 0));
                }
            }
        }
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return meta == 7 ? getCrop() : getSeed();
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return getSeed();
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[8];
        for (int i = 0; i < icons.length; ++i) icons[i] = register.registerIcon(getTextureName() + "_stage_" + i);
    }

    @Override
    public boolean canFertilize(World world, int x, int y, int z, boolean isClient) {
        return world.getBlockMetadata(x, y, z) != 7;
    }

    @Override
    public boolean shouldFertilize(World world, Random random, int x, int y, int z) {
        return true;
    }

    @Override
    public void fertilize(World world, Random random, int x, int y, int z) {
        fertilize(world, x, y, z);
    }
}
