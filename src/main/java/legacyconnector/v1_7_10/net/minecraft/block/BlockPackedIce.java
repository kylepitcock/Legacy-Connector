package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** Packed ice: slippery, never melts into water and drops nothing without silk touch. */
public class BlockPackedIce extends Block {
    public BlockPackedIce(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockPackedIce() {
        super(Material.packedIce);
        this.slipperiness = 0.98F;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabBlock);
    }

    @Override
    public int quantityDropped(Random random) {
        return mcc$vanilla != null ? super.quantityDropped(random) : 0;
    }
}
