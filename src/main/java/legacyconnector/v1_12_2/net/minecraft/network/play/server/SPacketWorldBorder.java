package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A world border update: the border's size, centre or warnings, sent as the modern border packet of the action. */
public class SPacketWorldBorder implements Packet<INetHandlerPlayClient> {
    private Action action;
    private int size;
    private double centerX;
    private double centerZ;
    private double targetSize;
    private double diameter;
    private long timeUntilTarget;
    private int warningTime;
    private int warningDistance;
    private net.minecraft.world.level.border.WorldBorder border;

    public SPacketWorldBorder() {
    }

    public SPacketWorldBorder(legacyconnector.v1_12_2.net.minecraft.world.border.WorldBorder border, Action actionIn) {
        this.action = actionIn;
        this.border = border.mcc$modern();
        this.centerX = this.border.getCenterX();
        this.centerZ = this.border.getCenterZ();
        this.diameter = this.border.getSize();
        this.targetSize = this.border.getLerpTarget();
        this.timeUntilTarget = this.border.getLerpRemainingTime();
        this.size = this.border.getAbsoluteMaxSize();
        this.warningTime = this.border.getWarningTime();
        this.warningDistance = this.border.getWarningBlocks();
    }

    public Action mcc$action() {
        return action;
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        if (action == null || border == null) return null;
        return switch (action) {
            case SET_SIZE -> new net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket(border);
            case LERP_SIZE -> new net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket(border);
            case SET_CENTER -> new net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket(border);
            case INITIALIZE -> new net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket(border);
            case SET_WARNING_TIME -> new net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket(border);
            case SET_WARNING_BLOCKS -> new net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket(border);
        };
    }

    public enum Action {
        SET_SIZE, LERP_SIZE, SET_CENTER, INITIALIZE, SET_WARNING_TIME, SET_WARNING_BLOCKS
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
