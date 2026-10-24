package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** A wall or floor torch (and the base of mods' torches). */
public class BlockTorch extends Block {
    public BlockTorch(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockTorch() {
        super(Material.circuits);
    }

    protected BlockTorch(Material material) {
        super(material);
    }
}
