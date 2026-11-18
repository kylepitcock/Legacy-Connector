package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** Makes the client look through an entity's eyes (the spectator view). */
public class SPacketCamera implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private net.minecraft.world.entity.Entity target;

    public SPacketCamera() {
    }

    public SPacketCamera(legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn) {
        this.entityId = entityIn.getEntityId();
        this.target = entityIn.mcc$entity;
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return target == null ? null : new net.minecraft.network.protocol.game.ClientboundSetCameraPacket(target);
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packet
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }
}
