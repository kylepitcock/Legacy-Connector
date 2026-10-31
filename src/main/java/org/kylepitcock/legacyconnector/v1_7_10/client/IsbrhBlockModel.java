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
    private static final ModelProperty<List<BakedQuad>> TRANSLUCENT_QUADS = new ModelProperty<>();
    private static final java.util.Set<String> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final Block block;
    private final int renderId;
    /** A built-in handler of a vanilla render type (see VanillaBlockRenderers); null: the mod's handler of the render id. */
    private final ISimpleBlockRenderingHandler builtIn;
    /** Forge world passes the block is drawn in: 0 cut out, 1 translucent (pipes draw in both). */
    private final boolean pass0, pass1;
    private final ChunkRenderTypeSet renderTypes;

    public IsbrhBlockModel(BakedModel original, Block block, int renderId) {
        this(original, block, renderId, null);
    }

    public IsbrhBlockModel(BakedModel original, Block block, int renderId, ISimpleBlockRenderingHandler builtIn) {
        super(original);
        this.block = block;
        this.renderId = renderId;
        this.builtIn = builtIn;
        boolean p0 = true, p1 = false;
        try {
            p0 = block.canRenderInPass(0);
            p1 = block.canRenderInPass(1);
        } catch (RuntimeException | LinkageError e) {
            // keep cutout
        }
        if (!p0 && !p1) p0 = true;
        this.pass0 = p0;
        this.pass1 = p1;
        var cutout = net.minecraft.client.renderer.RenderType.cutout();
        var translucent = net.minecraft.client.renderer.RenderType.translucent();
        this.renderTypes = p0 && p1 ? ChunkRenderTypeSet.of(cutout, translucent) : ChunkRenderTypeSet.of(p1 ? translucent : cutout);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData base) {
        ISimpleBlockRenderingHandler handler = builtIn != null ? builtIn : RenderingRegistry.mcc$blockHandler(renderId);
        if (handler == null || FAILED.contains(block.getClass().getName())) return base;
        List<BakedQuad> solid = pass0 ? capture(handler, level, pos, 0) : List.of();
        if (solid == null) return base;
        List<BakedQuad> translucent = pass1 ? capture(handler, level, pos, 1) : List.of();
        if (translucent == null) return base;
        if (solid.isEmpty() && translucent.isEmpty()) {
            if (FAILED.add("empty:" + handler.getClass().getName() + ":" + block.getClass().getName())) {
                LOG.warn("Legacy block renderer {} drew nothing for {} (render id {})", handler.getClass().getName(), block.getClass().getName(), renderId);
            }
        } else if (FAILED.add("count:" + block.getClass().getName())) {
            LOG.info("Legacy block renderer {} drew {} cut-out and {} translucent quad(s) for {}; {}", handler.getClass().getSimpleName(),
                    solid.size(), translucent.size(), block.getClass().getSimpleName(), IsbrhQuads.describeLast());
        }
        return base.derive().with(QUADS, solid).with(TRANSLUCENT_QUADS, translucent).build();
    }

    /** One Forge world pass of the handler, as 1.7.10's chunk renderer ran it; null when the handler failed. */
    private List<BakedQuad> capture(ISimpleBlockRenderingHandler handler, BlockAndTintGetter level, BlockPos pos, int pass) {
        int ox = relocation(pos.getX()), oz = relocation(pos.getZ());
        RenderContext.Capture capture = RenderContext.beginCapture();
        // 1.7.10's chunk renderer had the Tessellator drawing already when it called the handlers.
        var tessellator = legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.instance;
        boolean started = !tessellator.isDrawing;
        if (started) tessellator.startDrawingQuads();
        try {
            legacyconnector.v1_7_10.net.minecraftforge.client.ForgeHooksClient.worldRenderPass = pass;
            // Called right before drawing, as the chunk renderer did: some blocks note the pass here (BuildCraft pipes).
            block.canRenderInPass(pass);
            legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess access = BlockAccessView.of(level);
            // Far out (a Sable / Create Aeronautics ship's plot, ~20 million blocks away) the handler runs at coordinates moved
            // near the origin, over a view shifted back to the real blocks: its own float math (ChromatiCraft's crystals,
            // Thaumcraft's jars, vanilla's RenderBlocks) would otherwise snap vertices to the 2-block steps of a float there.
            BlockAccessView shifted = ox != 0 || oz != 0 ? BlockAccessView.shifted(level, ox, oz) : null;
            if (shifted != null) access = shifted;
            else ox = oz = 0;
            handler.renderWorldBlock(access, pos.getX() - ox, pos.getY(), pos.getZ() - oz, block, renderId, new RenderBlocks(access));
        } catch (RuntimeException | LinkageError e) {
            if (FAILED.add(block.getClass().getName())) {
                LOG.error("Legacy block renderer for {} failed; the block keeps its generated model", block.getClass().getName(), e);
            }
            return null;
        } finally {
            legacyconnector.v1_7_10.net.minecraftforge.client.ForgeHooksClient.worldRenderPass = -1;
            try {
                if (started && tessellator.isDrawing) tessellator.draw();
            } finally {
                RenderContext.endCapture();
            }
        }
        return IsbrhQuads.bake(capture, pos.getX() - ox, pos.getY(), pos.getZ() - oz, getParticleIcon(ModelData.EMPTY));
    }

    /**
     * How far a coordinate is moved toward the origin for the handler: 0 within a million blocks (normal worlds draw as they
     * always did), else a multiple of 65536 (blocks keep their position within chunks and regions; what is left is below
     * 65536, where a float still resolves 1/256 of a block).
     */
    static int relocation(int c) {
        return c > -(1 << 20) && c < 1 << 20 ? 0 : c & ~0xFFFF;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> quads = data.get(QUADS);
        if (quads == null) return super.getQuads(state, side, rand, data, renderType);
        if (side != null) return List.of();
        if (renderType == null) {
            List<BakedQuad> translucent = data.get(TRANSLUCENT_QUADS);
            if (translucent == null || translucent.isEmpty()) return quads;
            List<BakedQuad> all = new java.util.ArrayList<>(quads);
            all.addAll(translucent);
            return all;
        }
        return renderType == net.minecraft.client.renderer.RenderType.translucent() ? data.get(TRANSLUCENT_QUADS) : quads;
    }

    @Override
    public net.neoforged.neoforge.client.ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return renderTypes;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }
}
