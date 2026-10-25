package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/** 1.12.2 {@code Item}: a registry entry with an unlocalized name, stack/damage limits and a creative tab. */
public class Item extends IForgeRegistryEntry.Impl<Item> {
    public static final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespaced<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, Item> REGISTRY = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ITEMS, "air");
    protected int maxStackSize = 64;
    private int maxDamage;
    private boolean hasSubtypes;
    private String unlocalizedName;
    private CreativeTabs tabToDisplayOn;
    private Item containerItem;
    protected boolean bFull3D;
    /** The modern item registered for this one (set by the bridge). */
    public Object mcc$modern;
    /** The modern vanilla item this one stands for, when it is a vanilla item. */
    public net.minecraft.world.item.Item mcc$vanillaModern;

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

    /** Tool tiers (an enum in 1.12.2; a class here so mods can add their own through EnumHelper). */
    public static final class ToolMaterial {
        public static final ToolMaterial WOOD = new ToolMaterial("WOOD", 0, 59, 2.0F, 0.0F, 15);
        public static final ToolMaterial STONE = new ToolMaterial("STONE", 1, 131, 4.0F, 1.0F, 5);
        public static final ToolMaterial IRON = new ToolMaterial("IRON", 2, 250, 6.0F, 2.0F, 14);
        public static final ToolMaterial DIAMOND = new ToolMaterial("DIAMOND", 3, 1561, 8.0F, 3.0F, 10);
        public static final ToolMaterial GOLD = new ToolMaterial("GOLD", 0, 32, 12.0F, 0.0F, 22);

        private final String name;
        private final int harvestLevel;
        private final int maxUses;
        private final float efficiency;
        private final float attackDamage;
        private final int enchantability;
        private ItemStack repairMaterial = ItemStack.EMPTY;

        public ToolMaterial(String name, int harvestLevel, int maxUses, float efficiency, float attackDamage, int enchantability) {
            this.name = name;
            this.harvestLevel = harvestLevel;
            this.maxUses = maxUses;
            this.efficiency = efficiency;
            this.attackDamage = attackDamage;
            this.enchantability = enchantability;
        }

        public int getMaxUses() {
            return maxUses;
        }

        public float getEfficiency() {
            return efficiency;
        }

        public float getAttackDamage() {
            return attackDamage;
        }

        public int getHarvestLevel() {
            return harvestLevel;
        }

        public int getEnchantability() {
            return enchantability;
        }

        public ToolMaterial setRepairItem(ItemStack stack) {
            this.repairMaterial = stack;
            return this;
        }

        public ItemStack getRepairItemStack() {
            return repairMaterial;
        }

        public Item getRepairItem() {
            return repairMaterial.getItem();
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static Item getItemFromBlock(legacyconnector.v1_12_2.net.minecraft.block.Block block) {
        Item item = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.itemOf(block);
        return item != null ? item : legacyconnector.v1_12_2.net.minecraft.init.Items.AIR;
    }

    public static Item getItemById(int id) {
        return REGISTRY.getObjectById(id);
    }

    public static int getIdFromItem(Item item) {
        return REGISTRY.getIDForObject(item);
    }

    public static Item getByNameOrId(String id) {
        Item item = REGISTRY.getObject(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(id));
        if (item != null) return item;
        try {
            return getItemById(Integer.parseInt(id));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
