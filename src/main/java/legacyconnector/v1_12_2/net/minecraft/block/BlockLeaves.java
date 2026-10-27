package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 leaves: decay when no log ({@code canSustainLeaves}) is within four blocks (checked when a nearby log breaks), drop saplings
 * (1 in 20, better with fortune) and, through {@link #dropApple}, apples for oak.
 */
public abstract class BlockLeaves extends Block {
    public static final PropertyBool DECAYABLE = PropertyBool.create("decayable");
    public static final PropertyBool CHECK_DECAY = PropertyBool.create("check_decay");
    protected boolean leavesFancy = true;
    int[] surroundings;

    public BlockLeaves() {
        super(Material.LEAVES);
        setTickRandomly(true);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.DECORATIONS);
        setHardness(0.2F);
        setLightOpacity(1);
        setSoundType(SoundType.PLANT);
    }

    /** Tells the leaves within two blocks to check whether they still have a log. */
    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (worldIn.isAreaLoaded(pos.add(-2, -2, -2), pos.add(2, 2, 2))) {
            for (int i = -1; i <= 1; ++i) {
                for (int j = -1; j <= 1; ++j) {
                    for (int k = -1; k <= 1; ++k) {
                        BlockPos blockpos = pos.add(i, j, k);
                        IBlockState iblockstate = worldIn.getBlockState(blockpos);
                        if (iblockstate.getBlock().isLeaves(iblockstate, worldIn, blockpos)) iblockstate.getBlock().beginLeavesDecay(iblockstate, worldIn, blockpos);
                    }
                }
            }
        }
    }

    /** The decay check: a log reachable through at most four leaves keeps the leaf, else it drops and goes. */
    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.isRemote || !state.getValue(CHECK_DECAY) || !state.getValue(DECAYABLE)) return;
        int k = pos.getX(), l = pos.getY(), i1 = pos.getZ();
        if (surroundings == null) surroundings = new int[32768];
        if (!worldIn.isAreaLoaded(new BlockPos(k - 5, l - 5, i1 - 5), new BlockPos(k + 5, l + 5, i1 + 5))) return;
        for (int i2 = -4; i2 <= 4; ++i2) {
            for (int j2 = -4; j2 <= 4; ++j2) {
                for (int k2 = -4; k2 <= 4; ++k2) {
                    BlockPos p = new BlockPos(k + i2, l + j2, i1 + k2);
                    IBlockState s = worldIn.getBlockState(p);
                    Block block = s.getBlock();
                    int index = (i2 + 16) * 1024 + (j2 + 16) * 32 + k2 + 16;
                    if (!block.canSustainLeaves(s, worldIn, p)) surroundings[index] = block.isLeaves(s, worldIn, p) ? -2 : -1;
                    else surroundings[index] = 0;
                }
            }
        }
        for (int i3 = 1; i3 <= 4; ++i3) {
            for (int j3 = -4; j3 <= 4; ++j3) {
                for (int k3 = -4; k3 <= 4; ++k3) {
                    for (int l3 = -4; l3 <= 4; ++l3) {
                        if (surroundings[(j3 + 16) * 1024 + (k3 + 16) * 32 + l3 + 16] == i3 - 1) {
                            spread(j3 - 1, k3, l3, i3);
                            spread(j3 + 1, k3, l3, i3);
                            spread(j3, k3 - 1, l3, i3);
                            spread(j3, k3 + 1, l3, i3);
                            spread(j3, k3, l3 - 1, i3);
                            spread(j3, k3, l3 + 1, i3);
                        }
                    }
                }
            }
        }
        if (surroundings[16912] >= 0) worldIn.setBlockState(pos, state.withProperty(CHECK_DECAY, false), 4);
        else destroy(worldIn, pos);
    }

    private void spread(int x, int y, int z, int distance) {
        int index = (x + 16) * 1024 + (y + 16) * 32 + z + 16;
        if (index >= 0 && index < surroundings.length && surroundings[index] == -2) surroundings[index] = distance;
    }

    private void destroy(World worldIn, BlockPos pos) {
        dropBlockAsItem(worldIn, pos, worldIn.getBlockState(pos), 0);
        worldIn.setBlockToAir(pos);
    }

    @Override
    public int quantityDropped(Random random) {
        return random.nextInt(20) == 0 ? 1 : 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(legacyconnector.v1_12_2.net.minecraft.init.Blocks.SAPLING);
    }

    /** Oak leaves drop an apple now and then (overridden by the vanilla oak leaves). */
    protected void dropApple(World worldIn, BlockPos pos, IBlockState state, int chance) {
    }

    protected int getSaplingDropChance(IBlockState state) {
        return 20;
    }

    /** Forge: a sapling by chance (better with fortune). */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Random rand = world instanceof World w ? w.rand : new Random();
        int chance = getSaplingDropChance(state);
        if (fortune > 0) {
            chance -= 2 << fortune;
            if (chance < 10) chance = 10;
        }
        if (rand.nextInt(chance) == 0) {
            Item item = getItemDropped(state, rand, fortune);
            if (item != null && item != legacyconnector.v1_12_2.net.minecraft.init.Items.AIR) drops.add(new ItemStack(item, 1, damageDropped(state)));
        }
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return !leavesFancy;
    }

    public void setGraphicsLevel(boolean fancy) {
        this.leavesFancy = fancy;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return leavesFancy ? BlockRenderLayer.CUTOUT_MIPPED : BlockRenderLayer.SOLID;
    }

    @Override
    public boolean causesSuffocation(IBlockState state) {
        return false;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return (leavesFancy || blockAccess.getBlockState(pos.offset(side)).getBlock() != this) && super.shouldSideBeRendered(blockState, blockAccess, pos, side);
    }

    @Override
    public void beginLeavesDecay(IBlockState state, World world, BlockPos pos) {
        if (!state.getValue(CHECK_DECAY)) world.setBlockState(pos, state.withProperty(CHECK_DECAY, true), 4);
    }

    @Override
    public boolean isLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }

    public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
        return true;
    }
}
