package legacyconnector.v1_12_2.net.minecraft.creativetab;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** A creative tab: its label, icon and (for the search/vanilla tabs) nothing else; the bridge builds the modern tab from these. */
public class CreativeTabs {
    public static final java.util.List<CreativeTabs> CREATIVE_TAB_ARRAY = new java.util.ArrayList<>();
    public static final CreativeTabs BUILDING_BLOCKS = new CreativeTabs("buildingBlocks");
    public static final CreativeTabs DECORATIONS = new CreativeTabs("decorations");
    public static final CreativeTabs REDSTONE = new CreativeTabs("redstone");
    public static final CreativeTabs TRANSPORTATION = new CreativeTabs("transportation");
    public static final CreativeTabs MISC = new CreativeTabs("misc");
    public static final CreativeTabs FOOD = new CreativeTabs("food");
    public static final CreativeTabs TOOLS = new CreativeTabs("tools");
    public static final CreativeTabs COMBAT = new CreativeTabs("combat");
    public static final CreativeTabs BREWING = new CreativeTabs("brewing");
    public static final CreativeTabs MATERIALS = MISC;
    public static final CreativeTabs SEARCH = new CreativeTabs("search");

    private final int index;
    private final String tabLabel;
    private String backgroundTexture = "items.png";

    public CreativeTabs(String label) {
        this.tabLabel = label;
        synchronized (CREATIVE_TAB_ARRAY) {
            this.index = CREATIVE_TAB_ARRAY.size();
            CREATIVE_TAB_ARRAY.add(this);
        }
    }

    public int getTabIndex() {
        return index;
    }

    public String getTabLabel() {
        return tabLabel;
    }

    public String getTranslatedTabLabel() {
        return "itemGroup." + tabLabel;
    }

    /** Icon item: mods override one of these. */
    public ItemStack getTabIconItem() {
        return ItemStack.EMPTY;
    }

    public ItemStack getIconItemStack() {
        return getTabIconItem();
    }

    public CreativeTabs setBackgroundImageName(String name) {
        this.backgroundTexture = name;
        return this;
    }

    public String getBackgroundImageName() {
        return backgroundTexture;
    }

    public boolean hasSearchBar() {
        return false;
    }

    public void displayAllRelevantItems(NonNullList<ItemStack> items) {
        for (Item item : org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ITEMS.getValues()) {
            if (item.getCreativeTab() == this) item.getSubItems(this, items);
        }
    }
}
