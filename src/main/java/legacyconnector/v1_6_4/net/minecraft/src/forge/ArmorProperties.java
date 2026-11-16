package legacyconnector.v1_6_4.net.minecraft.src.forge;

public class ArmorProperties implements Comparable<ArmorProperties> {
    public int Priority;
    public int AbsorbMax;
    public double AbsorbRatio;
    public int Slot;

    public ArmorProperties(int priority, double ratio, int max) {
        this.Priority = priority;
        this.AbsorbRatio = ratio;
        this.AbsorbMax = max;
    }

    @Override
    public int compareTo(ArmorProperties other) {
        if (other.Priority != Priority) return other.Priority - Priority;
        double left = AbsorbRatio == 0 ? 0 : AbsorbMax / AbsorbRatio;
        double right = other.AbsorbRatio == 0 ? 0 : other.AbsorbMax / other.AbsorbRatio;
        return Double.compare(left, right);
    }

    public ArmorProperties copy() {
        ArmorProperties c = new ArmorProperties(Priority, AbsorbRatio, AbsorbMax);
        c.Slot = Slot;
        return c;
    }

    @Override
    public String toString() {
        return String.format("%d, %d, %f, %d", Priority, AbsorbMax, AbsorbRatio, Slot);
    }
}
