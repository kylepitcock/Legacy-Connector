package legacyconnector.v1_12_2.net.minecraft.network.datasync;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;

/** How a type of synchronized entity data is written, read and copied. */
public interface DataSerializer<T> {
    void write(PacketBuffer buf, T value);

    T read(PacketBuffer buf) throws IOException;

    DataParameter<T> createKey(int id);

    T copyValue(T value);
}
