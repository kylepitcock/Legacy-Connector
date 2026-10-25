package legacyconnector.v1_12_2.net.minecraft.block.material;

/** The colours blocks have on maps. */
public class MapColor {
    public static final MapColor[] COLORS = new MapColor[64];
    public static final MapColor AIR = new MapColor(0, 0);
    public static final MapColor GRASS = new MapColor(1, 8368696);
    public static final MapColor SAND = new MapColor(2, 16247203);
    public static final MapColor CLOTH = new MapColor(3, 13092807);
    public static final MapColor TNT = new MapColor(4, 16711680);
    public static final MapColor ICE = new MapColor(5, 10526975);
    public static final MapColor IRON = new MapColor(6, 10987431);
    public static final MapColor FOLIAGE = new MapColor(7, 31744);
    public static final MapColor SNOW = new MapColor(8, 16777215);
    public static final MapColor CLAY = new MapColor(9, 10791096);
    public static final MapColor DIRT = new MapColor(10, 9923917);
    public static final MapColor STONE = new MapColor(11, 7368816);
    public static final MapColor WATER = new MapColor(12, 4210943);
    public static final MapColor WOOD = new MapColor(13, 9402184);
    public static final MapColor QUARTZ = new MapColor(14, 16776437);
    public static final MapColor ADOBE = new MapColor(15, 14188339);
    public static final MapColor MAGENTA = new MapColor(16, 11685080);
    public static final MapColor LIGHT_BLUE = new MapColor(17, 6724056);
    public static final MapColor YELLOW = new MapColor(18, 15066419);
    public static final MapColor LIME = new MapColor(19, 8375321);
    public static final MapColor PINK = new MapColor(20, 15892389);
    public static final MapColor GRAY = new MapColor(21, 5000268);
    public static final MapColor SILVER = new MapColor(22, 10066329);
    public static final MapColor CYAN = new MapColor(23, 5013401);
    public static final MapColor PURPLE = new MapColor(24, 8339378);
    public static final MapColor BLUE = new MapColor(25, 3361970);
    public static final MapColor BROWN = new MapColor(26, 6704179);
    public static final MapColor GREEN = new MapColor(27, 6717235);
    public static final MapColor RED = new MapColor(28, 10040115);
    public static final MapColor BLACK = new MapColor(29, 1644825);
    public static final MapColor GOLD = new MapColor(30, 16445005);
    public static final MapColor DIAMOND = new MapColor(31, 6085589);
    public static final MapColor LAPIS = new MapColor(32, 4882687);
    public static final MapColor EMERALD = new MapColor(33, 55610);
    public static final MapColor OBSIDIAN = new MapColor(34, 8476209);
    public static final MapColor NETHERRACK = new MapColor(35, 7340544);
    public static final MapColor WHITE_STAINED_HARDENED_CLAY = new MapColor(36, 13742497);
    public static final MapColor ORANGE_STAINED_HARDENED_CLAY = new MapColor(37, 10441252);
    public static final MapColor MAGENTA_STAINED_HARDENED_CLAY = new MapColor(38, 9787244);
    public static final MapColor LIGHT_BLUE_STAINED_HARDENED_CLAY = new MapColor(39, 7367818);
    public static final MapColor YELLOW_STAINED_HARDENED_CLAY = new MapColor(40, 12223780);
    public static final MapColor LIME_STAINED_HARDENED_CLAY = new MapColor(41, 6780213);
    public static final MapColor PINK_STAINED_HARDENED_CLAY = new MapColor(42, 10505550);
    public static final MapColor GRAY_STAINED_HARDENED_CLAY = new MapColor(43, 3746083);
    public static final MapColor SILVER_STAINED_HARDENED_CLAY = new MapColor(44, 8874850);
    public static final MapColor CYAN_STAINED_HARDENED_CLAY = new MapColor(45, 5725276);
    public static final MapColor PURPLE_STAINED_HARDENED_CLAY = new MapColor(46, 8014168);
    public static final MapColor BLUE_STAINED_HARDENED_CLAY = new MapColor(47, 4996700);
    public static final MapColor BROWN_STAINED_HARDENED_CLAY = new MapColor(48, 4993571);
    public static final MapColor GREEN_STAINED_HARDENED_CLAY = new MapColor(49, 5001770);
    public static final MapColor RED_STAINED_HARDENED_CLAY = new MapColor(50, 9321518);
    public static final MapColor BLACK_STAINED_HARDENED_CLAY = new MapColor(51, 2430480);

    public int colorValue;
    public final int colorIndex;

    private MapColor(int index, int color) {
        if (index < 0 || index > 63) throw new IndexOutOfBoundsException("Map colour ID must be between 0 and 63 (inclusive)");
        this.colorIndex = index;
        this.colorValue = color;
        COLORS[index] = this;
    }

    public static MapColor getBlockColor(legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor dye) {
        return COLORS[Math.min(dye.getMetadata() + 1, 15)];
    }

    public int getMapColor(int index) {
        int[] mult = {180, 220, 255, 135};
        int m = mult[index & 3];
        int r = (colorValue >> 16 & 255) * m / 255;
        int g = (colorValue >> 8 & 255) * m / 255;
        int b = (colorValue & 255) * m / 255;
        return -16777216 | b << 16 | g << 8 | r;
    }
}
