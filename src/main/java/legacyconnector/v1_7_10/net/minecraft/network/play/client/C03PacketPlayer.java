package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The client player's movement (position and/or look); sent as the modern movement packet. */
public class C03PacketPlayer extends Packet {
    public double x;
    public double y;
    public double z;
    public double stance;
    public float yaw;
    public float pitch;
    public boolean field_149474_g;
    public boolean field_149480_h;
    public boolean rotating;

    public C03PacketPlayer() {
    }

    public C03PacketPlayer(boolean onGround) {
        this.field_149474_g = onGround;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149474_g = data.readUnsignedByte() != 0;
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(field_149474_g ? 1 : 0);
    }

    public double getPositionX() {
        return x;
    }

    public double getPositionY() {
        return y;
    }

    public double getPositionZ() {
        return z;
    }

    public double getStance() {
        return stance;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean func_149465_i() {
        return field_149474_g;
    }

    public boolean func_149466_j() {
        return field_149480_h;
    }

    public boolean getRotating() {
        return rotating;
    }

    public void func_149469_a(boolean moving) {
        this.field_149480_h = moving;
    }

    @Override
    public void mcc$sendToServer() {
        net.minecraft.network.protocol.Packet<?> modern;
        if (field_149480_h && rotating) {
            modern = new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, field_149474_g);
        } else if (field_149480_h) {
            modern = new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(x, y, z, field_149474_g);
        } else if (rotating) {
            modern = new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(yaw, pitch, field_149474_g);
        } else {
            modern = new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly(field_149474_g);
        }
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientNetwork1710.send(modern);
    }

    /** Position only (y is the feet, the stance the eyes). */
    public static class C04PacketPlayerPosition extends C03PacketPlayer {
        public C04PacketPlayerPosition() {
            this.field_149480_h = true;
        }

        public C04PacketPlayerPosition(double x, double y, double stance, double z, boolean onGround) {
            this.x = x;
            this.y = y;
            this.stance = stance;
            this.z = z;
            this.field_149474_g = onGround;
            this.field_149480_h = true;
        }

        @Override
        public void readPacketData(PacketBuffer data) throws IOException {
            x = data.readDouble();
            y = data.readDouble();
            stance = data.readDouble();
            z = data.readDouble();
            super.readPacketData(data);
        }

        @Override
        public void writePacketData(PacketBuffer data) throws IOException {
            data.writeDouble(x);
            data.writeDouble(y);
            data.writeDouble(stance);
            data.writeDouble(z);
            super.writePacketData(data);
        }
    }

    /** Look only. */
    public static class C05PacketPlayerLook extends C03PacketPlayer {
        public C05PacketPlayerLook() {
            this.rotating = true;
        }

        public C05PacketPlayerLook(float yaw, float pitch, boolean onGround) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.field_149474_g = onGround;
            this.rotating = true;
        }

        @Override
        public void readPacketData(PacketBuffer data) throws IOException {
            yaw = data.readFloat();
            pitch = data.readFloat();
            super.readPacketData(data);
        }

        @Override
        public void writePacketData(PacketBuffer data) throws IOException {
            data.writeFloat(yaw);
            data.writeFloat(pitch);
            super.writePacketData(data);
        }
    }

    /** Position and look. */
    public static class C06PacketPlayerPosLook extends C03PacketPlayer {
        public C06PacketPlayerPosLook() {
            this.field_149480_h = true;
            this.rotating = true;
        }

        public C06PacketPlayerPosLook(double x, double y, double stance, double z, float yaw, float pitch, boolean onGround) {
            this.x = x;
            this.y = y;
            this.stance = stance;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.field_149474_g = onGround;
            this.rotating = true;
            this.field_149480_h = true;
        }

        @Override
        public void readPacketData(PacketBuffer data) throws IOException {
            x = data.readDouble();
            y = data.readDouble();
            stance = data.readDouble();
            z = data.readDouble();
            yaw = data.readFloat();
            pitch = data.readFloat();
            super.readPacketData(data);
        }

        @Override
        public void writePacketData(PacketBuffer data) throws IOException {
            data.writeDouble(x);
            data.writeDouble(y);
            data.writeDouble(stance);
            data.writeDouble(z);
            data.writeFloat(yaw);
            data.writeFloat(pitch);
            super.writePacketData(data);
        }
    }
}
