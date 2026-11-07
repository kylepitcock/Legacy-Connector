package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's experience packet: bar progress, total points and level. */
public class Packet43Experience extends Packet {
    public float experience;
    public int experienceTotal;
    public int experienceLevel;

    public Packet43Experience() {}

    public Packet43Experience(float experience, int total, int level) {
        this.experience = experience;
        this.experienceTotal = total;
        this.experienceLevel = level;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        experience = in.readFloat();
        experienceLevel = in.readShort();
        experienceTotal = in.readShort();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeFloat(experience);
        out.writeShort(experienceLevel);
        out.writeShort(experienceTotal);
    }

    @Override
    public int getPacketSize() {
        return 4;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(experience, experienceTotal, experienceLevel));
    }
}
