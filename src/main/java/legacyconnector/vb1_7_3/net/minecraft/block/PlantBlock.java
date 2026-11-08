package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Beta 1.7.3 {@code BlockFlower}: a small crossed-squares plant (render type 1) without collision. */
public class PlantBlock extends Block {
    protected PlantBlock(int id, int textureId) {
        super(id, Material.PLANT);
        this.textureId = textureId;
        setTickRandomly(true);
        float f = 0.2F;
        setBoundingBox(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f * 3.0F, 0.5F + f);
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
        return 1;
    }
}
