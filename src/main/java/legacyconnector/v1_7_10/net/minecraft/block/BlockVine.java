package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** Vines: climbable, spread over solid blocks. */
public class BlockVine extends Block {
    public BlockVine(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockVine() {
        super(Material.vine);
    }
}
