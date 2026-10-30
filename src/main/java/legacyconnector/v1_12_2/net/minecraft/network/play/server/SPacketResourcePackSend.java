package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** Offers the client a server resource pack (an optional one, as 1.12's were); sent as the modern resource pack push. */
public class SPacketResourcePackSend implements Packet<INetHandlerPlayClient> {
    private String url;
    private String hash;

    public SPacketResourcePackSend() {
    }

    public SPacketResourcePackSend(String urlIn, String hashIn) {
        this.url = urlIn;
        this.hash = hashIn;
        if (hashIn != null && hashIn.length() > 40) throw new IllegalArgumentException("Hash is too long (max 40, was " + hashIn.length() + ")");
    }

    public String getURL() {
        return url;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packet
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        if (url == null) return null;
        var id = java.util.UUID.nameUUIDFromBytes(url.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket(id, url, hash == null ? "" : hash, false, java.util.Optional.empty());
    }
}
