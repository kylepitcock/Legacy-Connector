package legacyconnector.vb1_7_3.net.minecraft.block;

/** Beta 1.7.3 {@code MapColor}: the 14 colours of the first maps. */
public class MapColor {
    public static final MapColor[] COLORS = new MapColor[16];
    public static final MapColor CLEAR = new MapColor(0, 0);
    public static final MapColor PALE_GREEN = new MapColor(1, 8368696);
    public static final MapColor PALE_YELLOW = new MapColor(2, 16247203);
    public static final MapColor LIGHT_GRAY = new MapColor(3, 10987431);
    public static final MapColor RED = new MapColor(4, 16711680);
    public static final MapColor LIGHT_BLUE = new MapColor(5, 10526975);
    public static final MapColor LIGHT_GRAY2 = new MapColor(6, 10987431);
    public static final MapColor GREEN = new MapColor(7, 31744);
    public static final MapColor WHITE = new MapColor(8, 16777215);
    public static final MapColor SILVER = new MapColor(9, 10791096);
    public static final MapColor ORANGE = new MapColor(10, 12020271);
    public static final MapColor GRAY = new MapColor(11, 7368816);
    public static final MapColor BLUE = new MapColor(12, 4210943);
    public static final MapColor BROWN = new MapColor(13, 6837042);

    public final int color;
    public final int id;

    private MapColor(int id, int color) {
        this.id = id;
        this.color = color;
        COLORS[id] = this;
    }
}
