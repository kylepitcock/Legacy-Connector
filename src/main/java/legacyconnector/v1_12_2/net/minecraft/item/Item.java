package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/** 1.12.2 {@code Item}: a registry entry with an unlocalized name, stack/damage limits and a creative tab. */
public class Item extends IForgeRegistryEntry.Impl<Item> {
    protected int maxStackSize = 64;
    private int maxDamage;
    private boolean hasSubtypes;
    private String unlocalizedName;
    private CreativeTabs tabToDisplayOn;
    private Item containerItem;
    protected boolean bFull3D;
    /** The modern item registered for this one (set by the bridge). */
    public Object mcc$modern;

    public Item() {
    }

    public Item setUnlocalizedName(String name) {
        this.unlocalizedName = name;
        return this;
    }

    public String getUnlocalizedName() {
        return "item." + unlocalizedName;
    }

    public String getUnlocalizedName(ItemStack stack) {
        return getUnlocalizedName();
    }

    public String getTranslationKey() {
        return getUnlocalizedName();
    }

    public Item setMaxStackSize(int size) {
        this.maxStackSize = size;
        return this;
    }

    public int getItemStackLimit() {
        return maxStackSize;
    }

    public int getItemStackLimit(ItemStack stack) {
        return maxStackSize;
    }

    public Item setMaxDamage(int max) {
        this.maxDamage = max;
        return this;
    }

    public int getMaxDamage() {
        return maxDamage;
    }

    public int getMaxDamage(ItemStack stack) {
        return maxDamage;
    }

    public boolean isDamageable() {
        return maxDamage > 0 && !hasSubtypes;
    }

    public Item setHasSubtypes(boolean has) {
        this.hasSubtypes = has;
        return this;
    }

    public boolean getHasSubtypes() {
        return hasSubtypes;
    }

    public Item setCreativeTab(CreativeTabs tab) {
        this.tabToDisplayOn = tab;
        return this;
    }

    public CreativeTabs getCreativeTab() {
        return tabToDisplayOn;
    }

    public Item setContainerItem(Item item) {
        this.containerItem = item;
        return this;
    }

    public Item getContainerItem() {
        return containerItem;
    }

    public boolean hasContainerItem() {
        return containerItem != null;
    }

    public Item setFull3D() {
        this.bFull3D = true;
        return this;
    }

    public boolean isFull3D() {
        return bFull3D;
    }

    /** The stacks the creative inventory lists for this item: one, or one per subtype for items with subtypes (override). */
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) items.add(new ItemStack(this));
    }

    protected boolean isInCreativeTab(CreativeTabs targetTab) {
        CreativeTabs tab = getCreativeTab();
        return tab != null && (targetTab == CreativeTabs.SEARCH || targetTab == tab);
    }

    public String getItemStackDisplayName(ItemStack stack) {
        return null;
    }

    public boolean hasEffect(ItemStack stack) {
        return false;
    }
}
