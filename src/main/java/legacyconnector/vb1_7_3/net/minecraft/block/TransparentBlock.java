package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Beta 1.7.3 {@code BlockLeavesBase}: the see-through base of leaves; {@code renderSides} was the fancy-graphics switch. */
public class TransparentBlock extends Block {
    protected boolean renderSides;

    protected TransparentBlock(int id, int textureId, Material material, boolean renderSides) {
        super(id, textureId, material);
        this.renderSides = renderSides;
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    @Override public boolean isSideVisible(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view,int x,int y,int z,int side) {
        return (renderSides || view.getBlockId(x,y,z) != id) && super.isSideVisible(view,x,y,z,side);
    }
}
