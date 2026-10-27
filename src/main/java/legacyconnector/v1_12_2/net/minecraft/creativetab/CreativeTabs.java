package legacyconnector.v1_12_2.net.minecraft.creativetab;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** A creative tab: its label, icon and (for the search/vanilla tabs) nothing else; the bridge builds the modern tab from these. */
public class CreativeTabs {
    /** The tabs in creation order (1.12.2's CREATIVE_TAB_ARRAY, read through {@link #mcc$get$CREATIVE_TAB_ARRAY}). */
    public static final java.util.List<CreativeTabs> mcc$TABS = new java.util.ArrayList<>();
    /** Phantom field: reads are redirected to {@code mcc$get$CREATIVE_TAB_ARRAY} (a snapshot of the tabs as 1.12's array). */
    @Deprecated
    public static CreativeTabs[] CREATIVE_TAB_ARRAY;
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
        synchronized (mcc$TABS) {
            this.index = mcc$TABS.size();
            mcc$TABS.add(this);
        }
    }

    /** Forge's form: tabs are appended in creation order here, so the index is always the next free one. */
    public CreativeTabs(int index, String label) {
        this(label);
    }

    /** The index the next tab gets. */
    public static int getNextID() {
        synchronized (mcc$TABS) {
            return mcc$TABS.size();
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

    private boolean drawTitle = true;
    private boolean hasScrollbar = true;

    /** Hides the tab's title in the creative inventory (tabs with a title drawn into their background). */
    public CreativeTabs setNoTitle() {
        this.drawTitle = false;
        return this;
    }

    public boolean drawInForegroundOfTab() {
        return drawTitle;
    }

    public CreativeTabs setNoScrollbar() {
        this.hasScrollbar = false;
        return this;
    }

    public boolean shouldHidePlayerInventory() {
        return hasScrollbar;
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

    // ---- area F: names of the later 1.12 mappings, enchantment types, the array view

    public static CreativeTabs[] mcc$get$CREATIVE_TAB_ARRAY() {
        synchronized (mcc$TABS) {
            return mcc$TABS.toArray(new CreativeTabs[0]);
        }
    }

    public int getIndex() {
        return getTabIndex();
    }

    public String getTranslationKey() {
        return getTabLabel();
    }

    public ItemStack getIcon() {
        return getIconItemStack();
    }

    private legacyconnector.v1_12_2.net.minecraft.enchantment.EnumEnchantmentType[] mcc$enchantmentTypes =
            new legacyconnector.v1_12_2.net.minecraft.enchantment.EnumEnchantmentType[0];

    public legacyconnector.v1_12_2.net.minecraft.enchantment.EnumEnchantmentType[] getRelevantEnchantmentTypes() {
        return mcc$enchantmentTypes;
    }

    public CreativeTabs setRelevantEnchantmentTypes(legacyconnector.v1_12_2.net.minecraft.enchantment.EnumEnchantmentType... types) {
        this.mcc$enchantmentTypes = types == null ? new legacyconnector.v1_12_2.net.minecraft.enchantment.EnumEnchantmentType[0] : types;
        return this;
    }

    public boolean hasRelevantEnchantmentType(legacyconnector.v1_12_2.net.minecraft.enchantment.EnumEnchantmentType type) {
        if (type == null) return false;
        for (var t : mcc$enchantmentTypes) if (t == type) return true;
        return false;
    }
}
