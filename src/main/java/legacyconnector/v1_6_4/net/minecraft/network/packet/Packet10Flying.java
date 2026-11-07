package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/** 1.6.4's player movement packet (position and/or look, sent by the client every tick); the subclasses are 11, 12 and 13. */
public class Packet10Flying extends Packet {
    public double xPosition;
    public double yPosition;
    public double zPosition;
    public double stance;
    public float yaw;
    public float pitch;
    public boolean onGround;
    public boolean moving;
    public boolean rotating;

    public Packet10Flying() {}

    public Packet10Flying(boolean onGround) {
        this.onGround = onGround;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        if (moving) {
            xPosition = in.readDouble();
            yPosition = in.readDouble();
            stance = in.readDouble();
            zPosition = in.readDouble();
        }
        if (rotating) {
            yaw = in.readFloat();
            pitch = in.readFloat();
        }
        onGround = in.readUnsignedByte() != 0;
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        if (moving) {
            out.writeDouble(xPosition);
            out.writeDouble(yPosition);
            out.writeDouble(stance);
            out.writeDouble(zPosition);
        }
        if (rotating) {
            out.writeFloat(yaw);
            out.writeFloat(pitch);
        }
        out.writeByte(onGround ? 1 : 0);
    }

    @Override
    public int getPacketSize() {
        return 1 + (moving ? 32 : 0) + (rotating ? 8 : 0);
    }

    @Override
    public void mcc$sendToServer() {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection == null) return;
        if (moving && rotating) {
            connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(xPosition, yPosition, zPosition, yaw, pitch, onGround));
        } else if (moving) {
            connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(xPosition, yPosition, zPosition, onGround));
        } else if (rotating) {
            connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround));
        } else {
            connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly(onGround));
        }
    }
}
