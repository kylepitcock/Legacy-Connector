package legacyconnector.v1_12_2.net.minecraftforge.common.animation;

/** An event an animation clip fires (a name and how long before the current time it happened); ordered by offset. */
public final class Event implements Comparable<Event> {
    private final String event;
    private final float offset;

    public Event(String event, float offset) {
        this.event = event;
        this.offset = offset;
    }

    public String event() {
        return event;
    }

    public float offset() {
        return offset;
    }

    @Override
    public int compareTo(Event event) {
        return Float.compare(offset, event.offset);
    }

    @Override
    public String toString() {
        return "Event(" + event + ", " + offset + ")";
    }
}
