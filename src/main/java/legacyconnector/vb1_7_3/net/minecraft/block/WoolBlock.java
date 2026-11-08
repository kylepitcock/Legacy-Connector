package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Beta wool stores colors in reverse dye order; both conversions operate on four metadata bits. */
public class WoolBlock extends Block {
    public WoolBlock() {
        super(35, 64, Material.WOOL);
    }

    @Override
    public int getTexture(int side, int meta) {
        if (meta == 0) return textureId;
        int reversed = ~(meta & 15);
        return 113 + ((reversed & 8) >> 3) + (reversed & 7) * 16;
    }

    @Override
    public int getDroppedItemMeta(int meta) {
        return meta;
    }

    public static int getBlockMeta(int dyeMeta) {
        return ~dyeMeta & 15;
    }

    public static int getItemMeta(int woolMeta) {
        return ~woolMeta & 15;
    }
}
