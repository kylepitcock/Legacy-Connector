package legacyconnector.v1_12_2.net.minecraft.tileentity;

import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * 1.12.2's banner patterns: the texture name, the short hash saved in a banner's {@code Patterns} list, and how the loom of 1.12 (the
 * crafting grid) made it: a 3x3 dye shape or an item. The hashes are the modern ones, so legacy banner data shows on modern banners.
 */
public enum BannerPattern {
    BASE("base", "b"),
    SQUARE_BOTTOM_LEFT("square_bottom_left", "bl", "   ", "   ", "#  "),
    SQUARE_BOTTOM_RIGHT("square_bottom_right", "br", "   ", "   ", "  #"),
    SQUARE_TOP_LEFT("square_top_left", "tl", "#  ", "   ", "   "),
    SQUARE_TOP_RIGHT("square_top_right", "tr", "  #", "   ", "   "),
    STRIPE_BOTTOM("stripe_bottom", "bs", "   ", "   ", "###"),
    STRIPE_TOP("stripe_top", "ts", "###", "   ", "   "),
    STRIPE_LEFT("stripe_left", "ls", "#  ", "#  ", "#  "),
    STRIPE_RIGHT("stripe_right", "rs", "  #", "  #", "  #"),
    STRIPE_CENTER("stripe_center", "cs", " # ", " # ", " # "),
    STRIPE_MIDDLE("stripe_middle", "ms", "   ", "###", "   "),
    STRIPE_DOWNRIGHT("stripe_downright", "drs", "#  ", " # ", "  #"),
    STRIPE_DOWNLEFT("stripe_downleft", "dls", "  #", " # ", "#  "),
    STRIPE_SMALL("small_stripes", "ss", "# #", "# #", "   "),
    CROSS("cross", "cr", "# #", " # ", "# #"),
    STRAIGHT_CROSS("straight_cross", "sc", " # ", "###", " # "),
    TRIANGLE_BOTTOM("triangle_bottom", "bt", "   ", " # ", "# #"),
    TRIANGLE_TOP("triangle_top", "tt", "# #", " # ", "   "),
    TRIANGLES_BOTTOM("triangles_bottom", "bts", "   ", "# #", " # "),
    TRIANGLES_TOP("triangles_top", "tts", " # ", "# #", "   "),
    DIAGONAL_LEFT("diagonal_left", "ld", "## ", "#  ", "   "),
    DIAGONAL_RIGHT("diagonal_up_right", "rd", "   ", "  #", " ##"),
    DIAGONAL_LEFT_MIRROR("diagonal_up_left", "lud", "   ", "#  ", "## "),
    DIAGONAL_RIGHT_MIRROR("diagonal_right", "rud", " ##", "  #", "   "),
    CIRCLE_MIDDLE("circle", "mc", "   ", " # ", "   "),
    RHOMBUS_MIDDLE("rhombus", "mr", " # ", "# #", " # "),
    HALF_VERTICAL("half_vertical", "vh", "## ", "## ", "## "),
    HALF_HORIZONTAL("half_horizontal", "hh", "###", "###", "   "),
    HALF_VERTICAL_MIRROR("half_vertical_right", "vhr", " ##", " ##", " ##"),
    HALF_HORIZONTAL_MIRROR("half_horizontal_bottom", "hhb", "   ", "###", "###"),
    BORDER("border", "bo", "###", "# #", "###"),
    CURLY_BORDER("curly_border", "cbo", () -> modern(net.minecraft.world.item.Items.VINE)),
    CREEPER("creeper", "cre", () -> modern(net.minecraft.world.item.Items.CREEPER_HEAD)),
    GRADIENT("gradient", "gra", "# #", " # ", " # "),
    GRADIENT_UP("gradient_up", "gru", " # ", " # ", "# #"),
    BRICKS("bricks", "bri", () -> modern(net.minecraft.world.item.Items.BRICKS)),
    SKULL("skull", "sku", () -> modern(net.minecraft.world.item.Items.WITHER_SKELETON_SKULL)),
    FLOWER("flower", "flo", () -> modern(net.minecraft.world.item.Items.OXEYE_DAISY)),
    MOJANG("mojang", "moj", () -> modern(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE));

    private final String fileName;
    private final String hashname;
    private final String[] patterns = new String[3];
    private ItemStack patternItem;
    private final Supplier<ItemStack> mcc$item;

    BannerPattern(String name, String id) {
        this.fileName = name;
        this.hashname = id;
        this.mcc$item = null;
        this.patternItem = null;
    }

    BannerPattern(String name, String id, ItemStack craftingItem) {
        this(name, id);
        this.patternItem = craftingItem;
    }

    BannerPattern(String name, String id, Supplier<ItemStack> craftingItem) {
        this.fileName = name;
        this.hashname = id;
        this.mcc$item = craftingItem;
    }

    BannerPattern(String name, String id, String craftingTop, String craftingMid, String craftingBot) {
        this(name, id);
        this.patterns[0] = craftingTop;
        this.patterns[1] = craftingMid;
        this.patterns[2] = craftingBot;
    }

    private static ItemStack modern(net.minecraft.world.item.Item item) {
        return Stacks1122.fromModern(new net.minecraft.world.item.ItemStack(item));
    }

    public String getFileName() {
        return fileName;
    }

    public String getHashname() {
        return hashname;
    }

    public String[] getPatterns() {
        return patterns;
    }

    public boolean hasPattern() {
        return !hasPatternItem() && patterns[0] != null;
    }

    public boolean hasPatternItem() {
        return patternItem != null || mcc$item != null;
    }

    public ItemStack getPatternItem() {
        if (patternItem == null && mcc$item != null) patternItem = mcc$item.get();
        return patternItem == null ? ItemStack.EMPTY : patternItem;
    }

    public static BannerPattern byHash(String hash) {
        for (BannerPattern p : values()) if (p.hashname.equals(hash)) return p;
        return null;
    }
}
