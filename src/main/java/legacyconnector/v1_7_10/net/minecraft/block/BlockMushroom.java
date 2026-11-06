package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/**
 * A small mushroom: stays on mycelium, podzol, or in the dark on blocks that sustain it. Bone meal grows the vanilla mushrooms into
 * huge ones (through the modern mushroom); 1.7.10 had no huge form for mods' mushrooms.
 */
public class BlockMushroom extends legacyconnector.v1_7_10.net.minecraft.block.BlockFlower implements legacyconnector.v1_7_10.net.minecraft.block.IGrowable {
    public BlockMushroom(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockMushroom() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.plants);
        float f = 0.2F;
        setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f * 2.0F, 0.5F + f);
        setTickRandomly(true);
    }

    // 1.6.4's BlockMushroom extends BlockFlower (Block.mushroomBrown is typed BlockFlower there); the flower behaviour does not apply to it
    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        this.blockIcon = register.registerIcon(getTextureName());
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    @Override
    public void getSubBlocks(legacyconnector.v1_7_10.net.minecraft.item.Item item, legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs tab, java.util.List list) {
        list.add(new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, 1, 0));
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && canBlockStay(world, x, y, z);
    }

    @Override
    protected boolean canPlaceBlockOn(Block block) {
        return block.isFullBlock();
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        if (y < 0 || y >= world.getHeight()) return false;
        Block below = world.getBlock(x, y - 1, z);
        return below == legacyconnector.v1_7_10.net.minecraft.init.Blocks.mycelium
                || below == legacyconnector.v1_7_10.net.minecraft.init.Blocks.dirt && world.getBlockMetadata(x, y - 1, z) == 2
                || world.getFullBlockLightValue(x, y, z) < 13
                        && below.canSustainPlant(world, x, y - 1, z, legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.UP, this);
    }

    /** {@code func_149884_c}: turns the mushroom into a huge mushroom; false (and the mushroom stays) when there is no room. */
    public boolean fertilizeMushroom(World world, int x, int y, int z, Random random) {
        var level = VanillaOps.server(world);
        if (level == null) return false;
        var pos = VanillaOps.pos(x, y, z);
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.MushroomBlock mushroom) {
            return mushroom.growMushroom(level, pos, state, VanillaOps.random(random, level));
        }
        return false;
    }

    @Override
    public boolean canFertilize(World world, int x, int y, int z, boolean isClient) {
        return true;
    }

    @Override
    public boolean shouldFertilize(World world, Random random, int x, int y, int z) {
        return random.nextFloat() < 0.4D;
    }

    @Override
    public void fertilize(World world, Random random, int x, int y, int z) {
        fertilizeMushroom(world, x, y, z, random);
    }
}
