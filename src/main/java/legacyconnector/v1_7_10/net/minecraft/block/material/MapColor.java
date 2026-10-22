package legacyconnector.v1_7_10.net.minecraft.block.material;

public class MapColor {
    public static final MapColor[] mapColorArray = new MapColor[64];
    public static final MapColor airColor = new MapColor(0, 0);
    public static final MapColor grassColor = new MapColor(1, 8368696);
    public static final MapColor sandColor = new MapColor(2, 16247203);
    public static final MapColor clothColor = new MapColor(3, 10987431);
    public static final MapColor tntColor = new MapColor(4, 16711680);
    public static final MapColor iceColor = new MapColor(5, 10526975);
    public static final MapColor ironColor = new MapColor(6, 10987431);
    public static final MapColor foliageColor = new MapColor(7, 31744);
    public static final MapColor snowColor = new MapColor(8, 16777215);
    public static final MapColor clayColor = new MapColor(9, 10791096);
    public static final MapColor dirtColor = new MapColor(10, 12020271);
    public static final MapColor stoneColor = new MapColor(11, 7368816);
    public static final MapColor waterColor = new MapColor(12, 4210943);
    public static final MapColor woodColor = new MapColor(13, 6837042);
    public static final MapColor quartzColor = new MapColor(14, 16776437);
    public static final MapColor adobeColor = new MapColor(15, 14188339);
    public static final MapColor obsidianColor = new MapColor(29, 1644825);

    public final int colorValue;
    public final int colorIndex;

    private MapColor(int index, int color) {
        this.colorIndex = index;
        this.colorValue = color;
        mapColorArray[index] = this;
    }

    public int func_151643_b(int brightness) {
        return colorValue;
    }

    public net.minecraft.world.level.material.MapColor mcc$modern() {
        return net.minecraft.world.level.material.MapColor.byId(colorIndex);
    }
}
