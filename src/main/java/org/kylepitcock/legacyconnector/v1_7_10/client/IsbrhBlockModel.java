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
    /**
     * The block is a full opaque cube in 1.7.10's terms (isOpaqueCube, renderAsNormalBlock, full bounds): modern neighbours cull
     * their faces against it, so each of its faces the modern pipeline would show must be drawn (see {@link #fillMissingFaces}).
     */
    private final boolean opaqueCube;
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
        boolean cube = false;
        try {
            cube = block.isOpaqueCube() && block.renderAsNormalBlock() && block.getBlockBoundsMinX() <= 0.0D && block.getBlockBoundsMinY() <= 0.0D
                    && block.getBlockBoundsMinZ() <= 0.0D && block.getBlockBoundsMaxX() >= 1.0D && block.getBlockBoundsMaxY() >= 1.0D
                    && block.getBlockBoundsMaxZ() >= 1.0D;
        } catch (RuntimeException | LinkageError e) {
            // not a cube as far as we can tell
        }
        this.opaqueCube = cube;
        var cutout = net.minecraft.client.renderer.RenderType.cutout();
        var translucent = net.minecraft.client.renderer.RenderType.translucent();
        this.renderTypes = p0 && p1 ? ChunkRenderTypeSet.of(cutout, translucent) : ChunkRenderTypeSet.of(p1 ? translucent : cutout);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData base) {
        ISimpleBlockRenderingHandler handler = currentHandler();
        if (handler == null || FAILED.contains(block.getClass().getName())) return base;
        List<BakedQuad> solid = pass0 ? capture(handler, level, pos, 0) : List.of();
        if (solid == null) return base;
        List<BakedQuad> translucent = pass1 ? capture(handler, level, pos, 1) : List.of();
        if (translucent == null) return base;
        if (opaqueCube) {
            List<BakedQuad> filled = fillMissingFaces(level, pos, state, solid, translucent);
            if (!filled.isEmpty()) {
                if (pass0) {
                    solid = new java.util.ArrayList<>(solid);
                    solid.addAll(filled);
                } else {
                    translucent = new java.util.ArrayList<>(translucent);
                    translucent.addAll(filled);
                }
            }
        }
        boolean fallback = false;
        if (solid.isEmpty() && translucent.isEmpty()) {
            // A full opaque cube must never vanish: the generated model (the block's icons on a cube) stands in.
            fallback = opaqueCube && !hasTileRenderer(level, pos);
            if (FAILED.add("empty:" + handler.getClass().getName() + ":" + block.getClass().getName())) {
                // A handler that draws nothing is no defect when the block is drawn by its tile entity's special renderer (Twilight
                // Forest's cicadas, fireflies and moonworms: their handler is empty), when the position is not a face of the block (a
                // Twilight Forest giant block shows only the faces on the border of its 4x4x4 cell), or when the block needs its other
                // half (Carpenter's beds draw nothing until both halves exist).
                if (hasTileRenderer(level, pos)) {
                    LOG.info("Legacy block renderer {} draws nothing for {} at {} (render id {}): its tile entity {} has a special renderer",
                            handler.getClass().getName(), block.getClass().getName(), pos, renderId, describeTile(level, pos));
                } else {
                    LOG.warn("Legacy block renderer {} drew nothing for {} (render id {}, tile entity at {}: {})", handler.getClass().getName(),
                            block.getClass().getName(), renderId, pos, describeTile(level, pos));
                }
            }
        } else if (FAILED.add("count:" + block.getClass().getName())) {
            LOG.info("Legacy block renderer {} drew {} cut-out and {} translucent quad(s) for {}; {}", handler.getClass().getSimpleName(),
                    solid.size(), translucent.size(), block.getClass().getSimpleName(), IsbrhQuads.describeLast());
        }
        if (fallback) return base;
        return base.derive().with(QUADS, solid).with(TRANSLUCENT_QUADS, translucent).build();
    }

    /**
     * The mod's handler as the block names it now. The render id is read again on every draw: a mod may register its handlers (and
     * so change the ids its blocks report) after the models were made, and the old id then names a stale instance that no longer
     * gets the pass the block hands to the live one (ChromatiCraft re-registers its renderers: pylon stone drew its overlay instead of its sides).
     */
    private ISimpleBlockRenderingHandler currentHandler() {
        if (builtIn != null) return builtIn;
        ISimpleBlockRenderingHandler handler = RenderingRegistry.mcc$blockHandler(currentRenderId());
        return handler != null ? handler : RenderingRegistry.mcc$blockHandler(renderId);
    }

    private int currentRenderId() {
        if (builtIn != null) return renderId;
        try {
            return block.getRenderType();
        } catch (RuntimeException | LinkageError e) {
            return renderId;
        }
    }

    /** One Forge world pass of the handler, as 1.7.10's chunk renderer ran it; null when the handler failed. */
    private List<BakedQuad> capture(ISimpleBlockRenderingHandler handler, BlockAndTintGetter level, BlockPos pos, int pass) {
        return capture(handler, handler, level, pos, pass);
    }

    /** {@code lock}: the mod's handler, even when {@code handler} is a stand-in that draws through the block (see {@link #fillMissingFaces}). */
    private List<BakedQuad> capture(ISimpleBlockRenderingHandler handler, Object lock, BlockAndTintGetter level, BlockPos pos, int pass) {
        // Chunks are meshed on several worker threads (many more with Sodium/Embeddium), but a mod's handler is a singleton that
        // keeps the pass it is told by canRenderInPass in a field (ChromatiCraft's ISBRH.setRenderPass): two threads meshing blocks of
        // the same handler would draw each other's pass (the overlay twice and the stone never: a see-through block). The legacy
        // client had one render thread, so the calls of one handler are made one at a time (different handlers still run in parallel).
        if (builtIn != null) return captureLocked(handler, level, pos, pass);
        synchronized (lock) {
            return captureLocked(handler, level, pos, pass);
        }
    }

    private List<BakedQuad> captureLocked(ISimpleBlockRenderingHandler handler, BlockAndTintGetter level, BlockPos pos, int pass) {
        int ox = relocation(pos.getX()), oz = relocation(pos.getZ());
        RenderContext.Capture capture = RenderContext.beginCapture();
        // 1.7.10's chunk renderer had the Tessellator drawing already when it called the handlers.
        var tessellator = legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.mcc$get$instance();
        boolean started = !tessellator.isDrawing;
        if (started) tessellator.startDrawingQuads();
        try {
            legacyconnector.v1_7_10.net.minecraftforge.client.ForgeHooksClient.mcc$set$worldRenderPass(pass);
            // The Tessellator is shared: a renderer that added a translation and then threw (or never took it back) must not
            // shift every later block (Reika's pipes add the block position first and subtract it last).
            tessellator.setTranslation(0, 0, 0);
            // Called right before drawing, as the chunk renderer did: some blocks note the pass here (BuildCraft pipes).
            block.canRenderInPass(pass);
            legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess access = BlockAccessView.of(level);
            // Far out (a Sable / Create Aeronautics ship's plot, ~20 million blocks away) the handler runs at coordinates moved
            // near the origin, over a view shifted back to the real blocks: its own float math (ChromatiCraft's crystals,
            // Thaumcraft's jars, vanilla's RenderBlocks) would otherwise snap vertices to the 2-block steps of a float there.
            BlockAccessView shifted = ox != 0 || oz != 0 ? BlockAccessView.shifted(level, ox, oz) : null;
            if (shifted != null) access = shifted;
            else ox = oz = 0;
            handler.renderWorldBlock(access, pos.getX() - ox, pos.getY(), pos.getZ() - oz, block, currentRenderId(), new RenderBlocks(access));
        } catch (RuntimeException | LinkageError e) {
            if (FAILED.add(block.getClass().getName())) {
                LOG.error("Legacy block renderer for {} failed; the block keeps its generated model (render id {}, tile entity at {}: {})", block.getClass().getName(),
                        renderId, pos, describeTile(level, pos), e);
            }
            return null;
        } finally {
            legacyconnector.v1_7_10.net.minecraftforge.client.ForgeHooksClient.mcc$set$worldRenderPass(-1);
            tessellator.setTranslation(0, 0, 0);
            try {
                if (started && tessellator.isDrawing) tessellator.draw();
            } finally {
                RenderContext.endCapture();
            }
        }
        return IsbrhQuads.bake(capture, pos.getX() - ox, pos.getY(), pos.getZ() - oz, getParticleIcon(ModelData.EMPTY));
    }

    /**
     * The faces of a full opaque legacy cube that the modern pipeline shows but the legacy renderer left out, drawn as 1.7.10's
     * standard cube face with the block's world icon. The modern neighbour of such a face culls its own face against the cube
     * (the block occludes), so a face the handler skips is a hole through the terrain: the sky shows through. That happens where
     * 1.7.10's view of a neighbour ({@code shouldSideBeRendered}: its {@code isOpaqueCube}) and the modern one
     * ({@code Block.shouldRenderFace}: occlusion shapes) disagree, or where the handler skipped the face for its own pass logic.
     */
    private List<BakedQuad> fillMissingFaces(BlockAndTintGetter level, BlockPos pos, BlockState state, List<BakedQuad> solid, List<BakedQuad> translucent) {
        int missing = 0;
        try {
            if (!state.canOcclude()) return List.of();
            BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
            for (Direction d : DIRECTIONS) {
                next.setWithOffset(pos, d);
                if (!net.minecraft.world.level.block.Block.shouldRenderFace(state, level, pos, d, next)) continue;
                // A block with a cut-out pass is covered by that pass alone: a second-pass overlay on the face (ChromatiCraft's glowing
                // rune edges) is no base, and counting it left the stone out when the base pass was missing.
                if (pass0 ? covers(solid, d) : covers(translucent, d)) continue;
                missing |= 1 << d.get3DDataValue();
            }
        } catch (RuntimeException | LinkageError e) {
            return List.of();
        }
        if (missing == 0) return List.of();
        if (FAILED.add("filled:" + block.getClass().getName())) {
            LOG.info("Legacy block renderer for {} left out visible faces of its full cube (sides {} at {}); they are drawn as standard faces",
                    block.getClass().getName(), Integer.toBinaryString(missing), pos);
        }
        // The stand-in calls the block's canRenderInPass, which sets the pass field of the mod's handler: it holds that handler's lock,
        // or it flips the pass of another thread's draw in progress.
        ISimpleBlockRenderingHandler real = currentHandler();
        List<BakedQuad> faces = capture(new MissingFaces(missing), real != null ? real : this, level, pos, pass0 ? 0 : 1);
        return faces == null ? List.of() : faces;
    }

    private static final Direction[] DIRECTIONS = Direction.values();

    /** Whether some quad lies in the plane of the block's face {@code d} (positions are relative to the block). */
    private static boolean covers(List<BakedQuad> quads, Direction d) {
        int axis = d.getAxis().ordinal();
        float plane = d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F : 0.0F;
        for (BakedQuad quad : quads) {
            int[] v = quad.getVertices();
            int stride = v.length / 4;
            boolean onPlane = true;
            for (int i = 0; i < 4 && onPlane; i++) onPlane = Math.abs(Float.intBitsToFloat(v[i * stride + axis]) - plane) < 1.0F / 32.0F;
            if (onPlane) return true;
        }
        return false;
    }

    /** Draws the faces in {@code mask} (bit = legacy side) as RenderBlocks' standard cube faces with the block's world icons. */
    private static final class MissingFaces implements ISimpleBlockRenderingHandler {
        private static final float[] SHADE = {0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F};
        private final int mask;

        MissingFaces(int mask) {
            this.mask = mask;
        }

        @Override
        public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        }

        @Override
        public boolean renderWorldBlock(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, Block block, int modelId,
                RenderBlocks renderer) {
            var t = legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.mcc$get$instance();
            int c = 0xFFFFFF;
            try {
                c = block.colorMultiplier(world, x, y, z) & 0xFFFFFF;
            } catch (RuntimeException e) {
                c = 0xFFFFFF;
            }
            float r = (c >> 16 & 255) / 255.0F, g = (c >> 8 & 255) / 255.0F, b = (c & 255) / 255.0F;
            for (int side = 0; side < 6; side++) {
                if ((mask & 1 << side) == 0) continue;
                float shade = SHADE[side];
                if (c != 0xFFFFFF) {
                    t.setColorOpaque_F(r * shade, g * shade, b * shade);
                } else {
                    t.mcc$setColorState(-1L);
                    t.mcc$noteFaceColor(shade);
                }
                var icon = renderer.getBlockIcon(block, world, x, y, z, side);
                switch (side) {
                    case 0 -> renderer.renderFaceYNeg(block, x, y, z, icon);
                    case 1 -> renderer.renderFaceYPos(block, x, y, z, icon);
                    case 2 -> renderer.renderFaceZNeg(block, x, y, z, icon);
                    case 3 -> renderer.renderFaceZPos(block, x, y, z, icon);
                    case 4 -> renderer.renderFaceXNeg(block, x, y, z, icon);
                    default -> renderer.renderFaceXPos(block, x, y, z, icon);
                }
            }
            return true;
        }

        @Override
        public boolean shouldRender3DInInventory(int modelId) {
            return false;
        }

        @Override
        public int getRenderId() {
            return 0;
        }
    }

    /** Whether the legacy tile entity at the position is drawn by a tile entity special renderer. */
    private static boolean hasTileRenderer(BlockAndTintGetter level, BlockPos pos) {
        try {
            var te = BlockAccessView.of(level).getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            return te != null && legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.instance
                    .getSpecialRendererByClass(te.getClass()) != null;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** For the log: the legacy tile entity the handler sees at the position ("null" makes most pipe renderers draw nothing or throw). */
    private static String describeTile(BlockAndTintGetter level, BlockPos pos) {
        try {
            var te = BlockAccessView.of(level).getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            return te == null ? "null" : te.getClass().getName();
        } catch (RuntimeException | LinkageError e) {
            return "lookup failed: " + e;
        }
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
