package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** The player's health, food and saturation. */
public class SPacketUpdateHealth implements Packet<INetHandlerPlayClient> {
    private float health;
    private int foodLevel;
    private float saturationLevel;

    public SPacketUpdateHealth() {
    }

    public SPacketUpdateHealth(float healthIn, int foodLevelIn, float saturationLevelIn) {
        this.health = healthIn;
        this.foodLevel = foodLevelIn;
        this.saturationLevel = saturationLevelIn;
    }

    public float mcc$health() {
        return health;
    }

    public int mcc$foodLevel() {
        return foodLevel;
    }

    public float mcc$saturationLevel() {
        return saturationLevel;
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

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(health, foodLevel, saturationLevel);
    }
}
