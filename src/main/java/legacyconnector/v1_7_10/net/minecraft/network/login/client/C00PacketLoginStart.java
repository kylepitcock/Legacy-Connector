package legacyconnector.v1_7_10.net.minecraft.network.login.client;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The client's first login packet: the name it logs in as. 1.7.10's wire format (the name as a string); the layer does not send it. */
public class C00PacketLoginStart extends Packet {
    private GameProfile profile;

    public C00PacketLoginStart() {
    }

    public C00PacketLoginStart(GameProfile profile) {
        this.profile = profile;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        this.profile = new GameProfile(null, data.readStringFromBuffer(16));
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeStringToBuffer(this.profile.getName());
    }

    public GameProfile func_149304_c() {
        return this.profile;
    }

    @Override
    public boolean hasPriority() {
        return true;
    }
}
