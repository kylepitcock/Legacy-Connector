package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** Ice: slippery and melts to water. */
public class BlockIce extends BlockBreakable {
    public BlockIce(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockIce() {
        super("ice", Material.ice, false);
        this.slipperiness = 0.98F;
    }
}
