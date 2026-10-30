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

    // ------------------------------------------------------------------ use and gameplay hooks (1.12.2 defaults; mods override)

    /** The random items roll (drops, sounds). */
    protected static final java.util.Random itemRand = new java.util.Random();
    protected static final java.util.UUID ATTACK_DAMAGE_MODIFIER = java.util.UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    protected static final java.util.UUID ATTACK_SPEED_MODIFIER = java.util.UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    /** The stack a legacy player holds in a hand, as a live view. */
    protected static ItemStack mcc$held(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        if (player == null || !(player.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l)) return ItemStack.EMPTY;
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.view(l.getItemInHand(
                hand == legacyconnector.v1_12_2.net.minecraft.util.EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND));
    }

    public legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> onItemRightClick(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return new legacyconnector.v1_12_2.net.minecraft.util.ActionResult<>(legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS, mcc$held(player, hand));
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUse(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ) {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
    }

    /** Forge: called before the block is activated. */
    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUseFirst(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side,
            float hitX, float hitY, float hitZ, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
    }

    public ItemStack onItemUseFinish(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return stack;
    }

    public void onPlayerStoppedUsing(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity, int timeLeft) {
    }

    /** Forge: each tick while the item is being used. */
    public void onUsingTick(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity, int count) {
    }

    public int getMaxItemUseDuration(ItemStack stack) {
        return 0;
    }

    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;
    }

    /** Each tick while the stack is in an entity's inventory. */
    public void onUpdate(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, int itemSlot, boolean isSelected) {
    }

    public void onCreated(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
    }

    public boolean hitEntity(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase attacker) {
        return false;
    }

    public boolean onBlockDestroyed(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return false;
    }

    public boolean itemInteractionForEntity(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return false;
    }

    /** Forge: true cancels the block break. */
    public boolean onBlockStartBreak(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return false;
    }

    /** Forge: true cancels the attack. */
    public boolean onLeftClickEntity(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return false;
    }

    public boolean onEntitySwing(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity, ItemStack stack) {
        return false;
    }

    public boolean onDroppedByPlayer(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return true;
    }

    public float getDestroySpeed(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return 1.0F;
    }

    public boolean canHarvestBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return false;
    }

    /** Forge's stack-aware form. */
    public boolean canHarvestBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, ItemStack stack) {
        return canHarvestBlock(state);
    }

    /** The attribute modifiers the item gives in a slot (swords and tools add damage and speed). */
    @SuppressWarnings("deprecation")
    public com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> getItemAttributeModifiers(legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot slot) {
        return com.google.common.collect.HashMultimap.create();
    }

    public com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> getAttributeModifiers(legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot slot,
            ItemStack stack) {
        return getItemAttributeModifiers(slot);
    }

    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return false;
    }

    public boolean isShield(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        var name = stack.getItem().getRegistryName();
        return name != null && "minecraft:shield".equals(name.toString());
    }

    public boolean canDisableShield(ItemStack stack, ItemStack shield, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase attacker) {
        return this instanceof ItemAxe;
    }

    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment enchantment) {
        return true;
    }

    /** Forge: the damage of a stack (its metadata unless the item stores it elsewhere). */
    public int getDamage(ItemStack stack) {
        return stack.mcc$rawDamage();
    }

    public void setDamage(ItemStack stack, int damage) {
        stack.mcc$setRawDamage(Math.max(0, damage));
    }

    public boolean isDamaged(ItemStack stack) {
        return getDamage(stack) > 0;
    }

    public boolean showDurabilityBar(ItemStack stack) {
        return stack.isItemDamaged();
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        int max = stack.getMaxDamage();
        return max <= 0 ? 0.0D : (double) stack.getItemDamage() / (double) max;
    }

    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return java.awt.Color.HSBtoRGB(Math.max(0.0F, (float) (1.0D - getDurabilityForDisplay(stack))) / 3.0F, 1.0F, 1.0F);
    }

    public String getUnlocalizedNameInefficiently(ItemStack stack) {
        String name = getUnlocalizedName(stack);
        return name == null ? "" : name;
    }

    /** Forge: the tag sent to clients. */
    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound getNBTShareTag(ItemStack stack) {
        return stack.getTagCompound();
    }

    public boolean getShareTag() {
        return true;
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.areItemStacksEqual(oldStack, newStack);
    }

    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !(newStack.getItem() == oldStack.getItem() && ItemStack.areItemStackTagsEqual(newStack, oldStack)
                && (newStack.isItemStackDamageable() || newStack.getMetadata() == oldStack.getMetadata()));
    }

    /** Forge: burn time in a furnace, -1 for the vanilla rules. */
    public int getItemBurnTime(ItemStack stack) {
        return -1;
    }

    public int getEntityLifespan(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World world) {
        return 6000;
    }

    public boolean hasCustomEntity(ItemStack stack) {
        return false;
    }

    public float getXpRepairRatio(ItemStack stack) {
        return 2.0F;
    }

    public float getSmeltingExperience(ItemStack stack) {
        return -1.0F;
    }

    public boolean isBeaconPayment(ItemStack stack) {
        return false;
    }

    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
        return ItemStack.areItemsEqualIgnoreDurability(oldStack, newStack);
    }

    public String getCreatorModId(ItemStack stack) {
        var name = getRegistryName();
        return name == null ? "minecraft" : name.getNamespace();
    }

    public boolean isValidArmor(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot armorType, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return false;
    }

    public legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot getEquipmentSlot(ItemStack stack) {
        return null;
    }

    public boolean isPotionIngredient(ItemStack stack) {
        return false;
    }

    // ------------------------------------------------------------------ more Forge item hooks (defaults)

    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    public int getItemEnchantability(ItemStack stack) {
        return getItemEnchantability();
    }

    public void readNBTShareTag(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound nbt) {
        stack.setTagCompound(nbt);
    }

    /** Forge: every tick while worn as armour. */
    public void onArmorTick(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, ItemStack itemStack) {
    }

    /** Forge: the entity a dropped stack becomes when {@link #hasCustomEntity} says so (none by default). */
    public legacyconnector.v1_12_2.net.minecraft.entity.Entity createEntity(legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.entity.Entity location, ItemStack itemstack) {
        return null;
    }

    public String getArmorTexture(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity,
            legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot slot, String type) {
        return null;
    }

    public boolean shouldRotateAroundWhenRendering() {
        return false;
    }

    /**
     * The block the player looks at within reach (fluids too when asked: buckets), or null when the ray hits nothing; through the
     * modern ray clip from the player's eyes.
     */
    protected legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult rayTrace(legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn, boolean useLiquids) {
        var player = playerIn == null ? null : playerIn.mcc$player();
        var level = worldIn == null ? null : worldIn.mcc$level();
        if (player == null || level == null) return null;
        var eye = player.getEyePosition();
        var end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.OUTLINE,
                useLiquids ? net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY : net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return null;
        return legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult.mcc$of(hit);
    }

    /** Tooltip lines under the name (client; a server never asks). */
    public void addInformation(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.List<String> tooltip,
            legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag flagIn) {
    }

    // ------------------------------------------------------------------ client hooks and more Forge item hooks

    private legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer teisr;

    /** Forge: the renderer of items whose model is "builtin/entity" (chests, shields, mod items drawn in code). */
    public void setTileEntityItemStackRenderer(legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer renderer) {
        this.teisr = renderer;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer getTileEntityItemStackRenderer() {
        var r = teisr;
        return r != null ? r : legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer.instance;
    }

    /** Forge: the font the tooltip of a stack is drawn with; null is the game's font. */
    public legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer getFontRenderer(ItemStack stack) {
        return null;
    }

    /** Forge: the rarity (name colour) of a stack, vanilla's or a mod's own. */
    public legacyconnector.v1_12_2.net.minecraftforge.common.IRarity getForgeRarity(ItemStack stack) {
        return getRarity(stack);
    }

    /** Forge: the model an armor item is drawn with on an entity; null draws the default armor model. */
    public legacyconnector.v1_12_2.net.minecraft.client.model.ModelBiped getArmorModel(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entityLiving,
            ItemStack itemStack, legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot armorSlot,
            legacyconnector.v1_12_2.net.minecraft.client.model.ModelBiped _default) {
        return null;
    }

    /** Forge: each tick of the item entity of a dropped stack; true skips the entity's own update. */
    public boolean onEntityItemUpdate(legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem entityItem) {
        return false;
    }

    /** Forge: whether a sneaking player still activates the block clicked with this item (wrenches and the like). */
    public boolean doesSneakBypassUse(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return false;
    }

    /** Forge: whether a creative player breaks blocks with this item (swords do not). */
    public boolean canDestroyBlockInCreative(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return !(this instanceof ItemSword);
    }

    /** Called when a stack's tag arrives from an old save; true when the item changed it. */
    public boolean updateItemStackNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound nbt) {
        return false;
    }

    /** Whether the item is a map (sent its map data while held). */
    public boolean isMap() {
        return false;
    }

    /** Right-click in the air with the modern vanilla item's behaviour (buckets, bottles, maps...): its result stack for the hand. */
    protected static legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> mcc$useModern(net.minecraft.world.item.Item vanilla,
            legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        ItemStack held = mcc$held(player, hand);
        var p = player == null ? null : player.mcc$player();
        var level = world == null ? null : world.mcc$level();
        if (p == null || level == null) return new legacyconnector.v1_12_2.net.minecraft.util.ActionResult<>(legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS, held);
        var r = vanilla.use(level, p, hand == legacyconnector.v1_12_2.net.minecraft.util.EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND
                : net.minecraft.world.InteractionHand.MAIN_HAND);
        var type = r.getResult().consumesAction() ? legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.SUCCESS
                : r.getResult() == net.minecraft.world.InteractionResult.FAIL ? legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.FAIL
                : legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
        var out = r.getObject();
        var inHand = p.getItemInHand(hand == legacyconnector.v1_12_2.net.minecraft.util.EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND
                : net.minecraft.world.InteractionHand.MAIN_HAND);
        ItemStack result = out == inHand ? org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.view(out)
                : org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(out);
        return new legacyconnector.v1_12_2.net.minecraft.util.ActionResult<>(type, result);
    }

    /** Use on a block with the modern vanilla item's behaviour (placing armor stands, snow, filling bottles...). */
    protected static legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult mcc$useOnModern(net.minecraft.world.item.Item vanilla,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ) {
        var p = player == null ? null : player.mcc$player();
        var level = world == null ? null : world.mcc$level();
        if (p == null || level == null || pos == null) return legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
        var mp = pos.mcc$modern();
        var dir = facing == null ? net.minecraft.core.Direction.UP : facing.mcc$modern();
        var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(mp.getX() + hitX, mp.getY() + hitY, mp.getZ() + hitZ), dir, mp, false);
        var mh = hand == legacyconnector.v1_12_2.net.minecraft.util.EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND;
        var r = vanilla.useOn(new net.minecraft.world.item.context.UseOnContext(level, p, mh, p.getItemInHand(mh), hit));
        if (r.consumesAction()) return legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.SUCCESS;
        return r == net.minecraft.world.InteractionResult.FAIL ? legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.FAIL
                : legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
    }
}
