package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;

/** Beta 1.7.3 {@code ItemBlock}: the item of block {@code id + 256}, which shares the block's id. */
public class BlockItem extends Item {
    private int itemId;

    public BlockItem(int id) {
        super(id);
        this.itemId = id + 256;
        Block block = Block.BLOCKS[id + 256];
        if (block != null) setTextureId(block.getTexture(2));
    }

    /** For the vanilla views ({@code VanillaB173}), allocated without a constructor. */
    public void mcc$initVanillaBlock(int blockId) {
        this.itemId = blockId;
    }

    /** The block this item places. */
    public int mcc$blockId() {
        return itemId;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        Block block = Block.BLOCKS[itemId];
        return block == null ? super.getTranslationKey(stack) : block.getTranslationKey();
    }

    @Override
    public String getTranslationKey() {
        Block block = Block.BLOCKS[itemId];
        return block == null ? super.getTranslationKey() : block.getTranslationKey();
    }
}
