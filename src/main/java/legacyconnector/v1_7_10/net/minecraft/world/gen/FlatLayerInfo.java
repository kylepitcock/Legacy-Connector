package legacyconnector.v1_7_10.net.minecraft.world.gen;

import legacyconnector.v1_7_10.net.minecraft.block.Block;

/** One layer of a superflat preset: {@code count} blocks of a block and metadata, starting at {@link #getMinY}. */
public class FlatLayerInfo {
    private final int layerCount;
    private final Block layerBlock;
    private int layerMeta;
    private int layerMinimumY;

    public FlatLayerInfo(int count, Block block) {
        this.layerCount = count;
        this.layerBlock = block;
    }

    public FlatLayerInfo(int count, Block block, int meta) {
        this(count, block);
        this.layerMeta = meta;
    }

    public int getLayerCount() {
        return layerCount;
    }

    /** The layer's block. */
    public Block func_151536_b() {
        return layerBlock;
    }

    public int getFillBlockMeta() {
        return layerMeta;
    }

    public int getMinY() {
        return layerMinimumY;
    }

    public void setMinY(int y) {
        this.layerMinimumY = y;
    }

    @Override
    public String toString() {
        String name = Block.blockRegistry.getNameForObject(layerBlock);
        if (name == null) name = String.valueOf(Block.getIdFromBlock(layerBlock));
        String out = (layerCount > 1 ? layerCount + "*" : "") + name;
        return layerMeta > 0 ? out + ":" + layerMeta : out;
    }
}
