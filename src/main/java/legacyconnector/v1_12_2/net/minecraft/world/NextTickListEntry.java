package legacyconnector.v1_12_2.net.minecraft.world;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** 1.12.2 scheduled block tick: position, block, due time and priority (ordered by time, then priority, then creation). */
public class NextTickListEntry implements Comparable<NextTickListEntry> {
    private static long nextTickEntryID;
    private final Block block;
    public final BlockPos position;
    public long scheduledTime;
    public int priority;
    private final long tickEntryID;

    public NextTickListEntry(BlockPos positionIn, Block blockIn) {
        this.tickEntryID = nextTickEntryID++;
        this.position = positionIn.toImmutable();
        this.block = blockIn;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NextTickListEntry e && position.equals(e.position) && block == e.block;
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }

    public NextTickListEntry setScheduledTime(long time) {
        this.scheduledTime = time;
        return this;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(NextTickListEntry o) {
        if (scheduledTime != o.scheduledTime) return scheduledTime < o.scheduledTime ? -1 : 1;
        if (priority != o.priority) return priority - o.priority;
        return Long.compare(tickEntryID, o.tickEntryID);
    }

    @Override
    public String toString() {
        return block + ": " + position + ", " + scheduledTime + ", " + priority + ", " + tickEntryID;
    }

    public Block getBlock() {
        return block;
    }
}
