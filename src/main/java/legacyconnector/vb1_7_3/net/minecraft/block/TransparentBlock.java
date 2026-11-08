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
}
