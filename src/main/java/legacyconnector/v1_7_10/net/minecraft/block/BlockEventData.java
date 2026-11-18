package legacyconnector.v1_7_10.net.minecraft.block;

/** A queued block event (1.7.10 {@code BlockEventData}): the block at a position, an event id and a parameter, delivered to {@code Block.onBlockEventReceived}. */
public class BlockEventData {
    private final int coordX;
    private final int coordY;
    private final int coordZ;
    private final Block field_151344_d;
    private final int eventID;
    private final int eventParameter;

    public BlockEventData(int x, int y, int z, Block block, int eventId, int eventParameter) {
        this.coordX = x;
        this.coordY = y;
        this.coordZ = z;
        this.field_151344_d = block;
        this.eventID = eventId;
        this.eventParameter = eventParameter;
    }

    public int func_151340_a() {
        return coordX;
    }

    public int func_151342_b() {
        return coordY;
    }

    public int func_151341_c() {
        return coordZ;
    }

    public int getEventID() {
        return eventID;
    }

    public int getEventParameter() {
        return eventParameter;
    }

    public Block getBlock() {
        return field_151344_d;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BlockEventData e)) return false;
        return coordX == e.coordX && coordY == e.coordY && coordZ == e.coordZ && eventID == e.eventID && eventParameter == e.eventParameter
                && field_151344_d == e.field_151344_d;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(coordX, coordY, coordZ, eventID, eventParameter, field_151344_d);
    }

    @Override
    public String toString() {
        return "TE(" + coordX + "," + coordY + "," + coordZ + ")," + eventID + "," + eventParameter + "," + field_151344_d;
    }
}
