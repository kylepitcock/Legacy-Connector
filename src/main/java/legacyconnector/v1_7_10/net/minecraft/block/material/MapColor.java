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
    public static final MapColor magentaColor = new MapColor(16, 11685080);
    public static final MapColor lightBlueColor = new MapColor(17, 6724056);
    public static final MapColor yellowColor = new MapColor(18, 15066419);
    public static final MapColor limeColor = new MapColor(19, 8375321);
    public static final MapColor pinkColor = new MapColor(20, 15892389);
    public static final MapColor grayColor = new MapColor(21, 5000268);
    public static final MapColor silverColor = new MapColor(22, 10066329);
    public static final MapColor cyanColor = new MapColor(23, 5013401);
    public static final MapColor purpleColor = new MapColor(24, 8339378);
    public static final MapColor blueColor = new MapColor(25, 3361970);
    public static final MapColor brownColor = new MapColor(26, 6704179);
    public static final MapColor greenColor = new MapColor(27, 6717235);
    public static final MapColor redColor = new MapColor(28, 10040115);
    public static final MapColor blackColor = new MapColor(29, 1644825);
    public static final MapColor goldColor = new MapColor(30, 16445005);
    public static final MapColor diamondColor = new MapColor(31, 6085589);
    public static final MapColor lapisColor = new MapColor(32, 4882687);
    public static final MapColor emeraldColor = new MapColor(33, 55610);
    public static final MapColor obsidianColor = new MapColor(34, 1381407);
    public static final MapColor netherrackColor = new MapColor(35, 7340544);

    public final int colorValue;
    public final int colorIndex;

    private MapColor(int index, int color) {
        this.colorIndex = index;
        this.colorValue = color;
        mapColorArray[index] = this;
    }

    /** The map colour of a block's shade (0-3): 180/255, 220/255, full and 135/255 brightness, as 1.7.10 drew maps. */
    public int func_151643_b(int brightness) {
        int scale = switch (brightness) {
            case 0 -> 180;
            case 1 -> 220;
            case 3 -> 135;
            default -> 255;
        };
        int r = (colorValue >> 16 & 255) * scale / 255;
        int g = (colorValue >> 8 & 255) * scale / 255;
        int b = (colorValue & 255) * scale / 255;
        return -16777216 | r << 16 | g << 8 | b;
    }

    /** The map colour of a wool, carpet or stained clay colour by its metadata (0 white ... 15 black). */
    public static MapColor getMapColorForBlockColored(int meta) {
        return switch (~meta & 15) {
            case 0 -> blackColor;
            case 1 -> redColor;
            case 2 -> greenColor;
            case 3 -> brownColor;
            case 4 -> blueColor;
            case 5 -> purpleColor;
            case 6 -> cyanColor;
            case 7 -> silverColor;
            case 8 -> grayColor;
            case 9 -> pinkColor;
            case 10 -> limeColor;
            case 11 -> yellowColor;
            case 12 -> lightBlueColor;
            case 13 -> magentaColor;
            case 14 -> adobeColor;
            default -> snowColor;
        };
    }

    public net.minecraft.world.level.material.MapColor mcc$modern() {
        return net.minecraft.world.level.material.MapColor.byId(colorIndex);
    }
}
