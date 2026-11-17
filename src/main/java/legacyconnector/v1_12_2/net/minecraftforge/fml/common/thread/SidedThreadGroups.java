package legacyconnector.v1_12_2.net.minecraftforge.fml.common.thread;

import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/**
 * The two sided thread groups of 1.12.2. The host's own threads cannot be moved into them (a running thread keeps its group), so a
 * check by group identity finds only threads a mod started inside the group; the side of the running thread is answered by
 * {@code FMLCommonHandler.getEffectiveSide()} (by thread, as the layer knows the server and network threads).
 */
public final class SidedThreadGroups {
    public static final SidedThreadGroup CLIENT = new SidedThreadGroup(Side.CLIENT);
    public static final SidedThreadGroup SERVER = new SidedThreadGroup(Side.SERVER);

    private SidedThreadGroups() {
    }
}
