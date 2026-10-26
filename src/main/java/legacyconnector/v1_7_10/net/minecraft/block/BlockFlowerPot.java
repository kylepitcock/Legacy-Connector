package legacyconnector.v1_7_10.net.minecraft.block;

/** The flower pot; RenderBlocks draws the plant its tile entity holds inside it (render type 33). */
public class BlockFlowerPot extends BlockContainer {
    public BlockFlowerPot(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockFlowerPot() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.circuits);
        setBlockBoundsForItemRender();
    }

    @Override
    public void setBlockBoundsForItemRender() {
        float h = 0.375F;
        float r = h / 2.0F;
        setBlockBounds(0.5F - r, 0.0F, 0.5F - r, 0.5F + r, h, 0.5F + r);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 33;
    }

    @Override
    public boolean isFlowerPot() {
        return true;
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity createNewTileEntity(legacyconnector.v1_7_10.net.minecraft.world.World world, int meta) {
        return new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityFlowerPot(null, 0);
    }
}
