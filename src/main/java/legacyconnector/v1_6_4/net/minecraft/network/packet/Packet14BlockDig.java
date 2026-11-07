package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/**
 * 1.6.4's digging packet, sent by the client: {@code status} 0 start digging, 1 abort, 2 finish, 3 drop the item stack, 4 drop one item, 5 release the
 * use key; {@code face} is the clicked side (0 down, 1 up, 2 north, 3 south, 4 west, 5 east). Sent as the modern player-action packet.
 */
public class Packet14BlockDig extends Packet {
    public int xPosition;
    public int yPosition;
    public int zPosition;
    public int face;
    public int status;

    public Packet14BlockDig() {}

    public Packet14BlockDig(int status, int x, int y, int z, int face) {
        this.status = status;
        this.xPosition = x;
        this.yPosition = y;
        this.zPosition = z;
        this.face = face;
    }

    public int getXPosition() {
        return xPosition;
    }

    public int getYPosition() {
        return yPosition;
    }

    public int getZPosition() {
        return zPosition;
    }

    public int getFace() {
        return face;
    }

    public int getStatus() {
        return status;
    }

    /** The name of the modern {@code ServerboundPlayerActionPacket.Action} of a legacy status, null for none (5 is a no-op on this layer). */
    public static String actionName(int status) {
        return switch (status) {
            case 0 -> "START_DESTROY_BLOCK";
            case 1 -> "ABORT_DESTROY_BLOCK";
            case 2 -> "STOP_DESTROY_BLOCK";
            case 3 -> "DROP_ALL_ITEMS";
            case 4 -> "DROP_ITEM";
            case 5 -> "RELEASE_USE_ITEM";
            default -> null;
        };
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        status = in.readUnsignedByte();
        xPosition = in.readInt();
        yPosition = in.readUnsignedByte();
        zPosition = in.readInt();
        face = in.readUnsignedByte();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.write(status);
        out.writeInt(xPosition);
        out.write(yPosition);
        out.writeInt(zPosition);
        out.write(face);
    }

    @Override
    public int getPacketSize() {
        return 11;
    }

    @Override
    public void mcc$sendToServer() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        String name = actionName(status);
        if (name == null || mc.getConnection() == null) return;
        var action = net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.valueOf(name);
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(action,
                new net.minecraft.core.BlockPos(xPosition, yPosition, zPosition), net.minecraft.core.Direction.from3DDataValue(Math.min(5, face)), 0));
    }
}
