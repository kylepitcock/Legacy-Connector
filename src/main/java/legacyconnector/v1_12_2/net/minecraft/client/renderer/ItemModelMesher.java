package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.client.Models1122;

/** Item models: registration goes to Forge's ModelLoader (the generated item models); lookups answer the game's model of the stack. */
public class ItemModelMesher {
    private final ModelManager modelManager;

    public ItemModelMesher(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    public TextureAtlasSprite getParticleIcon(Item item) {
        return getParticleIcon(item, 0);
    }

    public TextureAtlasSprite getParticleIcon(Item item, int meta) {
        return getItemModel(new ItemStack(item, 1, meta)).getParticleTexture();
    }

    public IBakedModel getItemModel(ItemStack stack) {
        var renderer = net.minecraft.client.Minecraft.getInstance().getItemRenderer();
        var modern = renderer.getModel(Stacks1122.toModern(stack), null, null, 0);
        return Models1122.legacy(modern);
    }

    protected int getMetadata(ItemStack stack) {
        return stack.getMaxDamage() > 0 ? 0 : stack.getMetadata();
    }

    public void register(Item item, int meta, ModelResourceLocation location) {
        legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(item, meta, location);
    }

    public void register(Item item, ItemMeshDefinition definition) {
        legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.setCustomMeshDefinition(item, definition);
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    public void rebuildCache() {
        Models1122.invalidate();
    }

    public void registerBlockWithStateMapper(Block block, legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.IStateMapper mapper) {
        legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.setCustomStateMapper(block, mapper);
    }
}
