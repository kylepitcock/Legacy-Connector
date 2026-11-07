package legacyconnector.v1_20_1.net.minecraftforge.entity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/** Forge 1.20.1's extra spawn data hook; NeoForge 1.21.1 ({@code IEntityWithComplexSpawn}) passes a {@code RegistryFriendlyByteBuf}, a subclass of the old buffer type. */
public interface IEntityAdditionalSpawnData extends IEntityWithComplexSpawn {
    void writeSpawnData(FriendlyByteBuf buffer);

    void readSpawnData(FriendlyByteBuf additionalData);

    @Override
    default void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        writeSpawnData((FriendlyByteBuf) buffer);
    }

    @Override
    default void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        readSpawnData((FriendlyByteBuf) additionalData);
    }
}
