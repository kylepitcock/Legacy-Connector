package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's block renderer. Meshing into a legacy buffer goes through {@link BlockModelRenderer}; drawing one block with a brightness
 * (TESRs drawing a block) draws the modern block into the render context, model and all.
 */
public class BlockRendererDispatcher {
    private static BlockRendererDispatcher instance;
    private final BlockModelShapes blockModelShapes;
    private final BlockModelRenderer blockModelRenderer;

    public BlockRendererDispatcher(BlockModelShapes shapes, BlockColors colors) {
        blockModelShapes = shapes;
        blockModelRenderer = new BlockModelRenderer(colors);
    }

    /** The game's block renderer (Minecraft.getBlockRendererDispatcher()). */
    public static synchronized BlockRendererDispatcher mcc$instance() {
        if (instance == null) instance = new BlockRendererDispatcher(ModelManager.mcc$instance().getBlockModelShapes(), BlockColors.mcc$instance());
        return instance;
    }

    public BlockModelShapes getBlockModelShapes() {
        return blockModelShapes;
    }

    public void renderBlockDamage(IBlockState state, BlockPos pos, TextureAtlasSprite texture, IBlockAccess blockAccess) {
    }

    public boolean renderBlock(IBlockState state, BlockPos pos, IBlockAccess blockAccess, BufferBuilder bufferBuilderIn) {
        try {
            IBakedModel model = getModelForState(state);
            return blockModelRenderer.renderModel(blockAccess, model, state, pos, bufferBuilderIn, true);
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("meshing a block", e);
            return false;
        }
    }

    public BlockModelRenderer getBlockModelRenderer() {
        return blockModelRenderer;
    }

    public IBakedModel getModelForState(IBlockState state) {
        return blockModelShapes.getModelForState(state);
    }

    /** Draws a block at the current matrix (corner at the origin, turned a quarter as vanilla did) with the game's block renderer. */
    @SuppressWarnings("deprecation")
    public void renderBlockBrightness(IBlockState state, float brightness) {
        var pose = RenderContext.pose();
        if (pose == null || state == null) return;
        pose.pushPose();
        try {
            // vanilla turned the model a quarter about y (callers offset for it)
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            var modern = StatesBridge1122.toModern(state);
            net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderSingleBlock(modern, pose, RenderContext.buffers(), RenderContext.vertexLight(),
                    RenderContext.overlay());
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing a block", e);
        } finally {
            pose.popPose();
        }
    }
}
