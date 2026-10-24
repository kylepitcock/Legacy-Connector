package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** A block with sixteen dye colours in its metadata (wool, stained clay, carpet). */
public class BlockColored extends Block {
    public BlockColored(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockColored(Material material) {
        super(material);
    }

    /** Block metadata to dye metadata (and back): the two are mirrored. */
    public static int func_150032_b(int meta) {
        return ~meta & 15;
    }

    public static int func_150031_c(int meta) {
        return ~meta & 15;
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }
}
