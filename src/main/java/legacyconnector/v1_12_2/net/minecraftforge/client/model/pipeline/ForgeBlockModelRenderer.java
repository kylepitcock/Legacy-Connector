package legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BlockModelRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** Forge 1.12.2's block model renderer through the quad pipeline; {@link #render} pipes a model's quads through a vertex lighter. */
public class ForgeBlockModelRenderer extends BlockModelRenderer {
    public ForgeBlockModelRenderer(BlockColors colors) {
        super(colors);
    }

    /**
     * Pipes the model's unculled quads and the quads of every side that is not hidden (when {@code checkSides}) through the lighter,
     * which writes them on to its parent; answers whether anything was drawn.
     */
    public static boolean render(VertexLighterFlat lighter, IBlockAccess world, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder wr,
            boolean checkSides, long rand) {
        lighter.setWorld(world);
        lighter.setState(state);
        lighter.setBlockPos(pos);
        boolean empty = true;
        List<BakedQuad> quads = model.getQuads(state, null, rand);
        if (!quads.isEmpty()) {
            lighter.updateBlockInfo();
            empty = false;
            for (BakedQuad quad : quads) quad.pipe(lighter);
        }
        for (EnumFacing side : EnumFacing.values()) {
            quads = model.getQuads(state, side, rand);
            if (quads.isEmpty() || checkSides && !state.shouldSideBeRendered(world, pos, side)) continue;
            if (empty) lighter.updateBlockInfo();
            empty = false;
            for (BakedQuad quad : quads) quad.pipe(lighter);
        }
        lighter.resetBlockInfo();
        return !empty;
    }
}
