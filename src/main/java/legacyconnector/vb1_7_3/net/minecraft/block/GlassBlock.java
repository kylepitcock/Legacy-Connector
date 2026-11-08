package legacyconnector.vb1_7_3.net.minecraft.block;

import java.util.Random;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Beta 1.7.3 {@code BlockGlass}: drops nothing. */
public class GlassBlock extends TranslucentBlock {
    public GlassBlock(int id, int textureId, Material material, boolean transparent) {
        super(id, textureId, material, transparent);
    }

    @Override
    public int getDroppedItemCount(Random random) {
        return 0;
    }

    @Override
    public int getRenderLayer() {
        return 0;
    }
}
