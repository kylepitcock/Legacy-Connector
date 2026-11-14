package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;
import legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;

/**
 * Blocks drawn by a render type a mod added at run time (an {@code EnumBlockRenderType} constant made with {@code EnumHelper.addEnum}: CodeChickenLib's
 * {@code BlockRenderingRegistry.createRenderType}, whose registered renderers draw Thermal Dynamics' ducts, Thermal Expansion's machines and others).
 * 1.12's chunk renderer asked {@code Minecraft.getBlockRendererDispatcher().renderBlock(state, pos, world, buffer)} for every block whose render type is
 * not INVISIBLE, LIQUID or ENTITYBLOCK_ANIMATED; a mod that installed its own dispatcher (CodeChickenLib replaces the field by reflection) saw the
 * block there and drew it into the buffer. The model of such a block here does the same when the chunk is compiled: it hands the legacy dispatcher a
 * legacy {@link BufferBuilder} (block format, translated so the block is at the origin) and turns the quads written into it into modern ones. The
 * sprite of a quad is the block atlas sprite that contains its uv, as a renderer drawing with a sprite's own uvs wrote it.
 */
final class CustomRenderTypeModels1122 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Models");
    /** The level and position a chunk compile asks about, carried from {@code getModelData} to {@code getQuads}. */
    private static final ModelProperty<Where> WHERE = new ModelProperty<>();

    private record Where(BlockAndTintGetter level, BlockPos pos) {}

    private CustomRenderTypeModels1122() {}

    /** The render type is one a mod added (the four of the game are INVISIBLE, LIQUID, ENTITYBLOCK_ANIMATED and MODEL). */
    static boolean custom(EnumBlockRenderType type) {
        return type != null && type.ordinal() > EnumBlockRenderType.MODEL.ordinal();
    }

    /** Replaces the models of the meta states of every block with a mod-defined render type (after ModelBakeEvent: a mod's own replacement is left). */
    static void wrap(Map<ModelResourceLocation, BakedModel> models) {
        int wrapped = 0;
        BakedModel missing = models.get(net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_VARIANT);
        for (Block block : Registries1122.BLOCKS.getValues()) {
            if (!(block.mcc$modern instanceof Legacy1122Block modern)) continue;
            try {
                boolean[] custom = new boolean[16];
                boolean any = false;
                for (IBlockState state : block.getBlockState().getValidStates()) {
                    if (custom(block.getRenderType(state))) {
                        custom[block.getMetaFromState(state) & 15] = true;
                        any = true;
                    }
                }
                if (!any) continue;
                for (int meta = 0; meta < 16; meta++) {
                    if (!custom[meta]) continue;
                    ModelResourceLocation location = BlockModelShaper.stateToModelLocation(modern.defaultBlockState().setValue(Legacy1122Block.META, meta));
                    BakedModel base = models.get(location);
                    if (base == null) base = missing;
                    if (base == null) continue;
                    models.put(location, new View(base, block));
                    wrapped++;
                }
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("looking for the render type of " + block.getRegistryName(), e);
            }
        }
        if (wrapped > 0) LOG.info("1.12.2 custom render types: {} block model(s) are drawn by the mod's block renderer", wrapped);
    }

    /** A meta state's model that has the legacy block renderer draw the block. */
    static final class View extends BakedModelWrapper<BakedModel> {
        private final Block legacy;

        View(BakedModel base, Block legacy) {
            super(base);
            this.legacy = legacy;
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
            return modelData.derive().with(WHERE, new Where(level, pos.immutable())).build();
        }

        @Override
        public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data,
                RenderType renderType) {
            // everything is one unculled list
            if (side != null || data == null) return List.of();
            Where where = data.get(WHERE);
            if (where == null) return List.of();
            var previous = legacyconnector.v1_12_2.net.minecraftforge.client.MinecraftForgeClient.getRenderLayer();
            legacyconnector.v1_12_2.net.minecraftforge.client.ForgeHooksClient.setRenderLayer(Models1122.layer(renderType));
            try {
                return draw(state, where);
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("drawing " + legacy.getRegistryName() + " with its mod's block renderer", e);
                return List.of();
            } finally {
                legacyconnector.v1_12_2.net.minecraftforge.client.ForgeHooksClient.setRenderLayer(previous);
            }
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
            try {
                IBlockState legacyState = StatesBridge1122.toLegacy(state);
                if (legacyState != null) {
                    List<RenderType> types = new ArrayList<>(4);
                    for (var layer : legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer.values()) {
                        if (!legacy.canRenderInLayer(legacyState, layer)) continue;
                        types.add(switch (layer) {
                            case SOLID -> RenderType.solid();
                            case CUTOUT_MIPPED -> RenderType.cutoutMipped();
                            case CUTOUT -> RenderType.cutout();
                            case TRANSLUCENT -> RenderType.translucent();
                        });
                    }
                    if (!types.isEmpty()) return ChunkRenderTypeSet.of(types.toArray(new RenderType[0]));
                }
            } catch (RuntimeException | LinkageError e) {
                // a layer check that needs a world: solid
            }
            return ChunkRenderTypeSet.of(RenderType.solid());
        }

        private List<net.minecraft.client.renderer.block.model.BakedQuad> draw(BlockState state, Where where) {
            IBlockState legacyState = StatesBridge1122.toLegacy(state);
            var dispatcher = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().getBlockRendererDispatcher();
            if (legacyState == null || dispatcher == null) return List.of();
            BlockPos pos = where.pos();
            var legacyPos = new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(pos.getX(), pos.getY(), pos.getZ());
            BufferBuilder buffer = new BufferBuilder(4096);
            buffer.begin(7, DefaultVertexFormats.BLOCK);
            // the chunk compiler translated its buffer by the chunk origin; the quads of a model are relative to the block
            buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
            try {
                dispatcher.renderBlock(legacyState, legacyPos, WorldBridge1122.access(where.level()), buffer);
            } finally {
                if (buffer.isDrawing) buffer.finishDrawing();
            }
            return quads(buffer);
        }
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> quads(BufferBuilder buffer) {
        VertexFormat format = buffer.getVertexFormat();
        int vertices = buffer.getVertexCount();
        if (format == null || vertices < 4) return List.of();
        int stride = format.getIntegerSize();
        ByteBuffer bytes = buffer.getByteBuffer();
        java.nio.IntBuffer ints = bytes.duplicate().order(ByteOrder.nativeOrder()).position(0).limit(bytes.capacity()).asIntBuffer();
        int pos = format.mcc$offsetOf(VertexFormatElement.EnumUsage.POSITION, 0);
        int uv = format.mcc$offsetOf(VertexFormatElement.EnumUsage.UV, 0);
        List<net.minecraft.client.renderer.block.model.BakedQuad> out = new ArrayList<>(vertices / 4);
        for (int q = 0; q + 3 < vertices; q += 4) {
            int[] data = new int[stride * 4];
            for (int i = 0; i < data.length; i++) data[i] = ints.get(q * stride + i);
            float[] x = new float[4], y = new float[4], z = new float[4];
            float u = 0, v = 0;
            for (int i = 0; i < 4; i++) {
                int base = i * stride;
                x[i] = Float.intBitsToFloat(data[base + pos / 4]);
                y[i] = Float.intBitsToFloat(data[base + pos / 4 + 1]);
                z[i] = Float.intBitsToFloat(data[base + pos / 4 + 2]);
                u += Float.intBitsToFloat(data[base + uv / 4]) / 4;
                v += Float.intBitsToFloat(data[base + uv / 4 + 1]) / 4;
            }
            TextureAtlasSprite sprite = spriteAt(u, v);
            var legacyQuad = new legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad(data, -1, face(x, y, z),
                    Quads1122.legacySprite(sprite), false, format);
            out.add(Quads1122.toModern(legacyQuad));
        }
        return out;
    }

    /** The face a quad looks at, from its first three corners. */
    private static EnumFacing face(float[] x, float[] y, float[] z) {
        float ax = x[1] - x[0], ay = y[1] - y[0], az = z[1] - z[0];
        float bx = x[2] - x[0], by = y[2] - y[0], bz = z[2] - z[0];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0) return EnumFacing.UP;
        return EnumFacing.getFacingFromVector(nx / len, ny / len, nz / len);
    }

    private static volatile TextureAtlas indexedAtlas;
    private static volatile TextureAtlasSprite lastHit;
    private static volatile TextureAtlasSprite[] indexed = new TextureAtlasSprite[0];

    /** The block atlas sprite that contains a uv (the centre of the quad; strictly inside, a sprite's edge texel may belong to its neighbour). */
    private static TextureAtlasSprite spriteAt(float u, float v) {
        TextureAtlas atlas = Sprites1122.blockAtlas();
        if (atlas == null) return null;
        TextureAtlasSprite[] all = indexed;
        if (atlas != indexedAtlas) {
            synchronized (CustomRenderTypeModels1122.class) {
                if (atlas != indexedAtlas) {
                    try {
                        java.lang.reflect.Field f = TextureAtlas.class.getDeclaredField("texturesByName");
                        f.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        Map<net.minecraft.resources.ResourceLocation, TextureAtlasSprite> sprites =
                                (Map<net.minecraft.resources.ResourceLocation, TextureAtlasSprite>) f.get(atlas);
                        indexed = sprites.values().toArray(new TextureAtlasSprite[0]);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        indexed = new TextureAtlasSprite[0];
                    }
                    indexedAtlas = atlas;
                }
                all = indexed;
            }
        }
        TextureAtlasSprite last = lastHit;
        if (last != null && u > last.getU0() && u < last.getU1() && v > last.getV0() && v < last.getV1()) return last;
        TextureAtlasSprite edge = null;
        for (TextureAtlasSprite s : all) {
            if (u > s.getU0() && u < s.getU1() && v > s.getV0() && v < s.getV1()) {
                lastHit = s;
                return s;
            }
            if (edge == null && u >= s.getU0() && u <= s.getU1() && v >= s.getV0() && v <= s.getV1()) edge = s;
        }
        return edge;
    }
}
