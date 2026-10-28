package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;

/** The player's position, rotation and ground state (sent as the game's move player packets). */
public class CPacketPlayer implements Packet<INetHandlerPlayServer> {
    protected double x;
    protected double y;
    protected double z;
    protected float yaw;
    protected float pitch;
    protected boolean onGround;
    protected boolean moving;
    protected boolean rotating;

    public CPacketPlayer() {
    }

    public CPacketPlayer(boolean onGround) {
        this.onGround = onGround;
    }

    public double getX(double defaultValue) {
        return moving ? x : defaultValue;
    }

    public double getY(double defaultValue) {
        return moving ? y : defaultValue;
    }

    public double getZ(double defaultValue) {
        return moving ? z : defaultValue;
    }

    public float getYaw(float defaultValue) {
        return rotating ? yaw : defaultValue;
    }

    public float getPitch(float defaultValue) {
        return rotating ? pitch : defaultValue;
    }

    public boolean isOnGround() {
        return onGround;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        onGround = buf.readUnsignedByte() != 0;
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeByte(onGround ? 1 : 0);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        if (moving && rotating) return new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, onGround);
        if (moving) return new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(x, y, z, onGround);
        if (rotating) return new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround);
        return new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly(onGround);
    }

    public static class Position extends CPacketPlayer {
        public Position() {
            this.moving = true;
        }

        public Position(double x, double y, double z, boolean onGround) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.onGround = onGround;
            this.moving = true;
        }

        @Override
        public void readPacketData(PacketBuffer buf) throws IOException {
            x = buf.readDouble();
            y = buf.readDouble();
            z = buf.readDouble();
            super.readPacketData(buf);
        }

        @Override
        public void writePacketData(PacketBuffer buf) throws IOException {
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            super.writePacketData(buf);
        }
    }

    public static class Rotation extends CPacketPlayer {
        public Rotation() {
            this.rotating = true;
        }

        public Rotation(float yaw, float pitch, boolean onGround) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
            this.rotating = true;
        }

        @Override
        public void readPacketData(PacketBuffer buf) throws IOException {
            yaw = buf.readFloat();
            pitch = buf.readFloat();
            super.readPacketData(buf);
        }

        @Override
        public void writePacketData(PacketBuffer buf) throws IOException {
            buf.writeFloat(yaw);
            buf.writeFloat(pitch);
            super.writePacketData(buf);
        }
    }

    public static class PositionRotation extends CPacketPlayer {
        public PositionRotation() {
            this.moving = true;
            this.rotating = true;
        }

        public PositionRotation(double x, double y, double z, float yaw, float pitch, boolean onGround) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
            this.moving = true;
            this.rotating = true;
        }

        @Override
        public void readPacketData(PacketBuffer buf) throws IOException {
            x = buf.readDouble();
            y = buf.readDouble();
            z = buf.readDouble();
            yaw = buf.readFloat();
            pitch = buf.readFloat();
            super.readPacketData(buf);
        }

        @Override
        public void writePacketData(PacketBuffer buf) throws IOException {
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(yaw);
            buf.writeFloat(pitch);
            super.writePacketData(buf);
        }
    }
}
