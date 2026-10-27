package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/** A see-through full block (glass, ice): not opaque; next to the same block the faces between are not drawn unless it says so. */
public class BlockBreakable extends Block {
    private final boolean ignoreSimilarity;

    protected BlockBreakable(Material material, boolean ignoreSimilarity) {
        this(material, ignoreSimilarity, material == null ? null : material.getMaterialMapColor());
    }

    protected BlockBreakable(Material material, boolean ignoreSimilarity, MapColor color) {
        super(material, color);
        this.ignoreSimilarity = ignoreSimilarity;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    /** Whether the faces against the same block are hidden (the modern block's skipRendering asks). */
    public boolean mcc$hidesSimilarNeighbours() {
        return !ignoreSimilarity;
    }
}
