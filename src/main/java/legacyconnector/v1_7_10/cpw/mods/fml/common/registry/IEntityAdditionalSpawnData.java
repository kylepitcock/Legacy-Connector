package legacyconnector.v1_7_10.cpw.mods.fml.common.registry;

import io.netty.buffer.ByteBuf;

/** Extra data a legacy entity sends with its spawn packet. */
public interface IEntityAdditionalSpawnData {
    void writeSpawnData(ByteBuf buffer);

    void readSpawnData(ByteBuf additionalData);
}
