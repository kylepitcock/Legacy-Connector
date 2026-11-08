package legacyconnector.v1_7_10.codechicken.lib.data;

import java.io.DataOutput;
import java.io.IOException;

/** A data stream that writes to a Java data output. */
public class MCDataOutputWrapper implements MCDataOutput {
    public final DataOutput dout;

    public MCDataOutputWrapper(DataOutput dout) {
        this.dout = dout;
    }

    public MCDataOutputWrapper writeBoolean(boolean b) {
        try {
            dout.writeBoolean(b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public MCDataOutputWrapper writeByte(int b) {
        try {
            dout.writeByte(b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public MCDataOutputWrapper writeShort(int s) {
        try {
            dout.writeShort(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public MCDataOutputWrapper writeInt(int i) {
        try {
            dout.writeInt(i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public MCDataOutputWrapper writeFloat(float f) {
        try {
            dout.writeFloat(f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public MCDataOutputWrapper writeDouble(double d) {
        try {
            dout.writeDouble(d);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public MCDataOutputWrapper writeLong(long l) {
        try {
            dout.writeLong(l);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public MCDataOutputWrapper writeChar(char c) {
        try {
            dout.writeChar(c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public MCDataOutputWrapper writeByteArray(byte[] barray) {
        try {
            dout.write(barray);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
}
