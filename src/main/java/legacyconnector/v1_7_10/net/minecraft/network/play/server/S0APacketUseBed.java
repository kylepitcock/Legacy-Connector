package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * A player lies down in a bed. Modern clients learn that from the player's synchronized pose and bed position, which
 * the server sets when the player sleeps; the packet itself carries data only.
 */
public class S0APacketUseBed extends Packet {
    public int playerID;
    public int x;
    public int y;
    public int z;

    public S0APacketUseBed() {
    }

    public S0APacketUseBed(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, int x, int y, int z) {
        this.playerID = player.getEntityId();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        playerID = data.readInt();
        x = data.readInt();
        y = data.readByte();
        z = data.readInt();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(playerID);
        data.writeInt(x);
        data.writeByte(y);
        data.writeInt(z);
    }

    public legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer getPlayer(legacyconnector.v1_7_10.net.minecraft.world.World world) {
        return world.getEntityByID(playerID) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer p ? p : null;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
