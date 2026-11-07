package legacyconnector.v1_6_4.net.minecraft.network.packet;

/** 1.6.4's look-only movement packet. */
public class Packet12PlayerLook extends Packet10Flying {
    public Packet12PlayerLook() {
        this.rotating = true;
    }

    public Packet12PlayerLook(float yaw, float pitch, boolean onGround) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.rotating = true;
    }
}
