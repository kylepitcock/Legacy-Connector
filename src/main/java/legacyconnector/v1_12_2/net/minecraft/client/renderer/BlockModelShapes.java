package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.IStateMapper;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.client.Models1122;

/** The model of each block state: the game's model of the modern state. */
public class BlockModelShapes {
    private final ModelManager modelManager;

    public BlockModelShapes(ModelManager manager) {
        modelManager = manager;
    }

    public TextureAtlasSprite getTexture(IBlockState state) {
        return getModelForState(state).getParticleTexture();
    }

    public IBakedModel getModelForState(IBlockState state) {
        boolean metaHolds = StatesBridge1122.metaHolds(state);
        if (!metaHolds) {
            // a state beyond the metadata (a render part a smart model collects): the model of its own variant, as 1.12 baked it
            IBakedModel own = Models1122.stateModel(state);
            if (own != null) return own;
        }
        var shaper = net.minecraft.client.Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        var modern = shaper.getBlockModel(StatesBridge1122.toModern(state));
        IBakedModel model = Models1122.legacy(modern == null ? shaper.getModelManager().getMissingModel() : modern);
        // without a model of its own such a state gets the whole block's model: without its item overrides (see Models1122.withoutOverrides)
        return metaHolds ? model : Models1122.withoutOverrides(model);
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    /** Vanilla re-cached state models here; the game's model manager caches its own. */
    public void reloadModels() {
        Models1122.invalidate();
    }

    public void registerBlockWithStateMapper(Block assoc, IStateMapper stateMapper) {
        legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.setCustomStateMapper(assoc, stateMapper);
    }

    /** Blocks drawn by code only (no model): their states show what their renderers draw. */
    public void registerBuiltInBlocks(Block... builtIns) {
    }

    private final legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.BlockStateMapper blockStateMapper =
            new legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.BlockStateMapper();
    /** Vanilla's state-to-model cache; filled on demand from the game's models (states asked for so far). */
    public final java.util.Map<IBlockState, IBakedModel> bakedModelStore = new java.util.AbstractMap<>() {
        @Override
        public IBakedModel get(Object key) {
            return key instanceof IBlockState state ? getModelForState(state) : null;
        }

        @Override
        public java.util.Set<java.util.Map.Entry<IBlockState, IBakedModel>> entrySet() {
            return java.util.Collections.emptySet();
        }
    };

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.BlockStateMapper getBlockStateMapper() {
        return blockStateMapper;
    }
}
