package legacyconnector.v1_6_4.facade.net.minecraftforge.client.model;

import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraftforge.client.model.IModelCustom;

/**
 * Facade of the 1.7.10 shim's {@code AdvancedModelLoader} for 1.6.4 mods, which loaded models by classpath path
 * ({@code "/assets/<mod>/models/x.obj"}, older ones {@code "/mods/<mod>/models/x.obj"}); 1.7.10 takes a {@code ResourceLocation}.
 * See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class AdvancedModelLoader {
    private AdvancedModelLoader() {}

    public static IModelCustom loadModel(String path) {
        return legacyconnector.v1_7_10.net.minecraftforge.client.model.AdvancedModelLoader.loadModel(location(path));
    }

    /** The resource location of a 1.6.4 classpath resource path. */
    public static ResourceLocation location(String path) {
        String p = path.startsWith("/") ? path.substring(1) : path;
        int slash = p.indexOf('/');
        if (slash > 0 && (p.startsWith("assets/") || p.startsWith("mods/"))) {
            int next = p.indexOf('/', slash + 1);
            if (next > 0) return new ResourceLocation(p.substring(slash + 1, next), p.substring(next + 1));
        }
        return new ResourceLocation("minecraft", p);
    }
}
