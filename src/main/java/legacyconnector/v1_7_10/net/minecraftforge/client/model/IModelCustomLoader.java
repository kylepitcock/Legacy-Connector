package legacyconnector.v1_7_10.net.minecraftforge.client.model;

import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

public interface IModelCustomLoader {
    String getType();

    String[] getSuffixes();

    IModelCustom loadInstance(ResourceLocation resource) throws ModelFormatException;
}
