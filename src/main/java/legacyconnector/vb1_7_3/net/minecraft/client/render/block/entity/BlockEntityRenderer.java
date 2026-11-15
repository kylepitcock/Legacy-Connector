package legacyconnector.vb1_7_3.net.minecraft.client.render.block.entity;

import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta 1.7.3 {@code TileEntitySpecialRenderer}. Mods hand theirs to {@code ModLoader.RegisterTileEntity(class, id, renderer)}; they are
 * drawn by the client renderer bridge. The font (a synthesized type) is not declared here.
 */
public abstract class BlockEntityRenderer {
    /** The dispatcher a mod hands its renderer when it registers it itself ({@link BlockEntityRenderDispatcher#renderers}). */
    public BlockEntityRenderDispatcher dispatcher;

    public BlockEntityRenderer() {}

    public void setDispatcher(BlockEntityRenderDispatcher dispatcher) { this.dispatcher = dispatcher; }

    public abstract void render(BlockEntity entity, double x, double y, double z, float partialTicks);

    protected void bindTexture(String path) { org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.bind(path); }

    public void setWorld(World world) {}
}
