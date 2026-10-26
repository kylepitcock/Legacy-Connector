package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/**
 * A block that falls when nothing holds it up (sand, gravel, anvils, mods' falling blocks). A mod's falling block falls as a modern
 * falling block entity carrying its state; vanilla blocks fall through the modern game.
 */
public class BlockFalling extends Block {
    /** Set while chunks generate: blocks drop to the ground at once instead of becoming falling entities. */
    public static boolean fallInstantly;

    public BlockFalling(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockFalling(Material material) {
        super(material);
    }

    public BlockFalling() {
        this(Material.sand);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabBlock);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (mcc$vanilla == null) world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (mcc$vanilla == null) world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (mcc$vanilla != null) {
            super.updateTick(world, x, y, z, random);
            return;
        }
        if (!world.isRemote) mcc$tryToFall(world, x, y, z);
    }

    /** {@code func_149830_m}: starts falling (or, in generation, drops straight to the ground) when the block below is free. */
    private void mcc$tryToFall(World world, int x, int y, int z) {
        if (!canFallBelow(world, x, y - 1, z) || y < 0) return;
        var level = VanillaOps.server(world);
        if (!fallInstantly && level != null) {
            var pos = VanillaOps.pos(x, y, z);
            var state = level.getBlockState(pos);
            if (state.getBlock() == org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.modernBlock(this)) {
                net.minecraft.world.entity.item.FallingBlockEntity.fall(level, pos, state);
            }
            return;
        }
        int meta = world.getBlockMetadata(x, y, z);
        world.setBlockToAir(x, y, z);
        while (canFallBelow(world, x, y - 1, z) && y > 0) --y;
        if (y > 0) world.setBlock(x, y, z, this, meta, 3);
    }

    /** {@code func_149829_a}: called with the falling entity before it spawns (anvils make it hurt what it lands on). */
    protected void onStartFalling(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityFallingBlock entity) {
    }

    @Override
    public int tickRate(World world) {
        return 2;
    }

    /** {@code func_149831_e}: whether a falling block can move into the position (air, fire, water or lava). */
    public static boolean canFallBelow(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block.isAir(world, x, y, z)) return true;
        if (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.fire) return true;
        Material material = block.getMaterial();
        return material == Material.water || material == Material.lava;
    }

    /** {@code func_149828_a}: called where the block landed (anvils play their landing sound). */
    public void playSoundWhenFallen(World world, int x, int y, int z, int meta) {
    }
}
