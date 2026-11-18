package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySilverfish;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A silverfish block (1.7.10 {@code BlockSilverfish}): looks like stone (0), cobblestone (1) or stone bricks (2-5) and releases a
 * silverfish when broken by a player instead of dropping anything.
 */
public class BlockSilverfish extends Block {
    public BlockSilverfish(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockSilverfish() {
        super(Material.clay);
        setHardness(0.0F);
        setCreativeTab(CreativeTabs.tabDecorations);
    }

    /** {@code func_150196_a}: whether the block can be infested (stone, cobblestone, stone bricks). */
    public static boolean func_150196_a(Block block) {
        return block == Blocks.stone || block == Blocks.cobblestone || block == Blocks.stonebrick;
    }

    /** {@code func_150195_a}: the silverfish block metadata that imitates the block with the given metadata. */
    public static int func_150195_a(Block block, int meta) {
        if (meta == 0 && block == Blocks.cobblestone) return 1;
        if (block == Blocks.stonebrick) {
            switch (meta) {
                case 1:
                    return 3;
                case 2:
                    return 4;
                case 3:
                    return 5;
                default:
                    return 2;
            }
        }
        return 0;
    }

    /** {@code func_150197_b}: the block and metadata a silverfish block with the given metadata imitates (stone for 0). */
    public static org.apache.commons.lang3.tuple.ImmutablePair<Block, Integer> func_150197_b(int meta) {
        return switch (meta) {
            case 1 -> new org.apache.commons.lang3.tuple.ImmutablePair<>(Blocks.cobblestone, 0);
            case 2 -> new org.apache.commons.lang3.tuple.ImmutablePair<>(Blocks.stonebrick, 0);
            case 3 -> new org.apache.commons.lang3.tuple.ImmutablePair<>(Blocks.stonebrick, 1);
            case 4 -> new org.apache.commons.lang3.tuple.ImmutablePair<>(Blocks.stonebrick, 2);
            case 5 -> new org.apache.commons.lang3.tuple.ImmutablePair<>(Blocks.stonebrick, 3);
            default -> new org.apache.commons.lang3.tuple.ImmutablePair<>(Blocks.stone, 0);
        };
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (mcc$vanilla != null) return super.getIcon(side, meta);
        switch (meta) {
            case 1:
                return Blocks.cobblestone.getBlockTextureFromSide(side);
            case 2:
                return Blocks.stonebrick.getIcon(side, 0);
            case 3:
                return Blocks.stonebrick.getIcon(side, 1);
            case 4:
                return Blocks.stonebrick.getIcon(side, 2);
            case 5:
                return Blocks.stonebrick.getIcon(side, 3);
            default:
                return Blocks.stone.getBlockTextureFromSide(side);
        }
    }

    @Override
    public void onBlockDestroyedByPlayer(World world, int x, int y, int z, int meta) {
        super.onBlockDestroyedByPlayer(world, x, y, z, meta);
        if (mcc$vanilla != null || world.isRemote) return;
        EntitySilverfish silverfish = new EntitySilverfish(world);
        silverfish.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
        world.spawnEntityInWorld(silverfish);
        silverfish.spawnExplosionParticle();
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    protected ItemStack createStackedBlock(int meta) {
        switch (meta) {
            case 1:
                return new ItemStack(Blocks.cobblestone);
            case 2:
                return new ItemStack(Blocks.stonebrick);
            case 3:
                return new ItemStack(Blocks.stonebrick, 1, 1);
            case 4:
                return new ItemStack(Blocks.stonebrick, 1, 2);
            case 5:
                return new ItemStack(Blocks.stonebrick, 1, 3);
            default:
                return new ItemStack(Blocks.stone);
        }
    }

    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z);
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < 6; ++i) list.add(new ItemStack(item, 1, i));
    }
}
