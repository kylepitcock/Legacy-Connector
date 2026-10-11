package mccompat.v1_7_10.net.minecraftforge.client.model.obj;

import mccompat.v1_7_10.net.minecraft.util.ResourceLocation;
import mccompat.v1_7_10.net.minecraftforge.client.model.IModelCustom;
import mccompat.v1_7_10.net.minecraftforge.client.model.IModelCustomLoader;
import mccompat.v1_7_10.net.minecraftforge.client.model.ModelFormatException;

public class ObjModelLoader implements IModelCustomLoader {
    @Override
    public String getType() {
        return "OBJ model";
    }

    @Override
    public String[] getSuffixes() {
        return new String[] {"obj"};
    }

    @Override
    public IModelCustom loadInstance(ResourceLocation resource) throws ModelFormatException {
        return new WavefrontObject(resource);
    }
}
