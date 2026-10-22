package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.List;
import legacyconnector.v1_7_10.cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockAccessView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A block drawn by a legacy {@link ISimpleBlockRenderingHandler}. While a chunk is meshed, the handler's
 * {@code renderWorldBlock} runs against the real world with the Tessellator capturing its quads; they are returned as
 * this block's (unculled) quads. Blocks whose handler does not draw anything keep the generated cube model.
 */
public class IsbrhBlockModel extends BakedModelWrapper<BakedModel> {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/ISBRH");
    private static final ModelProperty<List<BakedQuad>> QUADS = new ModelProperty<>();
    private static final java.util.Set<String> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final Block block;
    private final int renderId;
    private final boolean translucent;

    public IsbrhBlockModel(BakedModel original, Block block, int renderId) {
        super(original);
        this.block = block;
        this.renderId = renderId;
        boolean t = false;
        try {
            t = block.getRenderBlockPass() == 1;
        } catch (RuntimeException | LinkageError e) {
            // keep cutout
        }
        this.translucent = t;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData base) {
        ISimpleBlockRenderingHandler handler = RenderingRegistry.mcc$blockHandler(renderId);
        if (handler == null || FAILED.contains(block.getClass().getName())) return base;
        RenderContext.Capture capture = RenderContext.beginCapture();
        boolean drew;
        // 1.7.10's chunk renderer had the Tessellator drawing already when it called the handlers.
        var tessellator = legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.instance;
        boolean started = !tessellator.isDrawing;
        if (started) tessellator.startDrawingQuads();
        try {
            RenderBlocks renderer = new RenderBlocks(BlockAccessView.of(level));
            var access = BlockAccessView.of(level);
            drew = handler.renderWorldBlock(access, pos.getX(), pos.getY(), pos.getZ(), block, renderId, renderer);
        } catch (RuntimeException | LinkageError e) {
            if (FAILED.add(block.getClass().getName())) {
                LOG.error("Legacy block renderer for {} failed; the block keeps its generated model", block.getClass().getName(), e);
            }
            return base;
        } finally {
            try {
                if (started && tessellator.isDrawing) tessellator.draw();
            } finally {
                RenderContext.endCapture();
            }
        }
        if (!drew && capture.quads.isEmpty()) {
            if (FAILED.add("empty:" + handler.getClass().getName() + ":" + block.getClass().getName())) {
                LOG.warn("Legacy block renderer {} drew nothing for {} (render id {})", handler.getClass().getName(), block.getClass().getName(), renderId);
            }
            return base.derive().with(QUADS, List.of()).build();
        }
        if (FAILED.add("count:" + block.getClass().getName())) {
            LOG.info("Legacy block renderer {} drew {} quad(s) for {} (handler returned {})", handler.getClass().getSimpleName(), capture.quads.size(), block.getClass().getSimpleName(), drew);
        }
        List<BakedQuad> quads = IsbrhQuads.bake(capture, pos.getX(), pos.getY(), pos.getZ(), getParticleIcon(ModelData.EMPTY));
        return base.derive().with(QUADS, quads).build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> quads = data.get(QUADS);
        if (quads == null) return super.getQuads(state, side, rand, data, renderType);
        return side == null ? quads : List.of();
    }

    @Override
    public net.neoforged.neoforge.client.ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return net.neoforged.neoforge.client.ChunkRenderTypeSet.of(translucent ? net.minecraft.client.renderer.RenderType.translucent()
                : net.minecraft.client.renderer.RenderType.cutout());
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }
}
