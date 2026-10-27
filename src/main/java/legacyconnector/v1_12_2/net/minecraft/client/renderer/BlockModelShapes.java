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
        var shaper = net.minecraft.client.Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        var modern = shaper.getBlockModel(StatesBridge1122.toModern(state));
        return Models1122.legacy(modern == null ? shaper.getModelManager().getMissingModel() : modern);
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
}
