package legacyconnector.v1_6_4.net.minecraft.src;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * ModLoaderMP's mod packet (id 230): the mod's id, a mod-defined type and three data arrays. The layer carries it as the payload of a custom
 * payload packet on the channel {@link ModLoaderMp#CHANNEL}; {@link #encode} and {@link #decode} are that wire form (the arrays keep ModLoaderMP's
 * limits: 255 entries each).
 */
public class Packet230ModLoader extends legacyconnector.v1_7_10.net.minecraft.network.Packet {
    public static final int MAX_DATA_LENGTH = 65535;
    public int modId;
    public int packetType;
    public int[] dataInt = new int[0];
    public float[] dataFloat = new float[0];
    public String[] dataString = new String[0];

    public Packet230ModLoader() {}

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(modId);
            out.writeInt(packetType);
            out.writeByte(dataInt == null ? 0 : dataInt.length);
            if (dataInt != null) for (int v : dataInt) out.writeInt(v);
            out.writeByte(dataFloat == null ? 0 : dataFloat.length);
            if (dataFloat != null) for (float v : dataFloat) out.writeFloat(v);
            out.writeByte(dataString == null ? 0 : dataString.length);
            if (dataString != null) for (String v : dataString) out.writeUTF(v == null ? "" : v);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }

    public static Packet230ModLoader decode(byte[] data) {
        Packet230ModLoader p = new Packet230ModLoader();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            p.modId = in.readInt();
            p.packetType = in.readInt();
            p.dataInt = new int[in.readUnsignedByte()];
            for (int i = 0; i < p.dataInt.length; i++) p.dataInt[i] = in.readInt();
            p.dataFloat = new float[in.readUnsignedByte()];
            for (int i = 0; i < p.dataFloat.length; i++) p.dataFloat[i] = in.readFloat();
            p.dataString = new String[in.readUnsignedByte()];
            for (int i = 0; i < p.dataString.length; i++) p.dataString[i] = in.readUTF();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return p;
    }
}
