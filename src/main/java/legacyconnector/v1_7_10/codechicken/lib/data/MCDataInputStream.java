package legacyconnector.v1_7_10.codechicken.lib.data;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** A data stream that reads from a Java input stream. */
public class MCDataInputStream implements MCDataInput {
    public final DataInputStream din;

    public MCDataInputStream(InputStream in) {
        this.din = in instanceof DataInputStream d ? d : new DataInputStream(in);
    }

    private static RuntimeException wrap(IOException e) {
        return new RuntimeException(e);
    }

    public long readLong() {
        try {
            return din.readLong();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    public int readInt() {
        try {
            return din.readInt();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    public short readShort() {
        try {
            return din.readShort();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    public int readUShort() {
        try {
            return din.readUnsignedShort();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    public byte readByte() {
        try {
            return din.readByte();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    public short readUByte() {
        try {
            return (short) din.readUnsignedByte();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    public double readDouble() {
        try {
            return din.readDouble();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    public float readFloat() {
        try {
            return din.readFloat();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    public boolean readBoolean() {
        try {
            return din.readBoolean();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    public char readChar() {
        try {
            return din.readChar();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    public int readVarShort() {
        int low = readUShort();
        int high = 0;
        if ((low & 0x8000) != 0) {
            low &= 0x7FFF;
            high = readUByte();
        }
        return high << 15 | low;
    }

    public int readVarInt() {
        int i = 0;
        int shift = 0;
        while (true) {
            int b = readUByte();
            i |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return i;
            shift += 7;
            if (shift > 35) throw new RuntimeException("VarInt too big");
        }
    }

    public byte[] readByteArray(int length) {
        byte[] b = new byte[length];
        try {
            din.readFully(b);
        } catch (IOException e) {
            throw wrap(e);
        }
        return b;
    }

    public String readString() {
        return new String(readByteArray(readVarInt()), java.nio.charset.StandardCharsets.UTF_8);
    }
}
