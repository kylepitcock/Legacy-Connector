package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** 1.12.2 {@code ItemStack}: an item, a count, a damage/metadata value and a tag. Converted to the modern stack at the boundaries. */
public final class ItemStack {
    public static final ItemStack EMPTY = new ItemStack((Item) null);

    private Item item;
    private int stackSize;
    private int itemDamage;
    private NBTTagCompound stackTagCompound;
    /**
     * The modern stack this one is a live view of (a stack in a hand, an inventory slot, ...), or null. Count and damage changes are
     * written through at once, tag changes when the bridges flush the views after a legacy callback ({@link
     * org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122#flushViews}).
     */
    private net.minecraft.world.item.ItemStack mcc$backing;
    /** The custom data of {@link #mcc$backing} the tag was last read from. */
    private Object mcc$seenData;
    /** The capabilities the item attached to this stack ({@code Item.initCapabilities}), made on first use. */
    private legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider mcc$capabilities;
    private boolean mcc$capabilitiesMade;
    /** Forge's decimal format for attribute tooltips. */
    public static final java.text.DecimalFormat DECIMALFORMAT = new java.text.DecimalFormat("#.##");

    public ItemStack(Item item) {
        this(item, 1);
    }

    public ItemStack(Item item, int amount) {
        this(item, amount, 0);
    }

    public ItemStack(Item item, int amount, int meta) {
        this.item = item;
        this.stackSize = item == null ? 0 : amount;
        this.itemDamage = meta;
    }

    public ItemStack(legacyconnector.v1_12_2.net.minecraft.block.Block block) {
        this(block, 1);
    }

    public ItemStack(legacyconnector.v1_12_2.net.minecraft.block.Block block, int amount) {
        this(block, amount, 0);
    }

    public ItemStack(legacyconnector.v1_12_2.net.minecraft.block.Block block, int amount, int meta) {
        this(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.itemOf(block), amount, meta);
    }

    public ItemStack(NBTTagCompound tag) {
        this((Item) null);
        if (tag != null && tag.hasKey("id")) {
            var found = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ITEMS.getValue(
                    new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(tag.getString("id")));
            if (found != null) {
                this.item = found;
                this.stackSize = tag.getByte("Count");
                this.itemDamage = Math.max(0, tag.getShort("Damage"));
                if (tag.hasKey("tag", 10)) this.stackTagCompound = tag.getCompoundTag("tag");
            }
        }
    }

    public boolean isEmpty() {
        if (mcc$backing != null) stackSize = mcc$backing.getCount();
        return this == EMPTY || item == null || stackSize <= 0;
    }

    /** Binds this stack as a live view of a modern stack (see {@link #mcc$backing}). */
    public void mcc$bind(net.minecraft.world.item.ItemStack backing) {
        this.mcc$backing = backing;
        this.mcc$seenData = backing.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        this.mcc$seenTag = stackTagCompound == null ? null : stackTagCompound.mcc$modern().copy();
    }

    /** The tag as last read or written, so an unchanged tag is not written back. */
    private net.minecraft.nbt.CompoundTag mcc$seenTag;

    public net.minecraft.world.item.ItemStack mcc$backing() {
        return mcc$backing;
    }

    /** Re-reads the tag when modern code changed the backing stack's data since the last read; marks the view for flushing. */
    private void mcc$refreshTag() {
        var b = mcc$backing;
        if (b == null) return;
        var data = b.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != mcc$seenData) {
            mcc$seenData = data;
            stackTagCompound = data == null ? null : new NBTTagCompound(data.copyTag());
            mcc$seenTag = stackTagCompound == null ? null : stackTagCompound.mcc$modern().copy();
        }
        org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.touched(this);
    }

    /** Writes the tag of a view back into its modern stack. */
    public void mcc$commit() {
        var b = mcc$backing;
        if (b == null || b.isEmpty()) return;
        net.minecraft.nbt.CompoundTag now = stackTagCompound == null || stackTagCompound.isEmpty() ? null : stackTagCompound.mcc$modern();
        if (java.util.Objects.equals(now, mcc$seenTag == null || mcc$seenTag.isEmpty() ? null : mcc$seenTag)) return;
        mcc$seenTag = now == null ? null : now.copy();
        if (stackTagCompound == null || stackTagCompound.isEmpty()) b.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        else b.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(stackTagCompound.mcc$modern()));
        mcc$seenData = b.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
    }

    private void mcc$writeCount() {
        if (mcc$backing != null) mcc$backing.setCount(Math.max(0, stackSize));
    }

    /** The item of the stack; an empty stack has the air item (mods read the registry name of any stack's item). */
    public Item getItem() {
        return item != null ? item : legacyconnector.v1_12_2.net.minecraft.init.Items.AIR;
    }

    public int getCount() {
        return isEmpty() ? 0 : stackSize;
    }

    public void setCount(int count) {
        this.stackSize = count;
        mcc$changed();
        mcc$writeCount();
    }

    public void grow(int count) {
        if (mcc$backing != null) stackSize = mcc$backing.getCount();
        this.stackSize += count;
        mcc$changed();
        mcc$writeCount();
    }

    public void shrink(int count) {
        if (mcc$backing != null) stackSize = mcc$backing.getCount();
        this.stackSize -= count;
        mcc$changed();
        mcc$writeCount();
    }

    /**
     * Where a stack read from a modern inventory goes back to when it changes (a player's held item, an inventory slot): the
     * legacy code holds the very stack the slot had in 1.12.2 and shrinks or damages it in place.
     */
    private transient java.util.function.Consumer<ItemStack> mcc$sink;

    public ItemStack mcc$bind(java.util.function.Consumer<ItemStack> sink) {
        if (this != EMPTY) this.mcc$sink = sink;
        return this;
    }

    private void mcc$changed() {
        var sink = mcc$sink;
        if (sink != null) sink.accept(this);
    }

    public int getMetadata() {
        return mcc$rawDamage();
    }

    /** Forge: the item decides where a stack's damage lives ({@code Item.getDamage}). */
    public int getItemDamage() {
        return item == null ? mcc$rawDamage() : item.getDamage(this);
    }

    public void setItemDamage(int damage) {
        if (item == null) mcc$setRawDamage(damage);
        else item.setDamage(this, damage);
    }

    /** The stored damage/metadata value (read through to a view's modern stack for damageable items). */
    public int mcc$rawDamage() {
        var b = mcc$backing;
        if (b != null && b.isDamageableItem()) itemDamage = b.getDamageValue();
        return itemDamage;
    }

    public void mcc$setRawDamage(int damage) {
        this.itemDamage = damage;
        mcc$changed();
        var b = mcc$backing;
        if (b != null && b.isDamageableItem()) b.setDamageValue(Math.max(0, damage));
    }

    public int getMaxStackSize() {
        return item == null ? 64 : item.getItemStackLimit(this);
    }

    public int getMaxDamage() {
        return item == null ? 0 : item.getMaxDamage(this);
    }

    public boolean isItemStackDamageable() {
        return item != null && item.getMaxDamage(this) > 0;
    }

    public boolean isStackable() {
        return getMaxStackSize() > 1 && (!isItemStackDamageable() || !isItemDamaged());
    }

    public boolean isItemDamaged() {
        return isItemStackDamageable() && getItemDamage() > 0;
    }

    public boolean hasTagCompound() {
        mcc$refreshTag();
        return stackTagCompound != null;
    }

    public NBTTagCompound getTagCompound() {
        mcc$refreshTag();
        return stackTagCompound;
    }

    public void setTagCompound(NBTTagCompound tag) {
        this.stackTagCompound = tag;
        mcc$changed();
        if (mcc$backing != null) mcc$commit();
    }

    public void setTagInfo(String key, legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase value) {
        mcc$refreshTag();
        if (stackTagCompound == null) stackTagCompound = new NBTTagCompound();
        stackTagCompound.setTag(key, value);
        mcc$changed();
        if (mcc$backing != null) mcc$commit();
    }

    public NBTTagCompound getSubCompound(String key) {
        mcc$refreshTag();
        return stackTagCompound != null && stackTagCompound.hasKey(key, 10) ? stackTagCompound.getCompoundTag(key) : null;
    }

    public void removeSubCompound(String key) {
        mcc$refreshTag();
        if (stackTagCompound != null && stackTagCompound.hasKey(key, 10)) stackTagCompound.removeTag(key);
        if (mcc$backing != null) mcc$commit();
    }

    public boolean hasDisplayName() {
        NBTTagCompound display = getSubCompound("display");
        return display != null && display.hasKey("Name", 8);
    }

    public ItemStack setStackDisplayName(String name) {
        getOrCreateSubCompound("display").setString("Name", name);
        if (mcc$backing != null) mcc$commit();
        return this;
    }

    public void clearCustomName() {
        NBTTagCompound display = getSubCompound("display");
        if (display != null) {
            display.removeTag("Name");
            if (display.isEmpty()) removeSubCompound("display");
        }
        if (mcc$backing != null) mcc$commit();
    }

    public int getRepairCost() {
        NBTTagCompound tag = getTagCompound();
        return tag != null && tag.hasKey("RepairCost", 3) ? tag.getInteger("RepairCost") : 0;
    }

    public void setRepairCost(int cost) {
        mcc$refreshTag();
        if (stackTagCompound == null) stackTagCompound = new NBTTagCompound();
        stackTagCompound.setInteger("RepairCost", cost);
        if (mcc$backing != null) mcc$commit();
    }

    public EnumRarity getRarity() {
        return getItem().getRarity(this);
    }

    public boolean hasEffect() {
        return getItem().hasEffect(this);
    }

    public boolean isItemEnchantable() {
        return getItem().isEnchantable(this) && !isItemEnchanted();
    }

    public int getMaxItemUseDuration() {
        return getItem().getMaxItemUseDuration(this);
    }

    public EnumAction getItemUseAction() {
        return getItem().getItemUseAction(this);
    }

    public float getDestroySpeed(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return getItem().getDestroySpeed(this, state);
    }

    public boolean canHarvestBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return getItem().canHarvestBlock(state, this);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUse(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side, float hitX, float hitY,
            float hitZ) {
        return getItem().onItemUse(player, world, pos, hand, side, hitX, hitY, hitZ);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> useItemRightClick(legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return getItem().onItemRightClick(world, player, hand);
    }

    public ItemStack onItemUseFinish(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return getItem().onItemUseFinish(this, world, entity);
    }

    public void onBlockDestroyed(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        getItem().onBlockDestroyed(this, world, state, pos, player);
    }

    public void hitEntity(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        getItem().hitEntity(this, target, player);
    }

    public boolean interactWithEntity(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return getItem().itemInteractionForEntity(this, player, target, hand);
    }

    public void onCrafting(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, int amount) {
        getItem().onCreated(this, world, player);
    }

    public void updateAnimation(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, int slot,
            boolean selected) {
        getItem().onUpdate(this, world, entity, slot, selected);
    }

    public com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> getAttributeModifiers(
            legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot slot) {
        return getItem().getAttributeModifiers(slot, this);
    }

    public boolean isItemEqualIgnoreDurability(ItemStack other) {
        return !isItemStackDamageable() ? isItemEqual(other) : !other.isEmpty() && item == other.item;
    }

    public static boolean areItemsEqualIgnoreDurability(ItemStack a, ItemStack b) {
        if (a == b) return true;
        return !a.isEmpty() && !b.isEmpty() && a.isItemEqualIgnoreDurability(b);
    }

    public static boolean areItemStackShareTagsEqual(ItemStack a, ItemStack b) {
        return areItemStackTagsEqual(a, b);
    }

    /** Forge: equal as the client sees them (the share tag instead of the full tag). */
    public static boolean areItemStacksEqualUsingNBTShareTag(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        return !a.isEmpty() && !b.isEmpty() && a.getCount() == b.getCount() && a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage()
                && areItemStackShareTagsEqual(a, b);
    }

    /** Whether the capability data of two stacks match (Forge); stacks here carry none apart from their tag. */
    public boolean areCapsCompatible(ItemStack other) {
        return true;
    }

    /** The modern player behind a legacy entity, when it is one in creative mode. */
    private static boolean mcc$creative(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.player.Player p && p.getAbilities().instabuild;
    }

    /**
     * Damages the stack as 1.12 did: unbreaking may save the damage and a stack that breaks shrinks by one. A held view is damaged
     * through the modern stack (break animation, stats); creative players' stacks are not damaged.
     */
    public void damageItem(int amount, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        if (mcc$creative(entity) || !isItemStackDamageable()) return;
        var b = mcc$backing;
        if (b != null && entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l
                && l.level() instanceof net.minecraft.server.level.ServerLevel) {
            net.minecraft.world.entity.EquipmentSlot slot = l.getMainHandItem() == b ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                    : l.getOffhandItem() == b ? net.minecraft.world.entity.EquipmentSlot.OFFHAND : null;
            if (slot != null) {
                b.hurtAndBreak(amount, l, slot);
                return;
            }
        }
        if (attemptDamageItem(amount, new java.util.Random(), null)) {
            shrink(1);
            setItemDamage(0);
        }
    }

    /** Adds damage after unbreaking's chance to save each point; true when the stack broke. */
    public boolean attemptDamageItem(int amount, java.util.Random random, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player) {
        if (!isItemStackDamageable()) return false;
        if (amount > 0) {
            int unbreaking = legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                    legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment.getEnchantmentByLocation("unbreaking"), this);
            int saved = 0;
            for (int i = 0; unbreaking > 0 && i < amount; i++) {
                if (random.nextInt(unbreaking + 1) > 0) saved++;
            }
            amount -= saved;
            if (amount <= 0) return false;
        }
        setItemDamage(getItemDamage() + amount);
        return getItemDamage() > getMaxDamage();
    }

    public boolean hasCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<?> capability,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        var caps = mcc$capabilities();
        return caps != null && caps.hasCapability(capability, facing);
    }

    public <T> T getCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<T> capability,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        var caps = mcc$capabilities();
        return caps == null ? null : caps.getCapability(capability, facing);
    }

    /** The provider {@code Item.initCapabilities} attaches to the stack, made once per stack as Forge did. */
    private legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider mcc$capabilities() {
        if (!mcc$capabilitiesMade && !isEmpty()) {
            mcc$capabilitiesMade = true;
            try {
                NBTTagCompound tag = getTagCompound();
                mcc$capabilities = item.initCapabilities(this, tag != null && tag.hasKey("ForgeCaps", 10) ? tag.getCompoundTag("ForgeCaps") : null);
            } catch (RuntimeException | LinkageError e) {
                mcc$capabilities = null;
            }
        }
        return mcc$capabilities;
    }

    /** The display name as a translation key (display.LocName), translated where it is shown. */
    public ItemStack setTranslatableName(String key) {
        getOrCreateSubCompound("display").setString("LocName", key);
        return this;
    }

    /** The {@code ench} list: compounds of a numeric enchantment id and a level. */
    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList getEnchantmentTagList() {
        return stackTagCompound != null ? stackTagCompound.getTagList("ench", 10) : new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList();
    }

    public void addEnchantment(legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment enchantment, int level) {
        if (stackTagCompound == null) stackTagCompound = new NBTTagCompound();
        if (!stackTagCompound.hasKey("ench", 9)) stackTagCompound.setTag("ench", new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList());
        legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList list = stackTagCompound.getTagList("ench", 10);
        NBTTagCompound e = new NBTTagCompound();
        e.setShort("id", (short) legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment.getEnchantmentID(enchantment));
        e.setShort("lvl", (short) (byte) level);
        list.appendTag(e);
    }

    public NBTTagCompound getOrCreateSubCompound(String key) {
        mcc$refreshTag();
        if (stackTagCompound == null) stackTagCompound = new NBTTagCompound();
        if (!stackTagCompound.hasKey(key, 10)) stackTagCompound.setTag(key, new NBTTagCompound());
        return stackTagCompound.getCompoundTag(key);
    }

    public ItemStack copy() {
        if (isEmpty()) return EMPTY;
        mcc$refreshTag();
        getItemDamage();
        ItemStack copy = new ItemStack(item, stackSize, itemDamage);
        if (stackTagCompound != null) copy.stackTagCompound = (NBTTagCompound) stackTagCompound.copy();
        return copy;
    }

    public ItemStack splitStack(int amount) {
        int n = Math.min(amount, stackSize);
        ItemStack out = copy();
        out.setCount(n);
        shrink(n);
        return out;
    }

    public boolean isItemEqual(ItemStack other) {
        return !other.isEmpty() && item == other.item && itemDamage == other.itemDamage;
    }

    public static boolean areItemsEqual(ItemStack a, ItemStack b) {
        return a.isEmpty() ? b.isEmpty() : !b.isEmpty() && a.isItemEqual(b);
    }

    public static boolean areItemStackTagsEqual(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return a.stackTagCompound == null ? b.stackTagCompound == null : a.stackTagCompound.equals(b.stackTagCompound);
    }

    public static boolean areItemStacksEqual(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        return !a.isEmpty() && !b.isEmpty() && a.stackSize == b.stackSize && a.item == b.item && a.itemDamage == b.itemDamage && areItemStackTagsEqual(a, b);
    }

    public String getUnlocalizedName() {
        return item == null ? "" : item.getUnlocalizedName(this);
    }

    public String getTranslationKey() {
        return item == null ? "" : item.getTranslationKey(this);
    }

    public boolean getHasSubtypes() {
        return getItem().getHasSubtypes();
    }

    /** Whether the stack carries a non-empty {@code ench} list. */
    public boolean isItemEnchanted() {
        return stackTagCompound != null && stackTagCompound.hasKey("ench", 9) && stackTagCompound.getTagList("ench", 10).tagCount() > 0;
    }

    public String getDisplayName() {
        if (item == null) return "";
        String n = item.getItemStackDisplayName(this);
        return n != null ? n : getUnlocalizedName();
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        var name = item == null ? null : item.getRegistryName();
        tag.setString("id", name == null ? "minecraft:air" : name.toString());
        tag.setByte("Count", (byte) stackSize);
        tag.setShort("Damage", (short) itemDamage);
        if (stackTagCompound != null) tag.setTag("tag", stackTagCompound);
        return tag;
    }

    @Override
    public String toString() {
        return stackSize + "x" + (item == null ? "air" : item.getUnlocalizedName()) + "@" + itemDamage;
    }

    public NBTTagCompound serializeNBT() {
        return writeToNBT(new NBTTagCompound());
    }

    public void deserializeNBT(NBTTagCompound tag) {
        ItemStack parsed = new ItemStack(tag);
        this.item = parsed.item;
        this.stackSize = parsed.stackSize;
        this.itemDamage = parsed.itemDamage;
        this.stackTagCompound = parsed.stackTagCompound;
    }

    // ------------------------------------------------------------------ more 1.12.2 / Forge stack API

    /** Forge: a stack with capability data (item capabilities keep their data in the stack tag, so the data is not needed). */
    public ItemStack(Item item, int amount, int meta, NBTTagCompound capNBT) {
        this(item, amount, meta);
    }

    private int mcc$animationsToGo;

    public int getAnimationsToGo() {
        return mcc$animationsToGo;
    }

    public void setAnimationsToGo(int animations) {
        this.mcc$animationsToGo = animations;
    }

    /** The name as a chat component: "[name]", italic when renamed. */
    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getTextComponent() {
        var name = new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(getDisplayName());
        if (hasDisplayName()) name.getStyle().setItalic(true);
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString("[").appendSibling(name).appendText("]");
    }

    /** Stacks in item frames belong to modern entities; a legacy stack is never on one. */
    public boolean isOnItemFrame() {
        return false;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUseFirst(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn,
            legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ) {
        return getItem().onItemUseFirst(playerIn, worldIn, pos, side, hitX, hitY, hitZ, hand);
    }
}
