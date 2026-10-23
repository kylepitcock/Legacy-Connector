package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * 1.7.10 {@code BlockLeavesBase}: leaves are never opaque cubes, so they render cut out and do not hide the faces of
 * their neighbours. {@code field_150121_P} is the fancy-graphics flag (always on: the modern game draws leaves cut out).
 */
public class BlockLeavesBase extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    protected boolean field_150121_P = true;

    public BlockLeavesBase(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockLeavesBase(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockLeavesBase(legacyconnector.v1_7_10.net.minecraft.block.material.Material p0, boolean p1) {
        this(p0);
        this.field_150121_P = p1;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean shouldSideBeRendered(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
        if (!field_150121_P && world.getBlock(x, y, z) == this) return false;
        return super.shouldSideBeRendered(world, x, y, z, side);
    }
}
