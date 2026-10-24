package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** A plain ore block (and the base of mods' ores). */
public class BlockOre extends Block {
    public BlockOre(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockOre() {
        super(Material.rock);
    }
}
