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

    /** Forge's stack-aware check (buckets keep their bucket, damaged tools themselves); the item-wide one by default. */
    public boolean hasContainerItem(ItemStack stack) {
        return hasContainerItem();
    }

    public ItemStack getContainerItem(ItemStack stack) {
        if (!hasContainerItem(stack)) return ItemStack.EMPTY;
        return new ItemStack(getContainerItem());
    }

    /** Forge: the capabilities a new stack of this item carries (fluid handlers of tanks and buckets, energy, ...); none by default. */
    public legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack,
            legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound nbt) {
        return null;
    }

    /** The tabs this item shows in (Forge: mods override it to list an item in several tabs). */
    public CreativeTabs[] getCreativeTabs() {
        return new CreativeTabs[] {getCreativeTab()};
    }

    private final java.util.Map<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, IItemPropertyGetter> properties = new java.util.LinkedHashMap<>();

    /** A model property (bow pull, compass angle, ...) that item model overrides select on. */
    public void addPropertyOverride(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation key, IItemPropertyGetter getter) {
        synchronized (properties) {
            properties.put(key, getter);
        }
    }

    public IItemPropertyGetter getPropertyGetter(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation key) {
        synchronized (properties) {
            return properties.get(key);
        }
    }

    public boolean hasCustomProperties() {
        synchronized (properties) {
            return !properties.isEmpty();
        }
    }

    /** The registered model properties (client model bridging reads them). */
    public java.util.Map<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, IItemPropertyGetter> mcc$properties() {
        synchronized (properties) {
            return new java.util.LinkedHashMap<>(properties);
        }
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
        for (CreativeTabs t : getCreativeTabs()) if (t != null && t == targetTab) return true;
        CreativeTabs tab = getCreativeTab();
        return tab != null && (targetTab == CreativeTabs.SEARCH || targetTab == tab);
    }

    public String getItemStackDisplayName(ItemStack stack) {
        return null;
    }

    /** The enchantment glint: enchanted stacks have it. */
    public boolean hasEffect(ItemStack stack) {
        return stack.isItemEnchanted();
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

    /** Enchanted stacks are rare, others common. */
    public EnumRarity getRarity(ItemStack stack) {
        return stack.isItemEnchanted() ? EnumRarity.RARE : EnumRarity.COMMON;
    }

    public int getItemEnchantability() {
        return 0;
    }

    public boolean isEnchantable(ItemStack stack) {
        return getItemStackLimit(stack) == 1 && isDamageable();
    }

    // ------------------------------------------------------------------ 1.12.2 / Forge item behaviour (defaults mods override and call)

    protected static java.util.Random itemRand = new java.util.Random();
    protected static final java.util.UUID ATTACK_DAMAGE_MODIFIER = java.util.UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    protected static final java.util.UUID ATTACK_SPEED_MODIFIER = java.util.UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    /** Right-click in the air (or on a block that did nothing): passes with the held stack. */
    public legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> onItemRightClick(legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn, legacyconnector.v1_12_2.net.minecraft.util.EnumHand handIn) {
        return new legacyconnector.v1_12_2.net.minecraft.util.ActionResult<>(legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS, playerIn.getHeldItem(handIn));
    }

    /** Right-click on a block. */
    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUse(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ) {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
    }

    /** Forge: right-click on a block before the block itself is activated. */
    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUseFirst(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
    }

    /** Every tick while the stack is in an entity's inventory. */
    public void onUpdate(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn,
            int itemSlot, boolean isSelected) {
    }

    /** When the stack is crafted or smelted by a player. */
    public void onCreated(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn) {
    }

    /** When the use duration runs out (eating, drinking): the stack that remains. */
    public ItemStack onItemUseFinish(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entityLiving) {
        return stack;
    }

    /** When the player lets go of right-click before the use finished (bows). */
    public void onPlayerStoppedUsing(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entityLiving, int timeLeft) {
    }

    /** Forge: every tick while the item is being used. */
    public void onUsingTick(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase player, int count) {
    }

    public int getMaxItemUseDuration(ItemStack stack) {
        return 0;
    }

    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;
    }

    /** Forge: the attribute modifiers of the stack in that slot (the item-wide ones by default). */
    public com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> getAttributeModifiers(
            legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot slot, ItemStack stack) {
        return getItemAttributeModifiers(slot);
    }

    public com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> getItemAttributeModifiers(
            legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot equipmentSlot) {
        return com.google.common.collect.HashMultimap.create();
    }

    /** Forge: the part of the tag sent to clients (all of it by default). */
    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound getNBTShareTag(ItemStack stack) {
        return stack.getTagCompound();
    }

    public void readNBTShareTag(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound nbt) {
        stack.setTagCompound(nbt);
    }

    /** Whether the second stack repairs the first in an anvil. */
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return false;
    }

    public String getUnlocalizedNameInefficiently(ItemStack stack) {
        String s = getUnlocalizedName(stack);
        return s == null ? "" : s;
    }

    public boolean isShield(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.SHIELD;
    }

    /** Forge: the damage of a stack (its stored value by default; mods keep it elsewhere by overriding both). */
    public int getDamage(ItemStack stack) {
        return stack.mcc$rawDamage();
    }

    public void setDamage(ItemStack stack, int damage) {
        stack.mcc$setRawDamage(damage);
    }

    public boolean isDamaged(ItemStack stack) {
        return getDamage(stack) > 0;
    }

    public int getMaxDamage(ItemStack stack, boolean unused) {
        return getMaxDamage(stack);
    }

    /** Forge: the mod that made the stack (the registry namespace). */
    public String getCreatorModId(ItemStack stack) {
        var name = getRegistryName();
        return name == null ? null : name.getNamespace();
    }

    public boolean showDurabilityBar(ItemStack stack) {
        return stack.isItemDamaged();
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        return (double) stack.getItemDamage() / (double) stack.getMaxDamage();
    }

    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return net.minecraft.util.Mth.hsvToRgb(Math.max(0.0F, (float) (1.0F - getDurabilityForDisplay(stack))) / 3.0F, 1.0F, 1.0F);
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment enchantment) {
        return enchantment.type != null && enchantment.type.canEnchantItem(stack.getItem());
    }

    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    public int getItemEnchantability(ItemStack stack) {
        return getItemEnchantability();
    }

    /** Whether the item can harvest the block regardless of the block's tool (shears on webs). */
    public boolean canHarvestBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState blockIn) {
        return false;
    }

    public boolean canHarvestBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, ItemStack stack) {
        return canHarvestBlock(state);
    }

    public float getDestroySpeed(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return 1.0F;
    }

    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    /** When the item hits an entity; true counts as a use (vanilla tools damage themselves here). */
    public boolean hitEntity(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase attacker) {
        return false;
    }

    public boolean onBlockDestroyed(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entityLiving) {
        return false;
    }

    /** Forge: before the player attacks an entity with the item; true cancels the attack. */
    public boolean onLeftClickEntity(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return false;
    }

    /** Forge: before a block is broken with the item; true cancels the break. */
    public boolean onBlockStartBreak(ItemStack itemstack, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return false;
    }

    /** Right-click on a living entity. */
    public boolean itemInteractionForEntity(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return false;
    }

    public boolean onEntitySwing(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entityLiving, ItemStack stack) {
        return false;
    }

    /** Forge: every tick while worn as armour. */
    public void onArmorTick(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, ItemStack itemStack) {
    }

    /** Forge: how long a dropped stack lasts (5 minutes). */
    public int getEntityLifespan(ItemStack itemStack, legacyconnector.v1_12_2.net.minecraft.world.World world) {
        return 6000;
    }

    public boolean hasCustomEntity(ItemStack stack) {
        return false;
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.Entity createEntity(legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.entity.Entity location, ItemStack itemstack) {
        return null;
    }

    /** Forge: the armour slot the stack goes in, or null for "decide by item type". */
    public legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot getEquipmentSlot(ItemStack stack) {
        return null;
    }

    public boolean isValidArmor(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot armorType,
            legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        var own = getEquipmentSlot(stack);
        if (own != null) return own == armorType;
        if (this instanceof ItemArmor armor) return armor.armorType == armorType;
        return armorType == legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.HEAD
                && (this == legacyconnector.v1_12_2.net.minecraft.init.Items.SKULL || this instanceof ItemBlock b
                        && b.getBlock() == legacyconnector.v1_12_2.net.minecraft.init.Blocks.PUMPKIN);
    }

    public String getArmorTexture(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity,
            legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot slot, String type) {
        return null;
    }

    public boolean canDisableShield(ItemStack stack, ItemStack shield, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase attacker) {
        return this instanceof ItemAxe;
    }

    public float getSmeltingExperience(ItemStack item) {
        return -1.0F;
    }

    /** Forge: before the player drops the stack; false keeps it. */
    public boolean onDroppedByPlayer(ItemStack item, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return true;
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.areItemStacksEqual(oldStack, newStack);
    }

    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !(newStack.getItem() == oldStack.getItem() && ItemStack.areItemStackTagsEqual(newStack, oldStack)
                && (newStack.isItemStackDamageable() || newStack.getMetadata() == oldStack.getMetadata()));
    }

    public boolean shouldRotateAroundWhenRendering() {
        return false;
    }

    public boolean isBeaconPayment(ItemStack stack) {
        return this == legacyconnector.v1_12_2.net.minecraft.init.Items.EMERALD || this == legacyconnector.v1_12_2.net.minecraft.init.Items.DIAMOND
                || this == legacyconnector.v1_12_2.net.minecraft.init.Items.GOLD_INGOT || this == legacyconnector.v1_12_2.net.minecraft.init.Items.IRON_INGOT;
    }

    public boolean isPotionIngredient(ItemStack stack) {
        return false;
    }

    public int getItemBurnTime(ItemStack itemStack) {
        return -1;
    }
}
