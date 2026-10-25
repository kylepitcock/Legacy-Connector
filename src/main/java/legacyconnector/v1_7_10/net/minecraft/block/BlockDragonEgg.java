package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** The dragon egg block. */
public class BlockDragonEgg extends Block {
    public BlockDragonEgg(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockDragonEgg() {
        super(Material.dragonEgg);
    }
}
