package legacyconnector.v1_7_10.net.minecraft.util;

/** Vanilla's pair (villager stock ranges, mods' own pairs). */
public class Tuple {
    private final Object first;
    private final Object second;

    public Tuple(Object first, Object second) {
        this.first = first;
        this.second = second;
    }

    public Object getFirst() {
        return first;
    }

    public Object getSecond() {
        return second;
    }
}
