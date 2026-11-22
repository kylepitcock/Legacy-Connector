package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.function.BiFunction;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

/** Bridges the old sprite-getter function used by AlienEvo's static block model baker. */
public final class ModelBakeryCompat {
    private ModelBakeryCompat() {}

    public static BiFunction<ResourceLocation, Material, TextureAtlasSprite> asBiFunction(
            ModelBakery.TextureGetter textureGetter) {
        return (location, material) ->
                textureGetter.get(ModelResourceLocation.standalone(location), material);
    }
}
