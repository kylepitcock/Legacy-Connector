package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** An entity's motion (in 1/8000 blocks per tick, as 1.12 sent it). */
public class SPacketEntityVelocity implements Packet<INetHandlerPlayClient> {
    private int entityID;
    private int motionX;
    private int motionY;
    private int motionZ;

    public SPacketEntityVelocity() {
    }

    public SPacketEntityVelocity(legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn) {
        this(entityIn.getEntityId(), motion(entityIn).x, motion(entityIn).y, motion(entityIn).z);
    }

    private static net.minecraft.world.phys.Vec3 motion(legacyconnector.v1_12_2.net.minecraft.entity.Entity e) {
        var m = e.mcc$modern();
        return m != null ? m.getDeltaMovement() : new net.minecraft.world.phys.Vec3(e.motionX, e.motionY, e.motionZ);
    }

    public SPacketEntityVelocity(int entityIdIn, double motionXIn, double motionYIn, double motionZIn) {
        this.entityID = entityIdIn;
        this.motionX = (int) (Math.max(-3.9D, Math.min(3.9D, motionXIn)) * 8000.0D);
        this.motionY = (int) (Math.max(-3.9D, Math.min(3.9D, motionYIn)) * 8000.0D);
        this.motionZ = (int) (Math.max(-3.9D, Math.min(3.9D, motionZIn)) * 8000.0D);
    }

    public int getEntityID() {
        return entityID;
    }

    public int getMotionX() {
        return motionX;
    }

    public int getMotionY() {
        return motionY;
    }

    public int getMotionZ() {
        return motionZ;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        entityID = buf.readVarInt();
        motionX = buf.readShort();
        motionY = buf.readShort();
        motionZ = buf.readShort();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(entityID);
        buf.writeShort(motionX);
        buf.writeShort(motionY);
        buf.writeShort(motionZ);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entityID,
                new net.minecraft.world.phys.Vec3(motionX / 8000.0D, motionY / 8000.0D, motionZ / 8000.0D));
    }
}
