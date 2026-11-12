package legacyconnector.vb1_7_3.net.minecraft.src;

/** ModLoaderMP's mod packet: an id of the mod, a mod-defined type and three data arrays. */
public class Packet230ModLoader extends legacyconnector.vb1_7_3.net.minecraft.network.packet.Packet {
    public static final int MAX_DATA_LENGTH = 65535;
    public int modId;
    public int packetType;
    public int[] dataInt = new int[0];
    public float[] dataFloat = new float[0];
    public String[] dataString = new String[0];

    public Packet230ModLoader() {}
}
