package legacyconnector.v1_6_4.net.minecraft.network.packet;

/** 1.6.4's position-and-look movement packet. */
public class Packet13PlayerLookMove extends Packet10Flying {
    public Packet13PlayerLookMove() {
        this.moving = true;
        this.rotating = true;
    }

    public Packet13PlayerLookMove(double x, double y, double stance, double z, float yaw, float pitch, boolean onGround) {
        this.xPosition = x;
        this.yPosition = y;
        this.stance = stance;
        this.zPosition = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.moving = true;
        this.rotating = true;
    }
}
