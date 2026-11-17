package legacyconnector.v1_6_4.net.minecraft.src;

import io.netty.buffer.ByteBuf;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;

/**
 * ModLoaderMP's {@code ISpawnable}: an entity that sends its own spawn data. The 1.2.5 client build declared {@code void spawn(Packet230ModLoader)} and the server
 * build {@code Packet230ModLoader getSpawnPacket()} (checked in both ModLoaderMP zips), so a mod's class has the one its build needs; both have a default here.
 *
 * <p>The layer carries the data in the entity's native spawn data ({@link IEntityAdditionalSpawnData}): the server half's packet is encoded with the
 * type id of its {@code registerEntityTrackerEntry}, the client half decodes it, looks the type up in {@code registerNetClientHandlerEntity}'s table and
 * calls {@code spawn} on the new entity (see {@link ModLoaderMp#mcc$writeSpawn} / {@link ModLoaderMp#mcc$readSpawn}). A build without {@code getSpawnPacket}
 * (a client build in single player, where nothing was ever sent) sends nothing.
 */
public interface ISpawnable extends IEntityAdditionalSpawnData {
    /** Client half: the entity was created and now gets the server's data (position, velocity, the mod's own fields). */
    default void spawn(Packet230ModLoader packet) {}

    /** Server half: the packet describing this entity to a client; null sends no data. */
    default Packet230ModLoader getSpawnPacket() {
        return null;
    }

    @Override
    default void writeSpawnData(ByteBuf buffer) {
        ModLoaderMp.mcc$writeSpawn(this, buffer);
    }

    @Override
    default void readSpawnData(ByteBuf data) {
        ModLoaderMp.mcc$readSpawn(this, data);
    }
}
