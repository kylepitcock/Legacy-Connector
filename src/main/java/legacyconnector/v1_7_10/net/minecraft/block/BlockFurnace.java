package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/**
 * The furnace. 1.7.10 swapped {@code furnace} and {@code lit_furnace} while it burned; the modern furnace (and smoker, blast
 * furnace) has a {@code lit} property instead, which {@link #updateFurnaceBlockState} sets.
 */
public class BlockFurnace extends BlockContainer {
    /** {@code field_149932_b}: this is the burning furnace block. */
    protected final boolean isBurning;
    /** {@code field_149934_M}: set while the block is being swapped, so the tile entity is kept. */
    public static boolean field_149934_M;

    public BlockFurnace(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.isBurning = "minecraft:lit_furnace".equals(info.legacyName());
    }

    protected BlockFurnace(boolean burning) {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.rock);
        this.isBurning = burning;
    }

    /** Lights or puts out the furnace at the position, keeping its contents and facing. */
    public static void updateFurnaceBlockState(boolean burning, World world, int x, int y, int z) {
        var level = VanillaOps.live(world);
        if (level == null) return;
        var pos = VanillaOps.pos(x, y, z);
        var state = level.getBlockState(pos);
        var lit = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
        if (state.hasProperty(lit) && state.getValue(lit) != burning) level.setBlock(pos, state.setValue(lit, burning), 3);
    }
}
