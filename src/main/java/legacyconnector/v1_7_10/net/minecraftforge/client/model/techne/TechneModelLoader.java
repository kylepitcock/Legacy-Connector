package legacyconnector.v1_7_10.net.minecraftforge.client.model.techne;

import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraftforge.client.model.IModelCustom;
import legacyconnector.v1_7_10.net.minecraftforge.client.model.IModelCustomLoader;
import legacyconnector.v1_7_10.net.minecraftforge.client.model.ModelFormatException;

public class TechneModelLoader implements IModelCustomLoader {
    @Override
    public String getType() {
        return "Techne model";
    }

    @Override
    public String[] getSuffixes() {
        return new String[] {"tcn"};
    }

    @Override
    public IModelCustom loadInstance(ResourceLocation resource) throws ModelFormatException {
        return new TechneModel(resource);
    }
}
