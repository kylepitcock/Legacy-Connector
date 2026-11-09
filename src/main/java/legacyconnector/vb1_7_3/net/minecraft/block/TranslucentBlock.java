package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/**
 * Beta 1.7.3 {@code BlockBreakable}: the see-through base of glass and ice. Not an opaque cube; with {@code transparent} false the faces
 * between two blocks of the same id are not drawn (a wall of glass shows no inner faces).
 */
public class TranslucentBlock extends Block {
    private boolean transparent;

    protected TranslucentBlock(int id, int textureId, Material material, boolean transparent) {
        super(id, textureId, material);
        this.transparent = transparent;
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    @Override public boolean isSideVisible(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view,int x,int y,int z,int side) {
        return (transparent || view.getBlockId(x,y,z) != id) && super.isSideVisible(view,x,y,z,side);
    }

    /** True if faces against the same block are drawn (b1.7.3's {@code isSideVisible} answer for a neighbour of this id). */
    public boolean mcc$drawsFacesBetweenSame() {
        return transparent;
    }
}
