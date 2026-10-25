package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** Air: the block of empty space. */
public class BlockAir extends Block {
    public BlockAir(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockAir() {
        super(Material.air);
    }
}
