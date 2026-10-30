package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** Moves the client's player: a position and rotation, each coordinate absolute or (in {@code flags}) relative. */
public class SPacketPlayerPosLook implements Packet<INetHandlerPlayClient> {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private Set<EnumFlags> flags;
    private int teleportId;

    public SPacketPlayerPosLook() {
    }

    public SPacketPlayerPosLook(double xIn, double yIn, double zIn, float yawIn, float pitchIn, Set<EnumFlags> flagsIn, int teleportIdIn) {
        this.x = xIn;
        this.y = yIn;
        this.z = zIn;
        this.yaw = yawIn;
        this.pitch = pitchIn;
        this.flags = flagsIn;
        this.teleportId = teleportIdIn;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public int getTeleportId() {
        return teleportId;
    }

    public Set<EnumFlags> getFlags() {
        return flags;
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
        Set<net.minecraft.world.entity.RelativeMovement> relative = EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class);
        if (flags != null) for (EnumFlags f : flags) relative.add(f.mcc$modern());
        return new net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket(x, y, z, yaw, pitch, relative, teleportId);
    }

    public enum EnumFlags {
        X(0), Y(1), Z(2), Y_ROT(3), X_ROT(4);

        private final int bit;

        EnumFlags(int bitIn) {
            this.bit = bitIn;
        }

        private int getMask() {
            return 1 << bit;
        }

        private boolean isSet(int flags) {
            return (flags & getMask()) == getMask();
        }

        public static Set<EnumFlags> unpack(int flags) {
            Set<EnumFlags> set = EnumSet.noneOf(EnumFlags.class);
            for (EnumFlags f : values()) if (f.isSet(flags)) set.add(f);
            return set;
        }

        public static int pack(Set<EnumFlags> flags) {
            int i = 0;
            for (EnumFlags f : flags) i |= f.getMask();
            return i;
        }

        net.minecraft.world.entity.RelativeMovement mcc$modern() {
            return net.minecraft.world.entity.RelativeMovement.values()[ordinal()];
        }
    }
}
