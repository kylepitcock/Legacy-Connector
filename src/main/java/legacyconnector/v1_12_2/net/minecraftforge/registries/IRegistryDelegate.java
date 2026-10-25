package legacyconnector.v1_12_2.net.minecraftforge.registries;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

public interface IRegistryDelegate<T> {
    T get();

    ResourceLocation name();

    Class<T> type();
}
