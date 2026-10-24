package legacyconnector.v1_7_10.net.minecraft.creativetab;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CreativeTabBridge;

/**
 * Vanilla legacy tabs map onto the modern vanilla tabs; every mod-created tab becomes a modern creative tab
 * registered by {@link CreativeTabBridge}.
 */
public abstract class CreativeTabs {
    public static CreativeTabs[] creativeTabArray = new CreativeTabs[12];
    public static final CreativeTabs tabBlock = new Vanilla(0, "buildingBlocks", "building_blocks");
    public static final CreativeTabs tabDecorations = new Vanilla(1, "decorations", "functional_blocks");
    public static final CreativeTabs tabRedstone = new Vanilla(2, "redstone", "redstone_blocks");
    public static final CreativeTabs tabTransport = new Vanilla(3, "transportation", "tools_and_utilities");
    public static final CreativeTabs tabMisc = new Vanilla(4, "misc", "ingredients");
    public static final CreativeTabs tabAllSearch = new Vanilla(5, "search", "search").setBackgroundImageName("item_search.png");
    public static final CreativeTabs tabFood = new Vanilla(6, "food", "food_and_drinks");
    public static final CreativeTabs tabTools = new Vanilla(7, "tools", "tools_and_utilities");
    public static final CreativeTabs tabCombat = new Vanilla(8, "combat", "combat");
    public static final CreativeTabs tabBrewing = new Vanilla(9, "brewing", "food_and_drinks");
    public static final CreativeTabs tabMaterials = new Vanilla(10, "materials", "ingredients");
    public static final CreativeTabs tabInventory = new Vanilla(11, "inventory", "inventory").setBackgroundImageName("inventory.png").setNoScrollbar().setNoTitle();

    private final int tabIndex;
    private final String tabLabel;
    private String backgroundImageName = "items.png";
    private boolean hasScrollbar = true;
    private boolean drawTitle = true;
    private ItemStack iconItemStack;

    public CreativeTabs(String label) {
        this(getNextID(), label);
    }

    public CreativeTabs(int index, String label) {
        if (index >= creativeTabArray.length) {
            CreativeTabs[] tmp = new CreativeTabs[index + 1];
            System.arraycopy(creativeTabArray, 0, tmp, 0, creativeTabArray.length);
            creativeTabArray = tmp;
        }
        this.tabIndex = index;
        this.tabLabel = label;
        creativeTabArray[index] = this;
        if (!(this instanceof Vanilla)) CreativeTabBridge.onTabCreated(this);
    }

    private legacyconnector.v1_7_10.net.minecraft.enchantment.EnumEnchantmentType[] relevantEnchantmentTypes = new legacyconnector.v1_7_10.net.minecraft.enchantment.EnumEnchantmentType[0];

    public CreativeTabs setRelevantEnchantmentTypes(legacyconnector.v1_7_10.net.minecraft.enchantment.EnumEnchantmentType... types) {
        relevantEnchantmentTypes = types;
        return this;
    }

    public legacyconnector.v1_7_10.net.minecraft.enchantment.EnumEnchantmentType[] func_111225_m() {
        return relevantEnchantmentTypes;
    }

    public static int getNextID() {
        return creativeTabArray.length;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public String getTabLabel() {
        return tabLabel;
    }

    public String getTranslatedTabLabel() {
        return StatCollector.translateToLocal("itemGroup." + getTabLabel());
    }

    public ItemStack getIconItemStack() {
        if (iconItemStack == null) {
            Item item = getTabIconItem();
            iconItemStack = new ItemStack(item == null ? Item.getItemById(1) : item, 1, getIconItemDamage());
        }
        return iconItemStack;
    }

    public abstract Item getTabIconItem();

    public int getIconItemDamage() {
        return 0;
    }

    public String getBackgroundImageName() {
        return backgroundImageName;
    }

    public CreativeTabs setBackgroundImageName(String name) {
        backgroundImageName = name;
        return this;
    }

    public boolean drawInForegroundOfTab() {
        return drawTitle;
    }

    public CreativeTabs setNoTitle() {
        drawTitle = false;
        return this;
    }

    public boolean shouldHidePlayerInventory() {
        return hasScrollbar;
    }

    public CreativeTabs setNoScrollbar() {
        hasScrollbar = false;
        return this;
    }

    public int getTabColumn() {
        return tabIndex > 11 ? ((tabIndex - 12) % 10) % 5 : tabIndex % 6;
    }

    public boolean isTabInFirstRow() {
        return tabIndex > 11 ? ((tabIndex - 12) % 10) < 5 : tabIndex < 6;
    }

    public int getTabPage() {
        return tabIndex > 11 ? ((tabIndex - 12) / 10) + 1 : 0;
    }

    public boolean hasSearchBar() {
        return tabIndex == 5;
    }

    /** Fills {@code list} with every item stack that should appear in this tab. */
    public void displayAllReleventItems(List list) {
        for (Object o : Item.itemRegistry) {
            Item item = (Item) o;
            if (item != null && item.getCreativeTab() == this) item.getSubItems(item, this, list);
        }
    }

    /** Modern vanilla tab id ({@code minecraft:building_blocks}) for the built-in legacy tabs. */
    public String mcc$vanillaTab() {
        return null;
    }

    static final class Vanilla extends CreativeTabs {
        private final String modern;

        Vanilla(int index, String label, String modern) {
            super(index, label);
            this.modern = modern;
        }

        @Override
        public Item getTabIconItem() {
            return null;
        }

        @Override
        public String mcc$vanillaTab() {
            return modern;
        }
    }
}
