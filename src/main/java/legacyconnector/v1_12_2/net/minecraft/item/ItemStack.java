package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** 1.12.2 {@code ItemStack}: an item, a count, a damage/metadata value and a tag. Converted to the modern stack at the boundaries. */
public final class ItemStack {
    public static final ItemStack EMPTY = new ItemStack((Item) null);

    private Item item;
    private int stackSize;
    private int itemDamage;
    private NBTTagCompound stackTagCompound;

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
        return this == EMPTY || item == null || stackSize <= 0;
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
    }

    public void grow(int count) {
        this.stackSize += count;
        mcc$changed();
    }

    public void shrink(int count) {
        this.stackSize -= count;
        mcc$changed();
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
        return itemDamage;
    }

    /** Forge: the damage as the item reports it ({@link Item#getDamage}, which reads the stored value unless overridden). */
    public int getItemDamage() {
        return item == null ? itemDamage : item.getDamage(this);
    }

    /** Forge: through {@link Item#setDamage} (which stores it, never below 0, unless overridden). */
    public void setItemDamage(int damage) {
        if (item == null) {
            this.itemDamage = damage;
            mcc$changed();
        } else {
            item.setDamage(this, damage);
        }
    }

    /** The stored damage/metadata value ({@link Item#getDamage}'s default). */
    public int mcc$rawDamage() {
        return itemDamage;
    }

    public void mcc$setRawDamage(int damage) {
        this.itemDamage = Math.max(0, damage);
        mcc$changed();
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
        return stackTagCompound != null;
    }

    public NBTTagCompound getTagCompound() {
        return stackTagCompound;
    }

    public void setTagCompound(NBTTagCompound tag) {
        this.stackTagCompound = tag;
        mcc$changed();
    }

    public void setTagInfo(String key, legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase value) {
        if (stackTagCompound == null) stackTagCompound = new NBTTagCompound();
        stackTagCompound.setTag(key, value);
        mcc$changed();
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
        if (stackTagCompound == null) stackTagCompound = new NBTTagCompound();
        if (!stackTagCompound.hasKey(key, 10)) stackTagCompound.setTag(key, new NBTTagCompound());
        return stackTagCompound.getCompoundTag(key);
    }

    public ItemStack copy() {
        if (isEmpty()) return EMPTY;
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
        NBTTagCompound display = getSubCompound("display");
        if (display != null) {
            if (display.hasKey("Name", 8)) return display.getString("Name");
            if (display.hasKey("LocName", 8)) return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(display.getString("LocName"));
        }
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

    // ------------------------------------------------------------------ 1.12.2 / Forge stack API (names, damage, use, capabilities)

    public static final java.text.DecimalFormat DECIMALFORMAT = new java.text.DecimalFormat("#.##");

    /** Forge: a stack with capability data (restored into its capabilities when they are first asked for). */
    public ItemStack(Item item, int amount, int meta, NBTTagCompound capNBT) {
        this(item, amount, meta);
        this.mcc$capNBT = capNBT;
    }

    private NBTTagCompound mcc$capNBT;
    private legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider[] mcc$caps;
    private int animationsToGo;

    /** The item's own capability provider and those attached by AttachCapabilitiesEvent, made on first use. */
    private legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider[] mcc$caps() {
        var caps = mcc$caps;
        if (caps != null) return caps;
        java.util.List<legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider> list = new java.util.ArrayList<>();
        if (item != null) {
            try {
                var own = item.initCapabilities(this, mcc$capNBT);
                if (own != null) list.add(own);
            } catch (RuntimeException | LinkageError e) {
                // an item whose capabilities cannot be built has none
            }
            try {
                var event = new legacyconnector.v1_12_2.net.minecraftforge.event.AttachCapabilitiesEvent<>(ItemStack.class, this);
                legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
                list.addAll(event.getCapabilities().values());
            } catch (RuntimeException | LinkageError e) {
                // no attached capabilities
            }
        }
        caps = list.toArray(new legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider[0]);
        mcc$caps = caps;
        return caps;
    }

    public boolean hasCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<?> capability,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        if (isEmpty() || capability == null) return false;
        for (var c : mcc$caps()) if (c.hasCapability(capability, facing)) return true;
        return false;
    }

    public <T> T getCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<T> capability,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        if (isEmpty() || capability == null) return null;
        for (var c : mcc$caps()) {
            T t = c.getCapability(capability, facing);
            if (t != null) return t;
        }
        return null;
    }

    /** Forge: capabilities are compatible when neither stack carries any (item capabilities keep their data in the stack tag). */
    public boolean areCapsCompatible(ItemStack other) {
        return this == other || mcc$caps().length == 0 && other.mcc$caps().length == 0 || item == other.item;
    }

    /** Uses up durability (Unbreaking may spare some); a stack that breaks shrinks by one. Creative players take no damage. */
    public void damageItem(int amount, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entityIn) {
        if (entityIn instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer p
                && legacyconnector.v1_12_2.net.minecraft.entity.player.PlayerCapabilities.mcc$get$isCreativeMode(
                        legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.mcc$get$capabilities(p))) {
            return;
        }
        if (!isItemStackDamageable()) return;
        java.util.Random rand = entityIn == null ? new java.util.Random() : entityIn.getRNG();
        if (attemptDamageItem(amount, rand, entityIn instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP mp ? mp : null)) {
            if (entityIn != null && entityIn.mcc$modern() instanceof net.minecraft.world.entity.LivingEntity living) {
                living.level().playSound(null, living.getX(), living.getY(), living.getZ(), net.minecraft.sounds.SoundEvents.ITEM_BREAK,
                        living.getSoundSource(), 0.8F, 0.8F + living.level().random.nextFloat() * 0.4F);
            }
            shrink(1);
            mcc$setRawDamage(0);
        }
    }

    /** Applies damage (Unbreaking may negate each point); true when the stack is now broken. */
    public boolean attemptDamageItem(int amount, java.util.Random rand, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP damager) {
        if (!isItemStackDamageable()) return false;
        if (amount > 0) {
            int i = legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(legacyconnector.v1_12_2.net.minecraft.init.Enchantments.UNBREAKING, this);
            int j = 0;
            for (int k = 0; i > 0 && k < amount; ++k) {
                boolean negate = !(getItem() instanceof ItemArmor && rand.nextFloat() < 0.6F) && rand.nextInt(i + 1) > 0;
                if (negate) ++j;
            }
            amount -= j;
            if (amount <= 0) return false;
        }
        setItemDamage(getItemDamage() + amount);
        return getItemDamage() > getMaxDamage();
    }

    public boolean hasDisplayName() {
        NBTTagCompound display = getSubCompound("display");
        return display != null && display.hasKey("Name", 8);
    }

    public ItemStack setStackDisplayName(String displayName) {
        getOrCreateSubCompound("display").setString("Name", displayName);
        mcc$changed();
        return this;
    }

    public void clearCustomName() {
        NBTTagCompound display = getSubCompound("display");
        if (display != null) {
            display.removeTag("Name");
            if (display.isEmpty()) {
                removeSubCompound("display");
                if (stackTagCompound != null && stackTagCompound.isEmpty()) stackTagCompound = null;
            }
        }
        mcc$changed();
    }

    public NBTTagCompound getSubCompound(String key) {
        return stackTagCompound != null && stackTagCompound.hasKey(key, 10) ? stackTagCompound.getCompoundTag(key) : null;
    }

    public void removeSubCompound(String key) {
        if (stackTagCompound != null && stackTagCompound.hasKey(key, 10)) {
            stackTagCompound.removeTag(key);
            mcc$changed();
        }
    }

    public int getMaxItemUseDuration() {
        return getItem().getMaxItemUseDuration(this);
    }

    public EnumAction getItemUseAction() {
        return getItem().getItemUseAction(this);
    }

    public boolean hasEffect() {
        return getItem().hasEffect(this);
    }

    public boolean isItemEnchantable() {
        return getItem().isEnchantable(this) && !isItemEnchanted();
    }

    public EnumRarity getRarity() {
        return getItem().getRarity(this);
    }

    public boolean isItemEqualIgnoreDurability(ItemStack stack) {
        return !isItemStackDamageable() ? isItemEqual(stack) : !stack.isEmpty() && item == stack.item;
    }

    public static boolean areItemsEqualIgnoreDurability(ItemStack stackA, ItemStack stackB) {
        if (stackA == stackB) return true;
        return !stackA.isEmpty() && !stackB.isEmpty() && stackA.isItemEqualIgnoreDurability(stackB);
    }

    /** Forge: equal as the client sees them (the share tag instead of the full tag). */
    public static boolean areItemStacksEqualUsingNBTShareTag(ItemStack stackA, ItemStack stackB) {
        if (stackA.isEmpty()) return stackB.isEmpty();
        return !stackB.isEmpty() && stackA.stackSize == stackB.stackSize && stackA.item == stackB.item && stackA.getItemDamage() == stackB.getItemDamage()
                && areItemStackShareTagsEqual(stackA, stackB);
    }

    public static boolean areItemStackShareTagsEqual(ItemStack stackA, ItemStack stackB) {
        NBTTagCompound a = stackA.getItem().getNBTShareTag(stackA), b = stackB.getItem().getNBTShareTag(stackB);
        return a == null ? b == null : a.equals(b);
    }

    /** The modifiers the stack's tag lists for the slot, else the item's own. */
    public com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> getAttributeModifiers(
            legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot equipmentSlot) {
        if (hasTagCompound() && stackTagCompound.hasKey("AttributeModifiers", 9)) {
            com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> out = com.google.common.collect.HashMultimap.create();
            var list = stackTagCompound.getTagList("AttributeModifiers", 10);
            for (int i = 0; i < list.tagCount(); ++i) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                if (tag.hasKey("Slot", 8) && !tag.getString("Slot").equals(equipmentSlot.getName())) continue;
                java.util.UUID id = new java.util.UUID(tag.getLong("UUIDMost"), tag.getLong("UUIDLeast"));
                if (id.getLeastSignificantBits() == 0L && id.getMostSignificantBits() == 0L) continue;
                out.put(tag.getString("AttributeName"), new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier(id,
                        tag.getString("Name"), tag.getDouble("Amount"), tag.getInteger("Operation")));
            }
            return out;
        }
        return getItem().getAttributeModifiers(equipmentSlot, this);
    }

    /** The name as a chat component: "[name]", italic when renamed. */
    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getTextComponent() {
        var name = new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(getDisplayName());
        if (hasDisplayName()) name.getStyle().setItalic(true);
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString("[").appendSibling(name).appendText("]");
    }

    /** Stacks in item frames are modern entities' stacks; a legacy stack is never on one. */
    public boolean isOnItemFrame() {
        return false;
    }

    public boolean interactWithEntity(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entityIn, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return getItem().itemInteractionForEntity(this, playerIn, entityIn, hand);
    }

    public void onCrafting(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn, int amount) {
        getItem().onCreated(this, worldIn, playerIn);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUse(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn,
            legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ) {
        return getItem().onItemUse(playerIn, worldIn, pos, hand, side, hitX, hitY, hitZ);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUseFirst(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn,
            legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ) {
        return getItem().onItemUseFirst(playerIn, worldIn, pos, side, hitX, hitY, hitZ, hand);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> useItemRightClick(legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return getItem().onItemRightClick(worldIn, playerIn, hand);
    }

    public ItemStack onItemUseFinish(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entityLiving) {
        return getItem().onItemUseFinish(this, worldIn, entityLiving);
    }

    public void onBlockDestroyed(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState blockIn,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn) {
        getItem().onBlockDestroyed(this, worldIn, blockIn, pos, playerIn);
    }

    public void hitEntity(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entityIn, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn) {
        getItem().hitEntity(this, entityIn, playerIn);
    }

    public float getDestroySpeed(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState blockIn) {
        return getItem().getDestroySpeed(this, blockIn);
    }

    public boolean canHarvestBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState blockIn) {
        return getItem().canHarvestBlock(blockIn, this);
    }

    public int getRepairCost() {
        return hasTagCompound() && stackTagCompound.hasKey("RepairCost", 3) ? stackTagCompound.getInteger("RepairCost") : 0;
    }

    public void setRepairCost(int cost) {
        if (stackTagCompound == null) stackTagCompound = new NBTTagCompound();
        stackTagCompound.setInteger("RepairCost", cost);
        mcc$changed();
    }

    public int getAnimationsToGo() {
        return animationsToGo;
    }

    public void setAnimationsToGo(int animations) {
        this.animationsToGo = animations;
    }

    public void updateAnimation(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn,
            int inventorySlot, boolean isCurrentItem) {
        if (animationsToGo > 0) --animationsToGo;
        if (item != null) item.onUpdate(this, worldIn, entityIn, inventorySlot, isCurrentItem);
    }
}
