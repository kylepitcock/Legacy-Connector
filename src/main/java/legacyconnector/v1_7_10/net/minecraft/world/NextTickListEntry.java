package legacyconnector.v1_7_10.net.minecraft.world;

import legacyconnector.v1_7_10.net.minecraft.block.Block;

/**
 * A scheduled block update: position, block, the world time it is due and its priority. {@code World.getPendingBlockUpdates}
 * reports the modern scheduled ticks this way, so mods that move blocks (frames, spatial storage) can schedule them again.
 */
public class NextTickListEntry implements Comparable<NextTickListEntry> {
    private static long nextTickEntryID;
    private final Block field_151352_g;
    public int xCoord;
    public int yCoord;
    public int zCoord;
    public long scheduledTime;
    public int priority;
    private final long tickEntryID;

    public NextTickListEntry(int x, int y, int z, Block block) {
        this.tickEntryID = nextId();
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        this.field_151352_g = block;
    }

    private static synchronized long nextId() {
        return nextTickEntryID++;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NextTickListEntry other)) return false;
        return xCoord == other.xCoord && yCoord == other.yCoord && zCoord == other.zCoord && Block.isEqualTo(field_151352_g, other.field_151352_g);
    }

    @Override
    public int hashCode() {
        return (xCoord * 1024 * 1024 + zCoord * 1024 + yCoord) * 256;
    }

    public NextTickListEntry setScheduledTime(long time) {
        this.scheduledTime = time;
        return this;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(NextTickListEntry other) {
        if (scheduledTime != other.scheduledTime) return scheduledTime < other.scheduledTime ? -1 : 1;
        if (priority != other.priority) return priority - other.priority;
        return Long.compare(tickEntryID, other.tickEntryID);
    }

    @Override
    public String toString() {
        return Block.getIdFromBlock(field_151352_g) + ": (" + xCoord + ", " + yCoord + ", " + zCoord + "), " + scheduledTime + ", " + priority + ", " + tickEntryID;
    }

    /** The block the update is for. */
    public Block func_151351_a() {
        return field_151352_g;
    }
}
