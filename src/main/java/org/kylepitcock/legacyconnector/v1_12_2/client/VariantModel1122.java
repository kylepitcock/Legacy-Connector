package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

/**
 * A blockstate variant as a standalone model ({@code "loader": "legacyconnector:variant1122", "model", "x", "y", "uvlock"}): the
 * model baked with the variant's rotation and uvlock exactly as the game bakes a blockstate variant. The models of 1.12 states beyond
 * their block's metadata (stairs corners: the shape comes from getActualState) cannot be variants of the modern blockstate file, and a
 * root transform turns the geometry but not the texture coordinates, so uvlocked variants (most stairs) drew with turned textures.
 */
public record VariantModel1122(ResourceLocation model, int x, int y, boolean uvlock) implements IUnbakedGeometry<VariantModel1122> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("legacyconnector", "variant1122");

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState,
            ItemOverrides overrides) {
        BlockModelRotation rotation = BlockModelRotation.by(x, y);
        if (rotation == null) rotation = BlockModelRotation.X0_Y0;
        return baker.bake(model, new Variant(model, rotation.getRotation(), uvlock, 1), spriteGetter);
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        UnbakedModel unbaked = modelGetter.apply(model);
        if (unbaked != null) unbaked.resolveParents(modelGetter);
    }

    public static final class Loader implements IGeometryLoader<VariantModel1122> {
        public static final Loader INSTANCE = new Loader();

        private Loader() {}

        @Override
        public VariantModel1122 read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
            if (!json.has("model")) throw new JsonParseException("variant1122 model without a \"model\"");
            ResourceLocation model = ResourceLocation.parse(json.get("model").getAsString());
            int x = json.has("x") ? json.get("x").getAsInt() : 0;
            int y = json.has("y") ? json.get("y").getAsInt() : 0;
            boolean uvlock = json.has("uvlock") && json.get("uvlock").getAsBoolean();
            return new VariantModel1122(model, x, y, uvlock);
        }
    }
}
