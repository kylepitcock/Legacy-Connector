package legacyconnector.v1_12_2.net.minecraft.world.chunk;

/** 1.12.2 4-bit array of a 16x16x16 section (light levels, legacy metadata), index y << 8 | z << 4 | x. */
public class NibbleArray {
    private final byte[] data;

    public NibbleArray() {
        this.data = new byte[2048];
    }

    public NibbleArray(byte[] storage) {
        if (storage.length != 2048) throw new IllegalArgumentException("ChunkNibbleArrays should be 2048 bytes not: " + storage.length);
        this.data = storage;
    }

    public int get(int x, int y, int z) {
        return getFromIndex(y << 8 | z << 4 | x);
    }

    public void set(int x, int y, int z, int value) {
        setIndex(y << 8 | z << 4 | x, value);
    }

    public int getFromIndex(int index) {
        int i = index >> 1;
        return (index & 1) == 0 ? data[i] & 15 : data[i] >> 4 & 15;
    }

    public void setIndex(int index, int value) {
        int i = index >> 1;
        if ((index & 1) == 0) data[i] = (byte) (data[i] & 240 | value & 15);
        else data[i] = (byte) (data[i] & 15 | (value & 15) << 4);
    }

    public byte[] getData() {
        return data;
    }
}
