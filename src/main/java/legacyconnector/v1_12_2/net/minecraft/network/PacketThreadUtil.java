package legacyconnector.v1_12_2.net.minecraft.network;

/** Moves a packet's handling to the main thread when it arrives on a network thread. */
public class PacketThreadUtil {
    private PacketThreadUtil() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends INetHandler> void checkThreadAndEnqueue(Packet<T> packetIn, T processor,
            legacyconnector.v1_12_2.net.minecraft.util.IThreadListener scheduler) throws ThreadQuickExitException {
        if (!scheduler.isCallingFromMinecraftThread()) {
            scheduler.addScheduledTask(() -> packetIn.processPacket(processor));
            throw ThreadQuickExitException.INSTANCE;
        }
    }
}
