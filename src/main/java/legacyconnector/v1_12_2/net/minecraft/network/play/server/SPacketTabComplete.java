package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** The completions the server offers for a half-typed chat command. */
public class SPacketTabComplete implements Packet<INetHandlerPlayClient> {
    private String[] matches;

    public SPacketTabComplete() {
    }

    public SPacketTabComplete(String[] matchesIn) {
        this.matches = matchesIn;
    }

    public String[] getMatches() {
        return matches;
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
}
