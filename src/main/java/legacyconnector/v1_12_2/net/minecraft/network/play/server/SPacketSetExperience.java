package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** The player's experience bar, total and level. */
public class SPacketSetExperience implements Packet<INetHandlerPlayClient> {
    private float experienceBar;
    private int totalExperience;
    private int level;

    public SPacketSetExperience() {
    }

    public SPacketSetExperience(float experienceBarIn, int totalExperienceIn, int levelIn) {
        this.experienceBar = experienceBarIn;
        this.totalExperience = totalExperienceIn;
        this.level = levelIn;
    }

    public float mcc$experienceBar() {
        return experienceBar;
    }

    public int mcc$totalExperience() {
        return totalExperience;
    }

    public int mcc$level() {
        return level;
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
        return new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(experienceBar, totalExperience, level);
    }
}
