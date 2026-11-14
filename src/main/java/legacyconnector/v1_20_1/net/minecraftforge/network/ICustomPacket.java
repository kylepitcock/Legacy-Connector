package legacyconnector.v1_20_1.net.minecraftforge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

/**
 * Forge 1.20.1's marker of a vanilla custom payload packet that carries a channel name, an index and its data (mods mix into it or read it for their own
 * network code). NeoForge 1.21.1 has typed payloads instead: {@code NetworkDirection.buildPacket} returns an implementation that holds the real packet
 * ({@link #getThis}); the defaults answer "no data" for classes a mod implements itself.
 */
public interface ICustomPacket<T extends Packet<?>> {
    default FriendlyByteBuf getInternalData() {
        return null;
    }

    default int getIndex() {
        return 0;
    }

    default ResourceLocation getName() {
        return null;
    }

    /** The vanilla packet to send (Forge: the packet itself). */
    @SuppressWarnings("unchecked")
    default T getThis() {
        return (T) this;
    }
}
