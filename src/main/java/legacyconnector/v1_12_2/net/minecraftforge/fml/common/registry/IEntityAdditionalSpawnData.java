package legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry;

import io.netty.buffer.ByteBuf;

/** Extra data a mod entity sends to clients when it spawns there (written on the server, read into the new client entity). */
public interface IEntityAdditionalSpawnData {
    void writeSpawnData(ByteBuf buffer);

    void readSpawnData(ByteBuf additionalData);
}
