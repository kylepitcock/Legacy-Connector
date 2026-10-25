package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/**
 * Vanilla 1.7.10 stairs: look and properties come from the model block. Metadata bits 0-1 are the facing
 * (0 east, 1 west, 2 south, 3 north), bit 2 upside-down. Rendered with the modern stairs model.
 */
public class BlockStairs extends Block {
    public final Block field_150149_b;
    public final int field_150151_M;

    public BlockStairs(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_150149_b = null;
        this.field_150151_M = 0;
    }

    protected BlockStairs(Block model, int modelMeta) {
        super(model == null ? null : model.getMaterial());
        this.field_150149_b = model;
        this.field_150151_M = modelMeta;
        if (model != null) {
            this.setHardness(model.mcc$hardness());
            this.setResistance(model.getExplosionResistance(null) / 3.0F);
            this.setStepSound(model.stepSound);
        }
        this.setLightOpacity(255);
        this.useNeighborBrightness = true;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return field_150149_b == null ? super.getIcon(side, meta) : field_150149_b.getIcon(side, field_150151_M);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 10;
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        // icons come from the model block
    }

    public static boolean isBlockStairs(Block block) {
        return block instanceof BlockStairs;
    }
}
