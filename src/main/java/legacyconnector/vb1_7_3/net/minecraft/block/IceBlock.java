package legacyconnector.vb1_7_3.net.minecraft.block;

import java.util.Random;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Beta 1.7.3 {@code BlockIce}: translucent (render layer 1), slippery, drops nothing. */
public class IceBlock extends TranslucentBlock {
    public IceBlock(int id, int textureId) {
        super(id, textureId, Material.ICE, false);
        this.slipperiness = 0.98F;
        setTickRandomly(true);
    }

    @Override
    public int getRenderLayer() {
        return 1;
    }

    @Override
    public int getDroppedItemCount(Random random) {
        return 0;
    }
}
