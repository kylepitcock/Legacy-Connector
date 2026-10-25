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
        private static final java.util.List<ToolMaterial> ALL = new java.util.ArrayList<>();
        private final int ordinal;

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
            this.ordinal = ALL.size();
            ALL.add(this);
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

        public static ToolMaterial[] values() {
            synchronized (ALL) {
                return ALL.toArray(new ToolMaterial[0]);
            }
        }

        /** {@code Enum.compareTo}: legacy calls taking an Enum are linked here. */
        public int compareTo(Object other) {
            return Integer.compare(ordinal(), ((ToolMaterial) other).ordinal());
        }

        public int ordinal() {
            return ordinal;
        }

        public static ToolMaterial valueOf(String name) {
            for (ToolMaterial v : values()) if (v.name().equals(name)) return v;
            throw new IllegalArgumentException("No enum constant " + name);
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

    public Item setTranslationKey(String name) {
        return setUnlocalizedName(name);
    }

    public String getTranslationKey(ItemStack stack) {
        return getUnlocalizedName(stack);
    }

    public Item setNoRepair() {
        this.canRepair = false;
        return this;
    }

    private boolean canRepair = true;
    private final java.util.Map<String, Integer> toolClasses = new java.util.HashMap<>();

    public boolean isRepairable() {
        return canRepair && isDamageable();
    }

    public void setHarvestLevel(String toolClass, int level) {
        toolClasses.put(toolClass, level);
    }

    public int getHarvestLevel(ItemStack stack, String toolClass, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState blockState) {
        Integer level = toolClasses.get(toolClass);
        return level == null ? -1 : level;
    }

    public java.util.Set<String> getToolClasses(ItemStack stack) {
        return toolClasses.keySet();
    }

    public int getMetadata(int damage) {
        return 0;
    }

    public int getMetadata(ItemStack stack) {
        return getMetadata(stack.getItemDamage());
    }

    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.COMMON;
    }

    public int getItemEnchantability() {
        return 0;
    }

    public boolean isEnchantable(ItemStack stack) {
        return getItemStackLimit(stack) == 1 && isDamageable();
    }
}
