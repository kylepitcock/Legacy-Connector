package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The player's health, food level and saturation. */
public class S06PacketUpdateHealth extends Packet {
    public float health;
    public int foodLevel;
    public float saturationLevel;

    public S06PacketUpdateHealth() {
    }

    public S06PacketUpdateHealth(float health, int foodLevel, float saturationLevel) {
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturationLevel = saturationLevel;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        health = data.readFloat();
        foodLevel = data.readShort();
        saturationLevel = data.readFloat();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeFloat(health);
        data.writeShort(foodLevel);
        data.writeFloat(saturationLevel);
    }

    public float getHealth() {
        return health;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public float getSaturationLevel() {
        return saturationLevel;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(health, foodLevel, saturationLevel));
    }
}
