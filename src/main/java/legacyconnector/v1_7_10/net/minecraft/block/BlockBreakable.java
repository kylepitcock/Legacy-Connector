package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;

/** Vanilla 1.7.10 glass/ice base: a named texture, not opaque, hides faces against itself. */
public class BlockBreakable extends Block {
    private boolean field_149996_a;
    private String field_149995_b;

    public BlockBreakable(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockBreakable(Material material) {
        super(material);
    }

    protected BlockBreakable(String textureName, Material material, boolean ignoreSimilarity) {
        super(material);
        this.field_149996_a = ignoreSimilarity;
        this.field_149995_b = textureName;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    /** Whether neighbouring blocks of this kind hide the faces between them (glass); read by the adapter's skipRendering. */
    public boolean mcc$hidesSimilarNeighbours() {
        return !field_149996_a;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        if (!field_149996_a && world.getBlock(x, y, z) == this) return false;
        return super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        this.blockIcon = register.registerIcon(field_149995_b != null ? field_149995_b : getTextureName());
    }
}
