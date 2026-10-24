package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.BlockSlab;

/** The item of a slab: placing it on a half slab of the same kind makes the double slab. */
public class ItemSlab extends ItemBlock {
    private final boolean field_150948_b;
    private final BlockSlab field_150949_c;
    private final BlockSlab field_150947_d;

    public ItemSlab(Block block, BlockSlab single, BlockSlab full, boolean isFull) {
        super(block);
        this.field_150949_c = single;
        this.field_150947_d = full;
        this.field_150948_b = isFull;
    }

    public ItemSlab(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info, Block block) {
        super(info, block);
        this.field_150949_c = null;
        this.field_150947_d = null;
        this.field_150948_b = false;
    }
}
