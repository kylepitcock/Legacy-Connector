package legacyconnector.vb1_7_3.net.minecraft.network;

/** Beta's {@code NetHandler}: what a packet's {@code processPacket} is handed; mods' own packets only ask which side it is. */
public abstract class NetworkHandler {
    protected NetworkHandler() {}

    /** Beta's {@code isServerHandler}. */
    public abstract boolean isServerSide();
}
