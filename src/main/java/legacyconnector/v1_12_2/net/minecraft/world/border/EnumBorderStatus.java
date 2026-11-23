package legacyconnector.v1_12_2.net.minecraft.world.border;

/** The state of a world border: growing, shrinking or at rest, with the colour the client draws its wall in. */
public enum EnumBorderStatus {
    GROWING(4259712),
    SHRINKING(16724016),
    STATIONARY(2138367);

    private final int color;

    EnumBorderStatus(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
