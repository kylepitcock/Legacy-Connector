package legacyconnector.v1_7_10.net.minecraft.world.chunk.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * An Anvil/McRegion file: 32 by 32 chunks in 4 KiB sectors, a location table (sector offset and count) and a timestamp table at the
 * start, each chunk stored as a length, a compression byte (1 gzip, 2 deflate) and the data. This is the on-disk format the host's
 * own storage reads too, so mods that read or write region files directly (map mods, converters) see the same bytes.
 */
public class RegionFile {
    private static final int SECTOR_BYTES = 4096;
    private static final int SECTOR_INTS = 1024;
    private static final byte[] EMPTY_SECTOR = new byte[SECTOR_BYTES];

    private final File fileName;
    private RandomAccessFile dataFile;
    private final int[] offsets = new int[SECTOR_INTS];
    private final int[] chunkTimestamps = new int[SECTOR_INTS];
    private List<Boolean> sectorFree;
    private int sizeDelta;
    private long lastModified;

    public RegionFile(File file) {
        this.fileName = file;
        this.sizeDelta = 0;
        try {
            if (file.exists()) lastModified = file.lastModified();
            dataFile = new RandomAccessFile(file, "rw");
            if (dataFile.length() < SECTOR_BYTES) {
                for (int i = 0; i < SECTOR_INTS; i++) dataFile.writeInt(0);
                for (int i = 0; i < SECTOR_INTS; i++) dataFile.writeInt(0);
                sizeDelta += SECTOR_BYTES * 2;
            }
            if ((dataFile.length() & 4095L) != 0L) {
                for (long i = 0; i < (dataFile.length() & 4095L); i++) dataFile.write(0);
            }
            int sectors = (int) dataFile.length() / SECTOR_BYTES;
            sectorFree = new ArrayList<>(sectors);
            for (int i = 0; i < sectors; i++) sectorFree.add(Boolean.TRUE);
            sectorFree.set(0, Boolean.FALSE);
            sectorFree.set(1, Boolean.FALSE);
            dataFile.seek(0L);
            for (int i = 0; i < SECTOR_INTS; i++) {
                int offset = dataFile.readInt();
                offsets[i] = offset;
                if (offset != 0 && (offset >> 8) + (offset & 255) <= sectorFree.size()) {
                    for (int s = 0; s < (offset & 255); s++) sectorFree.set((offset >> 8) + s, Boolean.FALSE);
                }
            }
            for (int i = 0; i < SECTOR_INTS; i++) chunkTimestamps[i] = dataFile.readInt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized int getSizeDelta() {
        int delta = sizeDelta;
        sizeDelta = 0;
        return delta;
    }

    private static boolean outOfBounds(int x, int z) {
        return x < 0 || x >= 32 || z < 0 || z >= 32;
    }

    /** Whether the chunk (region-local coordinates) has been written. */
    public synchronized boolean chunkExists(int x, int z) {
        if (outOfBounds(x, z)) return false;
        int offset = offsets[x + z * 32];
        if (offset == 0) return false;
        return (offset >> 8) + (offset & 255) <= sectorFree.size();
    }

    /** The chunk's data stream, decompressed; null when the chunk is absent or unreadable. */
    public synchronized DataInputStream getChunkDataInputStream(int x, int z) {
        if (!chunkExists(x, z)) return null;
        try {
            int offset = offsets[x + z * 32];
            int sector = offset >> 8;
            int count = offset & 255;
            dataFile.seek((long) sector * SECTOR_BYTES);
            int length = dataFile.readInt();
            if (length > SECTOR_BYTES * count || length <= 0) return null;
            byte version = dataFile.readByte();
            byte[] data = new byte[length - 1];
            dataFile.read(data);
            if (version == 1) return new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data)));
            if (version == 2) return new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)));
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /** A stream whose content is compressed and stored as the chunk when it is closed. */
    public DataOutputStream getChunkDataOutputStream(int x, int z) {
        if (outOfBounds(x, z)) return null;
        return new DataOutputStream(new DeflaterOutputStream(new ChunkBuffer(x, z)));
    }

    private class ChunkBuffer extends ByteArrayOutputStream {
        private final int chunkX;
        private final int chunkZ;

        ChunkBuffer(int x, int z) {
            super(8096);
            this.chunkX = x;
            this.chunkZ = z;
        }

        @Override
        public void close() throws IOException {
            RegionFile.this.write(chunkX, chunkZ, buf, count);
        }
    }

    protected synchronized void write(int x, int z, byte[] data, int length) {
        try {
            int offset = offsets[x + z * 32];
            int sector = offset >> 8;
            int oldCount = offset & 255;
            int needed = (length + 5) / SECTOR_BYTES + 1;
            if (needed >= 256) return;
            if (sector != 0 && oldCount == needed) {
                writeSector(sector, data, length);
            } else {
                for (int i = 0; i < oldCount; i++) sectorFree.set(sector + i, Boolean.TRUE);
                int start = sectorFree.indexOf(Boolean.TRUE);
                int run = 0;
                if (start != -1) {
                    for (int i = start; i < sectorFree.size(); i++) {
                        if (run != 0) {
                            run = sectorFree.get(i) ? run + 1 : 0;
                        } else if (sectorFree.get(i)) {
                            start = i;
                            run = 1;
                        }
                        if (run >= needed) break;
                    }
                }
                if (run >= needed) {
                    sector = start;
                    setOffset(x, z, sector << 8 | needed);
                    for (int i = 0; i < needed; i++) sectorFree.set(sector + i, Boolean.FALSE);
                    writeSector(sector, data, length);
                } else {
                    dataFile.seek(dataFile.length());
                    sector = sectorFree.size();
                    for (int i = 0; i < needed; i++) {
                        dataFile.write(EMPTY_SECTOR);
                        sectorFree.add(Boolean.FALSE);
                    }
                    sizeDelta += SECTOR_BYTES * needed;
                    writeSector(sector, data, length);
                    setOffset(x, z, sector << 8 | needed);
                }
            }
            setTimestamp(x, z, (int) (System.currentTimeMillis() / 1000L));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeSector(int sector, byte[] data, int length) throws IOException {
        dataFile.seek((long) sector * SECTOR_BYTES);
        dataFile.writeInt(length + 1);
        dataFile.writeByte(2);
        dataFile.write(data, 0, length);
    }

    private void setOffset(int x, int z, int offset) throws IOException {
        offsets[x + z * 32] = offset;
        dataFile.seek((x + z * 32) * 4L);
        dataFile.writeInt(offset);
    }

    private void setTimestamp(int x, int z, int time) throws IOException {
        chunkTimestamps[x + z * 32] = time;
        dataFile.seek(SECTOR_BYTES + (x + z * 32) * 4L);
        dataFile.writeInt(time);
    }

    public synchronized void close() throws IOException {
        if (dataFile != null) dataFile.close();
    }
}
