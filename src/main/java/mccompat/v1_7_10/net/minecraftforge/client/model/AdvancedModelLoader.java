package mccompat.v1_7_10.net.minecraftforge.client.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import mccompat.v1_7_10.net.minecraft.util.ResourceLocation;
import mccompat.v1_7_10.net.minecraftforge.client.model.obj.ObjModelLoader;

/** Forge's model loader registry; the Wavefront OBJ format is built in. */
public class AdvancedModelLoader {
    private static final Map<String, IModelCustomLoader> INSTANCES = new HashMap<>();

    static {
        registerModelHandler(new ObjModelLoader());
    }

    public static void registerModelHandler(IModelCustomLoader modelHandler) {
        for (String suffix : modelHandler.getSuffixes()) INSTANCES.put(suffix, modelHandler);
    }

    public static IModelCustom loadModel(ResourceLocation resource) throws IllegalArgumentException, ModelFormatException {
        String name = resource.getResourcePath();
        int i = name.lastIndexOf('.');
        if (i == -1) throw new IllegalArgumentException("The resource name is not valid: " + name);
        String suffix = name.substring(i + 1);
        IModelCustomLoader loader = INSTANCES.get(suffix);
        if (loader == null) throw new IllegalArgumentException("The resource name is not supported: " + name);
        return loader.loadInstance(resource);
    }

    public static Collection<String> getSupportedSuffixes() {
        return INSTANCES.keySet();
    }
}
