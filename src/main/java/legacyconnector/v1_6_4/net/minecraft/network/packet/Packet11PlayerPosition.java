package legacyconnector.v1_6_4.net.minecraft.network.packet;

/** 1.6.4's position-only movement packet. */
public class Packet11PlayerPosition extends Packet10Flying {
    public Packet11PlayerPosition() {
        this.moving = true;
    }

    public Packet11PlayerPosition(double x, double y, double stance, double z, boolean onGround) {
        this.xPosition = x;
        this.yPosition = y;
        this.stance = stance;
        this.zPosition = z;
        this.onGround = onGround;
        this.moving = true;
    }
}
