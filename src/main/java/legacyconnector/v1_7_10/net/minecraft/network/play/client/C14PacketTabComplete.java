package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The client asks the server to complete the chat text typed so far. */
public class C14PacketTabComplete extends Packet {
    private String text = "";

    public C14PacketTabComplete() {
    }

    public C14PacketTabComplete(String text) {
        this.text = text;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        text = data.readStringFromBuffer(32767);
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeStringToBuffer(text.length() > 32767 ? text.substring(0, 32767) : text);
    }

    public String func_149419_c() {
        return text;
    }

    @Override
    public void mcc$sendToServer() {
        // the modern suggestion request carries a transaction id the chat screen answers by; id 0 is not used by it
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientNetwork1710.send(
                new net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket(0, text.startsWith("/") ? text.substring(1) : text));
    }
}
