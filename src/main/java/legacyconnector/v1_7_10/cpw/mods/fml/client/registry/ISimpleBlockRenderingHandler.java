package legacyconnector.v1_7_10.cpw.mods.fml.client.registry;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;

/** Custom block renderer by render type id (world and inventory). */
public interface ISimpleBlockRenderingHandler {
    void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer);

    boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer);

    boolean shouldRender3DInInventory(int modelId);

    int getRenderId();
}
