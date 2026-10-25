package legacyconnector.v1_7_10.net.minecraft.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge;

/**
 * Legacy item stack as a <em>live view</em> over a modern {@link net.minecraft.world.item.ItemStack}.
 *
 * <p>1.7.10 code mutates stacks through public fields ({@code stack.stackSize--}). Those fields are declared here
 * so legacy code links, but the loader rewrites every access to the {@code mcc$get$/mcc$set$} accessors below,
 * which read and write the modern stack - so decrementing a stack taken from a player's inventory really
 * decrements it. Shim code itself must use {@link #mcc$count()} / {@link #mcc$setCount(int)}.</p>
 *
 * <p>Metadata/damage: for damageable items the legacy damage is the modern {@code DAMAGE} component; for items
 * with subtypes it selects the flattened modern item variant (or the {@code legacyconnector:legacy_meta} component for
 * subtypes that were never registered as variants). NBT lives in the {@code CUSTOM_DATA} component.</p>
 */
public final class ItemStack {
    /** The "#.###" format vanilla uses for attribute values in tooltips (mods reflect it by its SRG name). */
    public static final java.text.DecimalFormat field_111284_a = new java.text.DecimalFormat("#.###");

    /** Phantom: accesses are redirected to {@link #mcc$get$stackSize}. */
    @Deprecated
    public int stackSize;
    /** Phantom: accesses are redirected to {@link #mcc$get$stackTagCompound}. */
    @Deprecated
    public NBTTagCompound stackTagCompound;
    /** Phantom: accesses are redirected to {@link #mcc$get$itemDamage}. */
    @Deprecated
    public int itemDamage;
    public int animationsToGo;

    private net.minecraft.world.item.ItemStack mcc$stack;
    private Item mcc$item;
    private NBTTagCompound mcc$tagView;
    private CustomData mcc$tagSource;

    public ItemStack(Block block) {
        this(block, 1);
    }

    public ItemStack(Block block, int count) {
        this(block, count, 0);
    }

    public ItemStack(Block block, int count, int meta) {
        this(Item.getItemFromBlock(block), count, meta);
    }

    public ItemStack(Item item) {
        this(item, 1);
    }

    public ItemStack(Item item, int count) {
        this(item, count, 0);
    }

    public ItemStack(Item item, int count, int meta) {
        this.mcc$item = item;
        this.mcc$stack = ItemBridge.toModern(item, count, meta);
    }

    private ItemStack(net.minecraft.world.item.ItemStack modern, Item legacy) {
        this.mcc$stack = modern;
        this.mcc$item = legacy;
    }

    /** Wraps a modern stack without copying. Returns null for empty stacks (legacy code expects null). */
    public static ItemStack mcc$wrap(net.minecraft.world.item.ItemStack modern) {
        if (modern == null || modern.isEmpty()) return null;
        return new ItemStack(modern, ItemBridge.legacyItemOf(modern));
    }

    /** Modern stack backing this view (shared, not a copy). */
    public net.minecraft.world.item.ItemStack mcc$modern() {
        return mcc$stack;
    }

    public static net.minecraft.world.item.ItemStack mcc$toModern(ItemStack legacy) {
        return legacy == null ? net.minecraft.world.item.ItemStack.EMPTY : legacy.mcc$stack;
    }

    // ------------------------------------------------------------------ virtual fields

    public static int mcc$get$stackSize(ItemStack s) {
        return s.mcc$count();
    }

    public static void mcc$set$stackSize(ItemStack s, int value) {
        s.mcc$setCount(value);
    }

    public static int mcc$get$itemDamage(ItemStack s) {
        return s.getItemDamage();
    }

    public static void mcc$set$itemDamage(ItemStack s, int value) {
        s.setItemDamage(value);
    }

    public static NBTTagCompound mcc$get$stackTagCompound(ItemStack s) {
        return s.getTagCompound();
    }

    public static void mcc$set$stackTagCompound(ItemStack s, NBTTagCompound tag) {
        s.setTagCompound(tag);
    }

    public int mcc$count() {
        return mcc$stack.getCount();
    }

    public void mcc$setCount(int count) {
        if (count <= 0 && mcc$stack.isEmpty()) return;
        mcc$stack.setCount(count);
    }

    // ------------------------------------------------------------------ identity

    public Item getItem() {
        return mcc$item;
    }

    public void setItem(Item item) {
        this.mcc$item = item;
        this.mcc$stack = ItemBridge.toModern(item, mcc$count(), getItemDamage());
    }

    public int getItemDamage() {
        return ItemBridge.legacyDamage(mcc$item, mcc$stack);
    }

    public int getItemDamageForDisplay() {
        return getItemDamage();
    }

    public void setItemDamage(int damage) {
        mcc$stack = ItemBridge.withLegacyDamage(mcc$item, mcc$stack, damage);
    }

    public int mcc$rawDamage() {
        return mcc$stack.getDamageValue();
    }

    public void mcc$setRawDamage(int damage) {
        mcc$stack.setDamageValue(damage);
    }

    public int getMaxDamage() {
        return mcc$item == null ? 0 : mcc$item.getMaxDamage(this);
    }

    public boolean isItemStackDamageable() {
        return mcc$item != null && mcc$item.getMaxDamage() > 0 && (!mcc$item.getHasSubtypes());
    }

    public boolean isItemDamaged() {
        return isItemStackDamageable() && getItemDamage() > 0;
    }

    public boolean getHasSubtypes() {
        return mcc$item != null && mcc$item.getHasSubtypes();
    }

    public int getMaxStackSize() {
        return mcc$item == null ? 64 : mcc$item.getItemStackLimit(this);
    }

    public boolean isStackable() {
        return getMaxStackSize() > 1 && (!isItemStackDamageable() || !isItemDamaged());
    }

    public boolean attemptDamageItem(int amount, Random random) {
        if (!isItemStackDamageable()) return false;
        int damage = getItemDamage() + amount;
        setItemDamage(damage);
        return damage > getMaxDamage();
    }

    public void damageItem(int amount, EntityLivingBase entity) {
        if (entity instanceof EntityPlayer player && player.capabilities.mcc$creative()) return;
        if (isItemStackDamageable() && attemptDamageItem(amount, entity.getRNG())) {
            entity.renderBrokenItemStack(this);
            mcc$setCount(mcc$count() - 1);
            if (mcc$count() < 0) mcc$setCount(0);
            setItemDamage(0);
        }
    }

    // ------------------------------------------------------------------ NBT

    /**
     * 1.7.10 kept a stack's tag apart from its item. A stack with no usable modern item (the item is null or was never
     * registered) is empty in the modern game and cannot hold components, so a tag set on it is kept here.
     */
    private NBTTagCompound mcc$looseTag;

    public boolean hasTagCompound() {
        // As 1.7.10: a stack whose tag was assigned an (empty) compound has one. Code such as
        // "if (!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();" relies on it.
        return mcc$stack.get(DataComponents.CUSTOM_DATA) != null || (mcc$stack.isEmpty() && mcc$looseTag != null);
    }

    public NBTTagCompound getTagCompound() {
        CustomData data = mcc$stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            if (mcc$stack.isEmpty() && mcc$looseTag != null) return mcc$looseTag;
            mcc$tagView = null;
            mcc$tagSource = null;
            return null;
        }
        if (mcc$tagView == null || mcc$tagSource != data) {
            NBTTagCompound view = new NBTTagCompound(data.copyTag());
            view.mcc$setChangeListener(() -> mcc$writeTag(view));
            mcc$tagView = view;
            mcc$tagSource = data;
        }
        return mcc$tagView;
    }

    public void setTagCompound(NBTTagCompound tag) {
        if (tag == null) {
            mcc$stack.remove(DataComponents.CUSTOM_DATA);
            mcc$tagView = null;
            mcc$tagSource = null;
            mcc$looseTag = null;
            return;
        }
        if (mcc$stack.isEmpty()) {
            mcc$looseTag = tag;
            return;
        }
        tag.mcc$setChangeListener(() -> mcc$writeTag(tag));
        mcc$tagView = tag;
        mcc$writeTag(tag);
    }

    private void mcc$writeTag(NBTTagCompound tag) {
        CustomData data = CustomData.of(tag.mcc$modern());
        mcc$stack.set(DataComponents.CUSTOM_DATA, data);
        mcc$tagSource = mcc$stack.get(DataComponents.CUSTOM_DATA);
    }

    public void setTagInfo(String key, NBTBase value) {
        NBTTagCompound tag = getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            setTagCompound(tag);
        }
        tag.setTag(key, value);
    }

    public NBTTagList getEnchantmentTagList() {
        NBTTagCompound tag = getTagCompound();
        if (tag != null && tag.hasKey("ench", 9)) return tag.getTagList("ench", 10);
        return ItemBridge.legacyEnchantments(mcc$stack);
    }

    /** Legacy {@code ench} tag entry; custom enchantments have no modern equivalent, so the tag is the source of truth. */
    public void addEnchantment(legacyconnector.v1_7_10.net.minecraft.enchantment.Enchantment enchantment, int level) {
        NBTTagCompound tag = getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            setTagCompound(tag);
        }
        if (!tag.hasKey("ench", 9)) tag.setTag("ench", new NBTTagList());
        NBTTagList list = tag.getTagList("ench", 10);
        NBTTagCompound entry = new NBTTagCompound();
        entry.setShort("id", (short) enchantment.effectId);
        entry.setShort("lvl", (short) level);
        list.appendTag(entry);
        tag.setTag("ench", list);
        setTagCompound(tag);
    }

    public boolean isItemEnchanted() {
        NBTTagList list = getEnchantmentTagList();
        return mcc$stack.isEnchanted() || (list != null && list.tagCount() > 0);
    }

    public boolean isItemEnchantable() {
        return mcc$stack.isEnchantable();
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        ItemBridge.writeLegacyNBT(this, nbt);
        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        ItemStack read = loadItemStackFromNBT(nbt);
        if (read != null) {
            this.mcc$item = read.mcc$item;
            this.mcc$stack = read.mcc$stack;
            this.mcc$tagView = null;
        }
    }

    public static ItemStack loadItemStackFromNBT(NBTTagCompound nbt) {
        return ItemBridge.readLegacyNBT(nbt);
    }

    // ------------------------------------------------------------------ names, display

    public String getUnlocalizedName() {
        return mcc$item == null ? "item.null" : mcc$item.getUnlocalizedName(this);
    }

    public String getDisplayName() {
        if (mcc$stack.has(DataComponents.CUSTOM_NAME)) return mcc$stack.getHoverName().getString();
        return mcc$item == null ? "" : mcc$item.getItemStackDisplayName(this);
    }

    public ItemStack setStackDisplayName(String name) {
        mcc$stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return this;
    }

    public void func_135074_t() {
        mcc$stack.remove(DataComponents.CUSTOM_NAME);
    }

    public boolean hasDisplayName() {
        return mcc$stack.has(DataComponents.CUSTOM_NAME);
    }

    public EnumRarity getRarity() {
        return mcc$item == null ? EnumRarity.common : mcc$item.getRarity(this);
    }

    public boolean hasEffect() {
        return mcc$item != null && mcc$item.hasEffect(this);
    }

    public IIcon getIconIndex() {
        return mcc$item == null ? null : mcc$item.getIconIndex(this);
    }

    public int getItemSpriteNumber() {
        return mcc$item == null ? 0 : mcc$item.getSpriteNumber();
    }

    public List<String> getTooltip(EntityPlayer player, boolean advanced) {
        List<String> list = new ArrayList<>();
        list.add(getDisplayName());
        if (mcc$item != null) mcc$item.addInformation(this, player, list, advanced);
        return list;
    }

    public int getRepairCost() {
        Integer cost = mcc$stack.get(DataComponents.REPAIR_COST);
        return cost == null ? 0 : cost;
    }

    public void setRepairCost(int cost) {
        mcc$stack.set(DataComponents.REPAIR_COST, cost);
    }

    // ------------------------------------------------------------------ behaviour delegation

    public boolean tryPlaceItemIntoWorld(EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return mcc$item != null && mcc$item.onItemUse(this, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    public float getStrVsBlock(Block block) {
        return mcc$item == null ? 1.0F : mcc$item.getStrVsBlock(this, block);
    }

    public ItemStack useItemRightClick(World world, EntityPlayer player) {
        return mcc$item == null ? this : mcc$item.onItemRightClick(this, world, player);
    }

    public ItemStack onFoodEaten(World world, EntityPlayer player) {
        return mcc$item == null ? this : mcc$item.onEaten(this, world, player);
    }

    public void onPlayerStoppedUsing(World world, EntityPlayer player, int count) {
        if (mcc$item != null) mcc$item.onPlayerStoppedUsing(this, world, player, count);
    }

    public EnumAction getItemUseAction() {
        return mcc$item == null ? EnumAction.none : mcc$item.getItemUseAction(this);
    }

    public int getMaxItemUseDuration() {
        return mcc$item == null ? 0 : mcc$item.getMaxItemUseDuration(this);
    }

    public void hitEntity(EntityLivingBase target, EntityPlayer attacker) {
        if (mcc$item != null && mcc$item.hitEntity(this, target, attacker)) attacker.mcc$addStat("useItem");
    }

    public boolean interactWithEntity(EntityPlayer player, EntityLivingBase entity) {
        return mcc$item != null && mcc$item.itemInteractionForEntity(this, player, entity);
    }

    public void onCrafting(World world, EntityPlayer player, int amount) {
        if (mcc$item != null) mcc$item.onCreated(this, world, player);
    }

    public void updateAnimation(World world, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, int slot, boolean current) {
        if (animationsToGo > 0) animationsToGo--;
        if (mcc$item != null) mcc$item.onUpdate(this, world, entity, slot, current);
    }

    // ------------------------------------------------------------------ copies and comparisons

    public ItemStack copy() {
        return new ItemStack(mcc$stack.copy(), mcc$item);
    }

    public static ItemStack copyItemStack(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    public ItemStack splitStack(int amount) {
        net.minecraft.world.item.ItemStack split = mcc$stack.copyWithCount(Math.min(amount, Math.max(mcc$count(), 0)));
        mcc$setCount(mcc$count() - split.getCount());
        return new ItemStack(split, mcc$item);
    }

    public boolean isItemEqual(ItemStack other) {
        return other != null && mcc$item == other.mcc$item && getItemDamage() == other.getItemDamage();
    }

    public boolean isItemStackEqual(ItemStack other) {
        return other != null && mcc$count() == other.mcc$count() && isItemEqual(other) && areItemStackTagsEqual(this, other);
    }

    public static boolean areItemStacksEqual(ItemStack a, ItemStack b) {
        return a == null && b == null || a != null && b != null && a.isItemStackEqual(b);
    }

    public static boolean areItemStackTagsEqual(ItemStack a, ItemStack b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        CustomData x = a.mcc$stack.get(DataComponents.CUSTOM_DATA);
        CustomData y = b.mcc$stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tx = x == null ? null : x.copyTag();
        CompoundTag ty = y == null ? null : y.copyTag();
        return tx == null ? ty == null || ty.isEmpty() : tx.equals(ty) || tx.isEmpty() && ty == null;
    }

    public boolean canItemHarvestBlock(Block block) {
        return mcc$item != null && mcc$item.canHarvestBlock(block, this);
    }

    @Override
    public String toString() {
        return mcc$count() + "x" + (mcc$item == null ? "null" : mcc$item.getUnlocalizedName()) + "@" + getItemDamage();
    }

    public boolean hasEffect(int pass) {
        return mcc$item != null && mcc$item.hasEffect(this, pass);
    }

    public com.google.common.collect.Multimap getAttributeModifiers() {
        return mcc$item == null ? com.google.common.collect.HashMultimap.create() : mcc$item.getAttributeModifiers(this);
    }

    public legacyconnector.v1_7_10.net.minecraft.util.IChatComponent func_151000_E() {
        return new legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText(getDisplayName());
    }

    public void onBlockDestroyed(legacyconnector.v1_7_10.net.minecraft.world.World world, legacyconnector.v1_7_10.net.minecraft.block.Block block, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        if (mcc$item != null) mcc$item.onBlockDestroyed(this, world, block, x, y, z, player);
    }

    public boolean isOnItemFrame() {
        return false;
    }
}
