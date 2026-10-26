package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.ArrayList;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/**
 * A pumpkin or melon stem, ported from 1.7.10 with Forge's drops: metadata 0-7 is its age; full grown it puts its fruit on a free
 * neighbouring block (faster with farmland around it). Bone meal adds 2-5 stages ({@link #fertilizeStem}).
 */
public class BlockStem extends BlockBush implements IGrowable {
    /** The fruit the stem grows (pumpkin or melon). */
    public Block field_149877_a;
    public IIcon field_149876_b;

    public BlockStem(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    /** The fruit; vanilla stems name theirs lazily (the wrappers are made while {@code Blocks} is still initialising). */
    private Block mcc$fruit() {
        if (field_149877_a == null && mcc$vanilla != null) {
            field_149877_a = mcc$vanilla.legacyName().endsWith("melon_stem") ? Blocks.melon_block : Blocks.pumpkin;
        }
        return field_149877_a;
    }

    protected BlockStem() {
        super();
    }

    protected BlockStem(Block fruit) {
        this();
        this.field_149877_a = fruit;
        setTickRandomly(true);
        float f = 0.125F;
        setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, 0.25F, 0.5F + f);
        setCreativeTab(null);
    }

    @Override
    protected boolean canPlaceBlockOn(Block block) {
        return block == Blocks.farmland;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        super.updateTick(world, x, y, z, random);
        if (world.getBlock(x, y, z) != this || world.getBlockLightValue(x, y + 1, z) < 9) return;
        float rate = func_149875_n(world, x, y, z);
        if (random.nextInt((int) (25.0F / rate) + 1) != 0) return;
        int meta = world.getBlockMetadata(x, y, z);
        if (meta < 7) {
            world.setBlockMetadataWithNotify(x, y, z, meta + 1, 2);
            return;
        }
        Block fruit = mcc$fruit();
        if (fruit == null) return;
        if (world.getBlock(x - 1, y, z) == fruit || world.getBlock(x + 1, y, z) == fruit || world.getBlock(x, y, z - 1) == fruit
                || world.getBlock(x, y, z + 1) == fruit) {
            return;
        }
        int side = random.nextInt(4);
        int fx = x + (side == 0 ? -1 : side == 1 ? 1 : 0);
        int fz = z + (side == 2 ? -1 : side == 3 ? 1 : 0);
        Block below = world.getBlock(fx, y - 1, fz);
        if (world.isAirBlock(fx, y, fz)
                && (below.canSustainPlant(world, fx, y - 1, fz, ForgeDirection.UP, this) || below == Blocks.dirt || below == Blocks.grass)) {
            world.setBlock(fx, y, fz, fruit);
        }
    }

    /** {@code func_149874_m}: bone meal: 2-5 stages of growth (up to full grown). */
    public void fertilizeStem(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z) + MathHelper.getRandomIntegerInRange(world.rand, 2, 5);
        world.setBlockMetadataWithNotify(x, y, z, Math.min(meta, 7), 2);
    }

    /** The growth rate from the farmland under and around the stem (halved when stems crowd it). */
    private float func_149875_n(World world, int x, int y, int z) {
        float rate = 1.0F;
        boolean alongX = world.getBlock(x - 1, y, z) == this || world.getBlock(x + 1, y, z) == this;
        boolean alongZ = world.getBlock(x, y, z - 1) == this || world.getBlock(x, y, z + 1) == this;
        boolean diagonal = world.getBlock(x - 1, y, z - 1) == this || world.getBlock(x + 1, y, z - 1) == this
                || world.getBlock(x + 1, y, z + 1) == this || world.getBlock(x - 1, y, z + 1) == this;
        for (int dx = x - 1; dx <= x + 1; ++dx) {
            for (int dz = z - 1; dz <= z + 1; ++dz) {
                Block soil = world.getBlock(dx, y - 1, dz);
                float f = 0.0F;
                if (soil.canSustainPlant(world, dx, y - 1, dz, ForgeDirection.UP, this)) {
                    f = soil.isFertile(world, dx, y - 1, dz) ? 3.0F : 1.0F;
                }
                if (dx != x || dz != z) f /= 4.0F;
                rate += f;
            }
        }
        if (diagonal || alongX && alongZ) rate /= 2.0F;
        return rate;
    }

    @Override
    public int getRenderColor(int meta) {
        return meta * 32 << 16 | 255 - meta * 8 << 8 | meta * 4;
    }

    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return getRenderColor(world.getBlockMetadata(x, y, z));
    }

    @Override
    public void setBlockBoundsForItemRender() {
        float f = 0.125F;
        setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, 0.25F, 0.5F + f);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        float f = 0.125F;
        float height = (world.getBlockMetadata(x, y, z) * 2 + 2) / 16.0F;
        setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, height, 0.5F + f);
    }

    @Override
    public int getRenderType() {
        return 19;
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName() + "_disconnected");
        field_149876_b = register.registerIcon(getTextureName() + "_connected");
    }

    /** The bent stem's icon (pointing at its fruit). */
    public IIcon getStemIcon() {
        if (field_149876_b != null) return field_149876_b;
        return mcc$vanillaIcon(mcc$vanilla != null && mcc$vanilla.legacyName().endsWith("melon_stem") ? "melon_stem_connected" : "pumpkin_stem_connected");
    }

    /** The direction of the grown fruit next to a full-grown stem (0 -x, 1 +x, 2 -z, 3 +z), -1 if none. */
    public int getState(IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        if (meta < 7) return -1;
        Block fruit = mcc$fruit();
        if (world.getBlock(x - 1, y, z) == fruit) return 0;
        if (world.getBlock(x + 1, y, z) == fruit) return 1;
        if (world.getBlock(x, y, z - 1) == fruit) return 2;
        return world.getBlock(x, y, z + 1) == fruit ? 3 : -1;
    }

    /** The seeds of the stem's fruit (none for a fruit other than pumpkin or melon). */
    private Item mcc$seeds() {
        Block fruit = mcc$fruit();
        if (fruit == Blocks.pumpkin) return Items.pumpkin_seeds;
        if (fruit == Blocks.melon_block) return Items.melon_seeds;
        return null;
    }

    /** Forge: up to three seeds, more likely the older the stem. */
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        Item seeds = mcc$seeds();
        if (seeds == null) return drops;
        for (int i = 0; i < 3; ++i) if (world.rand.nextInt(15) <= metadata) drops.add(new ItemStack(seeds));
        return drops;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return null;
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return mcc$seeds();
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
        fertilizeStem(world, x, y, z);
    }
}
