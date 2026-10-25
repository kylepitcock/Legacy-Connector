package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** Glass: see-through, and the base of glass blocks that mods add. */
public class BlockGlass extends BlockBreakable {
    public BlockGlass(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockGlass(Material material, boolean ignoreSimilarity) {
        super("glass", material, ignoreSimilarity);
    }
}
