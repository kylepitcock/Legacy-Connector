package legacyconnector.vb1_7_3.net.minecraft.client.render.block.entity;

import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta 1.7.3 {@code TileEntitySpecialRenderer}. Mods hand theirs to {@code ModLoader.RegisterTileEntity(class, id, renderer)}; they are
 * kept for the client renderer bridge, which does not draw them yet. The dispatcher and font (synthesized types) are not declared here.
 */
public abstract class BlockEntityRenderer {
    public BlockEntityRenderer() {}

    public abstract void render(BlockEntity entity, double x, double y, double z, float partialTicks);

    protected void bindTexture(String path) {}

    public void setWorld(World world) {}
}
