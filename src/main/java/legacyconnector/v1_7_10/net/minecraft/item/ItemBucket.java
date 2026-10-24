package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;

/** A bucket carrying a fluid block ({@code Blocks.air} when empty). */
public class ItemBucket extends Item {
    private Block isFull;

    public ItemBucket(Block containedBlock) {
        this.isFull = containedBlock;
        this.maxStackSize = 1;
    }

    public ItemBucket(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public Block mcc$contained() {
        return isFull;
    }
}
