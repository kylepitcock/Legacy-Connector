package legacyconnector.v1_12_2.net.minecraftforge.registries;

import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializer;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;

/**
 * A mod's entity data serializer as a registry entry. The layer has no separate serializer registry: the serializer gets its id
 * ({@code DataSerializers}) when the entry is made, which is before any entity uses it.
 */
public final class DataSerializerEntry extends IForgeRegistryEntry.Impl<DataSerializerEntry> {
    private final DataSerializer<?> serializer;

    public DataSerializerEntry(DataSerializer<?> serializer) {
        this.serializer = serializer;
        DataSerializers.registerSerializer(serializer);
    }

    public DataSerializer<?> getSerializer() {
        return serializer;
    }
}
