package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BlockModelShapes;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap;
import org.kylepitcock.legacyconnector.v1_12_2.client.Models1122;

/** The baked models of the game, looked up by legacy location (see {@link Models1122#modernLocations}). */
public class ModelManager {
    private static final ModelManager INSTANCE = new ModelManager(TextureMap.mcc$blocks());
    private final TextureMap texMap;
    private final BlockModelShapes modelProvider = new BlockModelShapes(this);

    public ModelManager(TextureMap textures) {
        texMap = textures;
    }

    public static ModelManager mcc$instance() {
        return INSTANCE;
    }

    public IBakedModel getModel(ModelResourceLocation modelLocation) {
        if (modelLocation == null) return getMissingModel();
        return Models1122.legacyModel(modelLocation);
    }

    public IBakedModel getMissingModel() {
        return Models1122.legacy(net.minecraft.client.Minecraft.getInstance().getModelManager().getMissingModel());
    }

    public TextureMap getTextureMap() {
        return texMap;
    }

    public BlockModelShapes getBlockModelShapes() {
        return modelProvider;
    }
}
