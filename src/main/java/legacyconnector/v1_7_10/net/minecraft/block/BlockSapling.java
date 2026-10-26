package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/**
 * The 1.7.10 class {@code net.minecraft.block.BlockSapling}. Metadata bits 0-2 are the wood type (oak, spruce, birch, jungle,
 * acacia, dark oak), bit 3 the growth mark. The vanilla sapling grows through the modern block; a mod sapling that does not
 * override {@link #growTree} grows the modern vanilla tree of its wood type.
 */
public class BlockSapling extends legacyconnector.v1_7_10.net.minecraft.block.BlockBush implements legacyconnector.v1_7_10.net.minecraft.block.IGrowable {
    public static final String[] field_149882_a = {"oak", "spruce", "birch", "jungle", "acacia", "roofed_oak"};

    public BlockSapling(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockSapling() {
        super();
        float f = 0.4F;
        setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f * 2.0F, 0.5F + f);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabDecorations);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (mcc$vanilla != null) {
            super.updateTick(world, x, y, z, random);
            return;
        }
        if (world.isRemote) return;
        super.updateTick(world, x, y, z, random);
        if (world.getBlock(x, y, z) == this && world.getBlockLightValue(x, y + 1, z) >= 9 && random.nextInt(7) == 0) {
            markOrGrowMarked(world, x, y, z, random);
        }
    }

    /** {@code func_149879_c}: the first call marks the sapling (meta bit 3), the next one grows it. */
    public void markOrGrowMarked(World world, int x, int y, int z, Random random) {
        if (mcc$vanilla != null) {
            // the modern sapling keeps the mark as its stage property
            var level = VanillaOps.server(world);
            var pos = VanillaOps.pos(x, y, z);
            if (level != null && level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.SaplingBlock s) {
                s.advanceTree(level, pos, level.getBlockState(pos), VanillaOps.random(random, level));
            }
            return;
        }
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & 8) == 0) world.setBlockMetadataWithNotify(x, y, z, meta | 8, 4);
        else growTree(world, x, y, z, random);
    }

    /** {@code func_149878_d}: replaces the sapling with a tree of its wood type (2x2 saplings grow the large spruce, jungle or dark oak). */
    public void growTree(World world, int x, int y, int z, Random random) {
        if (!legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.TerrainGen.saplingGrowTree(world, random, x, y, z)) return;
        var level = VanillaOps.server(world);
        if (level == null) return;
        var pos = VanillaOps.pos(x, y, z);
        var state = level.getBlockState(pos);
        var rand = VanillaOps.random(random, level);
        if (state.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock s) {
            if (state.hasProperty(net.minecraft.world.level.block.SaplingBlock.STAGE)) {
                state = state.setValue(net.minecraft.world.level.block.SaplingBlock.STAGE, 1);
            }
            s.advanceTree(level, pos, state, rand);
            return;
        }
        net.minecraft.world.level.block.grower.TreeGrower grower = switch (world.getBlockMetadata(x, y, z) & 7) {
            case 1 -> net.minecraft.world.level.block.grower.TreeGrower.SPRUCE;
            case 2 -> net.minecraft.world.level.block.grower.TreeGrower.BIRCH;
            case 3 -> net.minecraft.world.level.block.grower.TreeGrower.JUNGLE;
            case 4 -> net.minecraft.world.level.block.grower.TreeGrower.ACACIA;
            case 5 -> net.minecraft.world.level.block.grower.TreeGrower.DARK_OAK;
            default -> net.minecraft.world.level.block.grower.TreeGrower.OAK;
        };
        grower.growTree(level, level.getChunkSource().getGenerator(), pos, state, rand);
    }

    /** {@code func_149880_a}: whether the block at the position is this sapling with the given wood type. */
    public boolean func_149880_a(World world, int x, int y, int z, int type) {
        return world.getBlock(x, y, z) == this && (world.getBlockMetadata(x, y, z) & 7) == type;
    }

    @Override
    public int damageDropped(int meta) {
        return Math.max(0, Math.min(meta & 7, 5));
    }

    @Override
    public boolean canFertilize(World world, int x, int y, int z, boolean isClient) {
        return true;
    }

    @Override
    public boolean shouldFertilize(World world, Random random, int x, int y, int z) {
        return world.rand.nextFloat() < 0.45D;
    }

    @Override
    public void fertilize(World world, Random random, int x, int y, int z) {
        markOrGrowMarked(world, x, y, z, random);
    }
}
