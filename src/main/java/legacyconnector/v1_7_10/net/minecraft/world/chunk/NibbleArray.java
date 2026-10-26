package legacyconnector.v1_7_10.net.minecraft.world.chunk;

/** Four-bit values packed two per byte (light and metadata of a chunk section), indexed {@code y << (depthBits + 4) | z << depthBits | x}. */
public class NibbleArray {
    public final byte[] data;
    public final int depthBits;
    public final int depthBitsPlusFour;

    public NibbleArray(int size, int depthBits) {
        this(new byte[size >> 1], depthBits);
    }

    public NibbleArray(byte[] data, int depthBits) {
        this.data = data;
        this.depthBits = depthBits;
        this.depthBitsPlusFour = depthBits + 4;
    }

    public int get(int x, int y, int z) {
        int index = y << depthBitsPlusFour | z << depthBits | x;
        int b = data[index >> 1];
        return (index & 1) == 0 ? b & 15 : b >> 4 & 15;
    }

    public void set(int x, int y, int z, int value) {
        int index = y << depthBitsPlusFour | z << depthBits | x;
        int i = index >> 1;
        data[i] = (index & 1) == 0 ? (byte) (data[i] & 0xF0 | value & 15) : (byte) (data[i] & 15 | (value & 15) << 4);
    }
}
