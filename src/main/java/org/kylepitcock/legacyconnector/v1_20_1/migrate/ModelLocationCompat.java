package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

/** 1.20.1's ModelResourceLocation is a ResourceLocation; 1.21.1's holds one. The loader inserts {@link #id} where translated code uses it as one. */
public final class ModelLocationCompat {
    private ModelLocationCompat() {}

    public static ResourceLocation id(ModelResourceLocation location) {
        return location == null ? null : location.id();
    }
}
