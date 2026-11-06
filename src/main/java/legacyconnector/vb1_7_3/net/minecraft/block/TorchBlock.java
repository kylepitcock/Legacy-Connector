package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Beta 1.7.3 {@code BlockTorch}: render type 2, no collision. */
public class TorchBlock extends Block {
    protected TorchBlock(int id, int textureId) {
        super(id, textureId, Material.PISTON_BREAKABLE);
        setTickRandomly(true);
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 2;
    }
}
