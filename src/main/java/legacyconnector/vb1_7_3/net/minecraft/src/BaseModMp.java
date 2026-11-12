package legacyconnector.vb1_7_3.net.minecraft.src;

/**
 * ModLoaderMP's {@code BaseModMp}. Hooks that take server-side game objects (player-bound packets, GUIs, commands) are not declared:
 * the mods' own implementations are found by name by the runtime, as with {@link BaseMod}.
 */
public abstract class BaseModMp extends BaseMod {
    public BaseModMp() {}

    /** ModLoaderMP's id of a mod is the hash of its class name, stable between sessions. */
    public final int getId() {
        return getClass().getSimpleName().hashCode();
    }

    /** A packet the server sent to this client. */
    public void HandlePacket(Packet230ModLoader packet) {}

    public void HandleTileEntityPacket(int x, int y, int z, int type, int[] ints, float[] floats, String[] strings) {}

    public boolean hasClientSide() {
        return true;
    }
}
