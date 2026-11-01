package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
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
import net.neoforged.neoforge.common.util.TriState;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;

/**
 * Block models chosen by the legacy actual state. 1.12 drew a block with the variant of {@code getActualState}: stairs corners
 * (SHAPE), fence, wall and pane connections, door halves are not in the metadata but worked out from the neighbours each time the chunk
 * was drawn. Here a block state is one modern {@code meta} state, whose blockstate variant is the metadata's state (straight stairs, a
 * bare fence post). For a block that overrides getActualState, the model of each {@code meta} state is wrapped: when the chunk is
 * compiled it asks the legacy block for the actual state at the position and draws the model 1.12 had for that state (the standalone
 * models of states beyond the metadata, Assets1122.stateModel, or the metadata model of another meta), passed on as model data.
 */
final class ActualStateModels1122 {
    private ActualStateModels1122() {}

    /** The model a position draws, when it is not the model of its stored state. */
    private static final ModelProperty<BakedModel> CHOSEN = new ModelProperty<>();

    /** Wraps the block models of legacy blocks with actual states in the baking result (after ModelBakeEvent: mods' replacements stay theirs). */
    static void wrap(Map<ModelResourceLocation, BakedModel> models) {
        Map<IBlockState, ModelResourceLocation> own = Models1122.stateLocations();
        BakedModel missing = models.get(ModelBakery.MISSING_MODEL_VARIANT);
        Class<?> S = IBlockState.class, A = legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess.class,
                P = legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.class;
        int wrapped = 0;
        for (Block block : Registries1122.BLOCKS.getValues()) {
            if (!(block.mcc$modern instanceof Legacy1122Block modern)) continue;
            try {
                if (!Legacy1122Block.overrides(block, "getActualState", S, A, P)) continue;
                // the models of the metadata states, as baked from the modern blockstate file
                BakedModel[] byMeta = new BakedModel[16];
                boolean custom = false;
                for (int meta = 0; meta < 16; meta++) {
                    BakedModel m = models.get(BlockModelShaper.stateToModelLocation(modern.defaultBlockState().setValue(Legacy1122Block.META, meta)));
                    if (m instanceof Models1122.ModernView || m instanceof View) custom = true; // a mod model: it is asked with the actual state itself
                    byMeta[meta] = m;
                }
                if (custom) continue;
                // every legacy state: its own model if it has one, else its metadata's model (1.12 had a variant for each)
                Map<IBlockState, BakedModel> byState = new HashMap<>();
                boolean differs = false;
                for (IBlockState state : block.getBlockState().getValidStates()) {
                    BakedModel m = null;
                    ModelResourceLocation location = own.get(state);
                    if (location != null) {
                        m = models.get(location);
                        if (m == missing) m = null;
                    }
                    if (m != null) differs = true;
                    else m = byMeta[block.getMetaFromState(state) & 15];
                    if (m != null) byState.put(state, m);
                }
                if (!differs) continue;
                for (int meta = 0; meta < 16; meta++) {
                    if (byMeta[meta] == null) continue;
                    ModelResourceLocation location = BlockModelShaper.stateToModelLocation(modern.defaultBlockState().setValue(Legacy1122Block.META, meta));
                    models.put(location, new View(byMeta[meta], block, byState));
                    wrapped++;
                }
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("choosing the actual-state models of " + block.getRegistryName(), e);
            }
        }
        if (wrapped > 0) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Models").info("1.12.2 actual-state models: {} block model(s) draw their neighbours' shape", wrapped);
    }

    /** A metadata state's model that draws the model of the block's actual state at the position. */
    static final class View extends BakedModelWrapper<BakedModel> {
        private final Block legacy;
        private final Map<IBlockState, BakedModel> byState;

        View(BakedModel base, Block legacy, Map<IBlockState, BakedModel> byState) {
            super(base);
            this.legacy = legacy;
            this.byState = byState;
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
            BakedModel chosen = null;
            try {
                IBlockState stored = StatesBridge1122.toLegacy(state);
                if (stored != null) {
                    IBlockState actual = legacy.getActualState(stored, WorldBridge1122.access(level),
                            new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
                    if (actual != null && actual != stored) chosen = byState.get(actual);
                }
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("asking a legacy block for its actual state", e);
            }
            if (chosen == null || chosen == originalModel) return originalModel.getModelData(level, pos, state, modelData);
            ModelData data = chosen.getModelData(level, pos, state, modelData);
            return data.derive().with(CHOSEN, chosen).build();
        }

        private BakedModel model(ModelData data) {
            BakedModel chosen = data == null ? null : data.get(CHOSEN);
            return chosen != null ? chosen : originalModel;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType) {
            return model(extraData).getQuads(state, side, rand, extraData, renderType);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
            return model(data).getRenderTypes(state, rand, data);
        }

        @Override
        public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
            return model(data).useAmbientOcclusion(state, data, renderType);
        }

        @Override
        public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon(ModelData data) {
            return model(data).getParticleIcon(data);
        }
    }
}
