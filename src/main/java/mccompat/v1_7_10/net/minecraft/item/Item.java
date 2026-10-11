package mccompat.v1_7_10.net.minecraft.item;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import mccompat.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.util.IIcon;
import mccompat.v1_7_10.net.minecraft.util.RegistryNamespaced;
import mccompat.v1_7_10.net.minecraft.util.StatCollector;
import mccompat.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge;

/**
 * The 1.7.10 Item API. Like blocks, a legacy item is logic only: {@link ItemBridge} registers one modern item per
 * known subtype (flattening metadata the way vanilla did in 1.13) and forwards use/tooltip/tick callbacks here.
 */
public class Item {
    public static final RegistryNamespaced itemRegistry = ItemBridge.legacyRegistry();
    protected static final UUID field_111210_e = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    protected static Random itemRand = new Random();

    protected int maxStackSize = 64;
    private int maxDamage;
    protected boolean bFull3D;
    protected boolean hasSubtypes;
    private Item containerItem;
    private String potionEffect;
    private String unlocalizedName;
    protected IIcon itemIcon;
    protected String iconString;
    private CreativeTabs tabToDisplayOn;
    protected boolean canRepair = true;

    /** Modern items registered for this legacy item, by metadata; filled by {@link ItemBridge}. */
    public final java.util.Map<Integer, net.minecraft.world.item.Item> mcc$variants = new java.util.TreeMap<>();

    /** Non-null for wrappers of vanilla (or modern-only) items. */
    public final org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo mcc$vanilla;

    public Item() {
        this.mcc$vanilla = null;
    }

    /** Wrapper constructor for vanilla items. */
    public Item(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        this.mcc$vanilla = info;
        this.unlocalizedName = info.legacyName();
        this.iconString = info.legacyName();
        ItemBridge.initVanilla(this, info);
    }

    /** Lets the bridge copy modern properties into a vanilla wrapper. */
    public void mcc$setVanillaProperties(int stackSize, int maxDamage, boolean subtypes) {
        this.maxStackSize = stackSize;
        this.maxDamage = maxDamage;
        this.hasSubtypes = subtypes;
    }

    // ------------------------------------------------------------------ static lookups

    public static int getIdFromItem(Item item) {
        return ItemBridge.idOf(item);
    }

    public static Item getItemById(int id) {
        return ItemBridge.byId(id);
    }

    public static Item getItemFromBlock(Block block) {
        return ItemBridge.fromBlock(block);
    }

    public static void registerItems() {
    }

    // ------------------------------------------------------------------ builder-style setters

    public Item setMaxStackSize(int size) {
        this.maxStackSize = size;
        return this;
    }

    public Item setHasSubtypes(boolean subtypes) {
        this.hasSubtypes = subtypes;
        return this;
    }

    public Item setMaxDamage(int damage) {
        this.maxDamage = damage;
        return this;
    }

    public Item setUnlocalizedName(String name) {
        this.unlocalizedName = name;
        return this;
    }

    public Item setTextureName(String name) {
        this.iconString = name;
        return this;
    }

    public Item setCreativeTab(CreativeTabs tab) {
        this.tabToDisplayOn = tab;
        return this;
    }

    public Item setFull3D() {
        this.bFull3D = true;
        return this;
    }

    public Item setContainerItem(Item item) {
        this.containerItem = item;
        return this;
    }

    public Item setPotionEffect(String effect) {
        this.potionEffect = effect;
        return this;
    }

    public Item setNoRepair() {
        this.canRepair = false;
        return this;
    }

    // ------------------------------------------------------------------ getters

    public int getItemStackLimit() {
        return maxStackSize;
    }

    public int getItemStackLimit(ItemStack stack) {
        return getItemStackLimit();
    }

    public boolean getHasSubtypes() {
        return hasSubtypes;
    }

    public int getMaxDamage() {
        return maxDamage;
    }

    public int getMaxDamage(ItemStack stack) {
        return getMaxDamage();
    }

    public boolean isDamageable() {
        return maxDamage > 0 && !hasSubtypes;
    }

    public boolean isDamaged(ItemStack stack) {
        return stack.getItemDamage() > 0;
    }

    public int getDamage(ItemStack stack) {
        return stack.mcc$rawDamage();
    }

    public void setDamage(ItemStack stack, int damage) {
        stack.mcc$setRawDamage(Math.max(0, damage));
    }

    public int getMetadata(int damage) {
        return 0;
    }

    public String getUnlocalizedName() {
        return "item." + unlocalizedName;
    }

    public String getUnlocalizedName(ItemStack stack) {
        return "item." + unlocalizedName;
    }

    public String getUnlocalizedNameInefficiently(ItemStack stack) {
        String s = getUnlocalizedName(stack);
        return s == null ? "" : StatCollector.translateToLocal(s);
    }

    public String getItemStackDisplayName(ItemStack stack) {
        return StatCollector.translateToLocal(getUnlocalizedNameInefficiently(stack) + ".name").trim();
    }

    protected String getIconString() {
        return iconString == null ? "MISSING_ICON_ITEM_" + getIdFromItem(this) + "_" + unlocalizedName : iconString;
    }

    public CreativeTabs getCreativeTab() {
        return tabToDisplayOn;
    }

    public CreativeTabs[] getCreativeTabs() {
        return new CreativeTabs[] {getCreativeTab()};
    }

    public Item getContainerItem() {
        return containerItem;
    }

    public boolean hasContainerItem() {
        return containerItem != null;
    }

    public ItemStack getContainerItem(ItemStack stack) {
        return hasContainerItem() ? new ItemStack(getContainerItem()) : null;
    }

    public boolean hasContainerItem(ItemStack stack) {
        return hasContainerItem();
    }

    public boolean doesContainerItemLeaveCraftingGrid(ItemStack stack) {
        return true;
    }

    public String getPotionEffect(ItemStack stack) {
        return potionEffect;
    }

    public boolean isPotionIngredient(ItemStack stack) {
        return potionEffect != null;
    }

    public boolean isFull3D() {
        return bFull3D;
    }

    public boolean shouldRotateAroundWhenRendering() {
        return false;
    }

    public int getItemEnchantability() {
        return 0;
    }

    public boolean isItemTool(ItemStack stack) {
        return getItemStackLimit() == 1 && isDamageable();
    }

    public boolean isMap() {
        return false;
    }

    public boolean getShareTag() {
        return true;
    }

    public boolean canItemEditBlocks() {
        return false;
    }

    public boolean isRepairable() {
        return canRepair && isDamageable();
    }

    public boolean getIsRepairable(ItemStack toRepair, ItemStack material) {
        return false;
    }

    public EnumRarity getRarity(ItemStack stack) {
        return stack.isItemEnchanted() ? EnumRarity.rare : EnumRarity.common;
    }

    public boolean hasEffect(ItemStack stack) {
        return stack.isItemEnchanted();
    }

    public boolean hasEffect(ItemStack stack, int pass) {
        return hasEffect(stack) && (pass == 0 || this != mccompat.v1_7_10.net.minecraft.init.Items.mcc$potion());
    }

    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.none;
    }

    public int getMaxItemUseDuration(ItemStack stack) {
        return 0;
    }

    public float getStrVsBlock(ItemStack stack, Block block) {
        return 1.0F;
    }

    public float getDigSpeed(ItemStack stack, Block block, int meta) {
        return getStrVsBlock(stack, block);
    }

    public boolean canHarvestBlock(Block block, ItemStack stack) {
        return canItemHarvestBlock(block);
    }

    public boolean canItemHarvestBlock(Block block) {
        return false;
    }

    public int getHarvestLevel(ItemStack stack, String toolClass) {
        return -1;
    }

    public java.util.Set<String> getToolClasses(ItemStack stack) {
        return java.util.Collections.emptySet();
    }

    // ------------------------------------------------------------------ rendering (client)

    public IIcon getIconFromDamage(int damage) {
        return itemIcon;
    }

    public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        return getIconFromDamage(damage);
    }

    public final IIcon getIconIndex(ItemStack stack) {
        return getIconFromDamage(stack.getItemDamage());
    }

    public IIcon getIcon(ItemStack stack, int pass) {
        return getIconFromDamageForRenderPass(stack.getItemDamage(), pass);
    }

    public boolean requiresMultipleRenderPasses() {
        return false;
    }

    public int getRenderPasses(int metadata) {
        return requiresMultipleRenderPasses() ? 2 : 1;
    }

    public int getColorFromItemStack(ItemStack stack, int pass) {
        return 16777215;
    }

    public int getSpriteNumber() {
        return 1;
    }

    public void registerIcons(IIconRegister register) {
        this.itemIcon = register.registerIcon(getIconString());
    }

    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
    }

    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
    }

    // ------------------------------------------------------------------ gameplay callbacks

    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return false;
    }

    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return false;
    }

    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        return stack;
    }

    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        return stack;
    }

    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int itemInUseCount) {
    }

    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
    }

    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isCurrentItem) {
    }

    public void onCreated(ItemStack stack, World world, EntityPlayer player) {
    }

    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        return false;
    }

    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase entity) {
        return false;
    }

    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity) {
        return false;
    }

    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        return false;
    }

    public boolean onDroppedByPlayer(ItemStack item, EntityPlayer player) {
        return true;
    }

    public boolean onEntityItemUpdate(mccompat.v1_7_10.net.minecraft.entity.item.EntityItem entityItem) {
        return false;
    }

    public int getEntityLifespan(ItemStack stack, World world) {
        return 6000;
    }

    @Override
    public String toString() {
        return "LegacyItem{" + unlocalizedName + "}";
    }

    /** Tool tiers. Mods add their own through {@code EnumHelper.addToolMaterial}. */
    public enum ToolMaterial {
        WOOD(0, 59, 2.0F, 0.0F, 15),
        STONE(1, 131, 4.0F, 1.0F, 5),
        IRON(2, 250, 6.0F, 2.0F, 14),
        EMERALD(3, 1561, 8.0F, 3.0F, 10),
        GOLD(0, 32, 12.0F, 0.0F, 22);

        private final int harvestLevel;
        private final int maxUses;
        private final float efficiencyOnProperMaterial;
        private final float damageVsEntity;
        private final int enchantability;
        /** Forge: repair item of mod materials. */
        public Item customCraftingMaterial = null;

        ToolMaterial(int harvestLevel, int maxUses, float efficiency, float damage, int enchantability) {
            this.harvestLevel = harvestLevel;
            this.maxUses = maxUses;
            this.efficiencyOnProperMaterial = efficiency;
            this.damageVsEntity = damage;
            this.enchantability = enchantability;
        }

        public int getMaxUses() {
            return maxUses;
        }

        public float getEfficiencyOnProperMaterial() {
            return efficiencyOnProperMaterial;
        }

        public float getDamageVsEntity() {
            return damageVsEntity;
        }

        public int getHarvestLevel() {
            return harvestLevel;
        }

        public int getEnchantability() {
            return enchantability;
        }

        public Item getBaseItemForRepair() {
            // No switch: EnumHelper appends constants after javac's switch map was built.
            if (this == WOOD) return Item.getItemFromBlock(mccompat.v1_7_10.net.minecraft.init.Blocks.planks);
            if (this == STONE) return Item.getItemFromBlock(mccompat.v1_7_10.net.minecraft.init.Blocks.cobblestone);
            if (this == IRON) return mccompat.v1_7_10.net.minecraft.init.Items.iron_ingot;
            if (this == EMERALD) return mccompat.v1_7_10.net.minecraft.init.Items.diamond;
            if (this == GOLD) return mccompat.v1_7_10.net.minecraft.init.Items.gold_ingot;
            return customCraftingMaterial;
        }
    }
}
