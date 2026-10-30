package legacyconnector.v1_12_2.net.minecraft.world.gen;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/** One layer of a superflat preset: a block (state) repeated {@code layerCount} times from {@code minY} up. */
public class FlatLayerInfo {
    private final int version;
    private IBlockState layerMaterial;
    private int layerCount = 1;
    private int layerMinimumY;

    public FlatLayerInfo(int height, Block layerMaterialIn) {
        this(3, height, layerMaterialIn);
    }

    public FlatLayerInfo(int version, int height, Block layerMaterialIn) {
        this.version = version;
        this.layerCount = height;
        this.layerMaterial = layerMaterialIn == null ? null : layerMaterialIn.getDefaultState();
    }

    public FlatLayerInfo(int version, int height, Block layerMaterialIn, int meta) {
        this(version, height, layerMaterialIn);
        if (layerMaterialIn != null) this.layerMaterial = layerMaterialIn.getStateFromMeta(meta);
    }

    public int getLayerCount() {
        return layerCount;
    }

    public IBlockState getLayerMaterial() {
        return layerMaterial;
    }

    public int getMinY() {
        return layerMinimumY;
    }

    public void setMinY(int minY) {
        this.layerMinimumY = minY;
    }

    @Override
    public String toString() {
        Block block = layerMaterial == null ? null : layerMaterial.getBlock();
        String s;
        if (version >= 3) {
            var name = block == null ? null : block.getRegistryName();
            s = name == null ? "null" : name.toString();
            if (layerCount > 1) s = layerCount + "*" + s;
        } else {
            s = Integer.toString(Block.getIdFromBlock(block));
            if (layerCount > 1) s = layerCount + "x" + s;
        }
        int meta = block == null ? 0 : block.getMetaFromState(layerMaterial);
        if (meta > 0) s = s + ":" + meta;
        return s;
    }
}
