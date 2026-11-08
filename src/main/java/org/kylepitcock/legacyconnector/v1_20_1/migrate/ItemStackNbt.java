package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The 1.20.1 item NBT API ({@code getTag}, {@code getOrCreateTag}, {@code setTag}, {@code ItemStack.of}, {@code save}, ...) over 1.21.1 data components.
 * The loader redirects every call of these methods here.
 *
 * <p>Mapping: everything a mod stores is the {@code minecraft:custom_data} component; the keys vanilla gave a component of their own are converted in both
 * directions: {@code Damage}, {@code RepairCost}, {@code Unbreakable}, {@code CustomModelData}, {@code display.Name}/{@code display.Lore},
 * {@code Enchantments}/{@code StoredEnchantments} and {@code BlockEntityTag}. Components with no 1.20.1 key stay in the stack and survive
 * {@link #save} through the component form that {@link #of} reads back.</p>
 *
 * <p>Limits (lossy at the edges, on purpose): {@link #getOrCreateTag} returns only the custom data (a live tag that writes through), {@link #getTag} the custom
 * data plus the converted keys (a copy as soon as there are converted keys, so a write to it is lost: mods write through {@code getOrCreateTag} or
 * {@code setTag}). Component values are immutable and shared between stack copies, so every handed out tag is a private copy first.</p>
 */
public final class ItemStackNbt {
    private ItemStackNbt() {}

    // ------------------------------------------------------------------ item parser results (1.20.1: item + nbt, 1.21.1: item holder + component patch)

    /** {@code ItemParser.parseForItem(HolderLookup<Item>, StringReader)}: the parser is an object over the registries now. */
    public static net.minecraft.commands.arguments.item.ItemParser.ItemResult parseForItem(net.minecraft.core.HolderLookup lookup, com.mojang.brigadier.StringReader reader)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        net.minecraft.core.HolderLookup.Provider provider = MigrateContext.provider();
        if (provider == null || provider == net.minecraft.core.RegistryAccess.EMPTY) // before any server or level: a provider of the built-in registries
            provider = net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.asLookup(),
                    net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.asLookup()));
        return new net.minecraft.commands.arguments.item.ItemParser(provider).parse(reader);
    }

    /** {@code ItemResult.nbt()}: the tag a 1.20.1 stack of this result would have carried (null when none). */
    public static CompoundTag itemResultNbt(net.minecraft.commands.arguments.item.ItemParser.ItemResult result) {
        return getTag(new ItemStack(result.item(), 1, result.components()));
    }

    // ------------------------------------------------------------------ reading

    /** The 1.20.1 {@code getTag}: null when the stack carries no data a 1.20.1 stack would have had a tag for. */
    public static CompoundTag getTag(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CompoundTag extra = converted(stack);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (extra == null) {
            if (data == null || data.isEmpty()) return null;
            return detach(stack, data); // live: a mod that mutates what hasTag/getTag handed out keeps working
        }
        CompoundTag merged = data == null ? new CompoundTag() : data.copyTag();
        merged.merge(extra);
        return merged;
    }

    public static boolean hasTag(ItemStack stack) {
        return getTag(stack) != null;
    }

    /** A private custom data tag that writes through to the stack. */
    public static CompoundTag getOrCreateTag(ItemStack stack) {
        if (stack.isEmpty()) return new CompoundTag();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return detach(stack, data == null ? CustomData.EMPTY : data);
    }

    private static CompoundTag detach(ItemStack stack, CustomData data) {
        CustomData fresh = CustomData.of(data.getUnsafe());
        stack.set(DataComponents.CUSTOM_DATA, fresh);
        return fresh.getUnsafe();
    }

    public static CompoundTag getTagElement(ItemStack stack, String key) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : null;
    }

    public static CompoundTag getOrCreateTagElement(ItemStack stack, String key) {
        CompoundTag tag = getOrCreateTag(stack);
        if (tag.contains(key, Tag.TAG_COMPOUND)) return tag.getCompound(key);
        CompoundTag fresh = new CompoundTag();
        tag.put(key, fresh);
        return fresh;
    }

    // ------------------------------------------------------------------ writing

    /** The 1.20.1 {@code setTag}: null clears; converted keys go to their components, the rest to custom data. */
    public static void setTag(ItemStack stack, CompoundTag tag) {
        if (stack.isEmpty()) return;
        stack.remove(DataComponents.CUSTOM_DATA);
        if (tag == null) {
            clearConverted(stack);
            return;
        }
        CompoundTag rest = tag.copy();
        applyConverted(stack, rest);
        if (!rest.isEmpty()) stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rest));
    }

    public static void addTagElement(ItemStack stack, String key, Tag value) {
        if (stack.isEmpty()) return;
        CompoundTag whole = getTag(stack);
        whole = whole == null ? new CompoundTag() : whole.copy();
        whole.put(key, value);
        setTag(stack, whole);
    }

    public static void removeTagKey(ItemStack stack, String key) {
        CompoundTag whole = getTag(stack);
        if (whole == null || !whole.contains(key)) return;
        whole = whole.copy();
        whole.remove(key);
        setTag(stack, whole);
    }

    // ------------------------------------------------------------------ whole stacks

    /** The 1.20.1 {@code ItemStack.save(CompoundTag)}: {@code id}, {@code Count}, {@code tag} for readers of the old form, plus the full component form. */
    public static CompoundTag save(ItemStack stack, CompoundTag out) {
        if (stack.isEmpty()) return out;
        HolderLookup.Provider provider = MigrateContext.provider();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        out.putString("id", id.toString());
        out.putByte("Count", (byte) Math.min(stack.getCount(), 127));
        out.putInt("count", stack.getCount());
        CompoundTag legacy = getTag(stack);
        if (legacy != null) out.put("tag", legacy);
        Tag modern = stack.save(provider);
        if (modern instanceof CompoundTag m && m.contains("components", Tag.TAG_COMPOUND)) out.put("components", m.getCompound("components"));
        return out;
    }

    /** The 1.20.1 {@code ItemStack.of(CompoundTag)}: reads the component form when present, else the 1.20.1 {@code id}/{@code Count}/{@code tag}. */
    public static ItemStack of(CompoundTag tag) {
        if (tag == null || !tag.contains("id", Tag.TAG_STRING)) return ItemStack.EMPTY;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (tag.contains("components", Tag.TAG_COMPOUND)) {
            CompoundTag modern = new CompoundTag();
            modern.putString("id", id.toString());
            modern.putInt("count", Math.max(1, tag.contains("count", Tag.TAG_ANY_NUMERIC) ? tag.getInt("count") : tag.getByte("Count")));
            modern.put("components", tag.getCompound("components"));
            ItemStack parsed = ItemStack.parseOptional(MigrateContext.provider(), modern);
            if (!parsed.isEmpty()) return parsed;
        }
        int count = tag.contains("count", Tag.TAG_ANY_NUMERIC) ? tag.getInt("count") : tag.contains("Count", Tag.TAG_ANY_NUMERIC) ? tag.getByte("Count") : 1;
        ItemStack stack = new ItemStack(item, count);
        if (tag.contains("tag", Tag.TAG_COMPOUND)) setTag(stack, tag.getCompound("tag"));
        return stack;
    }

    public static boolean isSameItemSameTags(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    // ------------------------------------------------------------------ display name

    public static boolean hasCustomHoverName(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_NAME);
    }

    public static ItemStack setHoverName(ItemStack stack, Component name) {
        if (name == null) stack.remove(DataComponents.CUSTOM_NAME);
        else stack.set(DataComponents.CUSTOM_NAME, name);
        return stack;
    }

    /** 1.20.1 {@code ItemStack.resetHoverName()}. */
    public static void resetHoverName(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_NAME);
    }

    // ------------------------------------------------------------------ converted keys

    /** The keys 1.20.1 kept in the stack tag that 1.21.1 has components for; null when the stack has none of them. */
    private static CompoundTag converted(ItemStack stack) {
        CompoundTag out = null;
        int damage = stack.getOrDefault(DataComponents.DAMAGE, 0);
        if (damage > 0) (out = nonNull(out)).putInt("Damage", damage);
        Integer repair = stack.get(DataComponents.REPAIR_COST);
        if (repair != null && repair > 0) (out = nonNull(out)).putInt("RepairCost", repair);
        if (stack.has(DataComponents.UNBREAKABLE)) (out = nonNull(out)).putBoolean("Unbreakable", true);
        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) (out = nonNull(out)).putInt("CustomModelData", cmd.value());
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        ItemLore lore = stack.get(DataComponents.LORE);
        if (name != null || (lore != null && !lore.lines().isEmpty())) {
            HolderLookup.Provider provider = MigrateContext.provider();
            CompoundTag display = new CompoundTag();
            if (name != null) display.putString("Name", Component.Serializer.toJson(name, provider));
            if (lore != null && !lore.lines().isEmpty()) {
                ListTag lines = new ListTag();
                for (Component line : lore.lines()) lines.add(StringTag.valueOf(Component.Serializer.toJson(line, provider)));
                display.put("Lore", lines);
            }
            (out = nonNull(out)).put("display", display);
        }
        ListTag ench = enchantments(stack.get(DataComponents.ENCHANTMENTS));
        if (ench != null) (out = nonNull(out)).put("Enchantments", ench);
        ListTag stored = enchantments(stack.get(DataComponents.STORED_ENCHANTMENTS));
        if (stored != null) (out = nonNull(out)).put("StoredEnchantments", stored);
        CustomData be = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (be != null && !be.isEmpty()) (out = nonNull(out)).put("BlockEntityTag", be.copyTag());
        return out;
    }

    private static CompoundTag nonNull(CompoundTag t) {
        return t != null ? t : new CompoundTag();
    }

    private static ListTag enchantments(ItemEnchantments e) {
        if (e == null || e.isEmpty()) return null;
        ListTag list = new ListTag();
        for (var entry : e.entrySet()) {
            CompoundTag c = new CompoundTag();
            entry.getKey().unwrapKey().ifPresent(k -> c.putString("id", k.location().toString()));
            c.putShort("lvl", (short) entry.getIntValue());
            list.add(c);
        }
        return list;
    }

    private static void clearConverted(ItemStack stack) {
        stack.remove(DataComponents.REPAIR_COST);
        stack.remove(DataComponents.UNBREAKABLE);
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        stack.remove(DataComponents.CUSTOM_NAME);
        stack.remove(DataComponents.LORE);
        stack.remove(DataComponents.ENCHANTMENTS);
        stack.remove(DataComponents.STORED_ENCHANTMENTS);
        stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        stack.remove(DataComponents.DAMAGE);
        if (stack.isDamageableItem()) stack.set(DataComponents.DAMAGE, 0);
    }

    /** Moves the converted keys out of {@code tag} (what stays is custom data) into components. */
    private static void applyConverted(ItemStack stack, CompoundTag tag) {
        clearConverted(stack);
        if (tag.contains("Damage", Tag.TAG_ANY_NUMERIC)) {
            if (stack.isDamageableItem()) stack.set(DataComponents.DAMAGE, tag.getInt("Damage"));
            tag.remove("Damage");
        }
        if (tag.contains("RepairCost", Tag.TAG_ANY_NUMERIC)) {
            stack.set(DataComponents.REPAIR_COST, tag.getInt("RepairCost"));
            tag.remove("RepairCost");
        }
        if (tag.contains("Unbreakable", Tag.TAG_ANY_NUMERIC)) {
            if (tag.getBoolean("Unbreakable")) stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
            tag.remove("Unbreakable");
        }
        if (tag.contains("CustomModelData", Tag.TAG_ANY_NUMERIC)) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(tag.getInt("CustomModelData")));
            tag.remove("CustomModelData");
        }
        if (tag.contains("display", Tag.TAG_COMPOUND)) {
            CompoundTag display = tag.getCompound("display");
            HolderLookup.Provider provider = MigrateContext.provider();
            if (display.contains("Name", Tag.TAG_STRING)) {
                try {
                    stack.set(DataComponents.CUSTOM_NAME, Component.Serializer.fromJson(display.getString("Name"), provider));
                } catch (RuntimeException ignored) {
                    // malformed old name: dropped
                }
                display.remove("Name");
            }
            if (display.contains("Lore", Tag.TAG_LIST)) {
                List<Component> lines = new ArrayList<>();
                ListTag lore = display.getList("Lore", Tag.TAG_STRING);
                for (int i = 0; i < lore.size(); i++) {
                    try {
                        lines.add(Component.Serializer.fromJson(lore.getString(i), provider));
                    } catch (RuntimeException ignored) {
                        // malformed line: dropped
                    }
                }
                if (!lines.isEmpty()) stack.set(DataComponents.LORE, new ItemLore(lines));
                display.remove("Lore");
            }
            if (display.isEmpty()) tag.remove("display");
        }
        enchantmentsFrom(stack, tag, "Enchantments", false);
        enchantmentsFrom(stack, tag, "StoredEnchantments", true);
        if (tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            CompoundTag be = tag.getCompound("BlockEntityTag").copy();
            if (be.contains("id", Tag.TAG_STRING)) stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(be));
            else if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
                var type = bi.getBlock() instanceof net.minecraft.world.level.block.EntityBlock eb
                        ? eb.newBlockEntity(net.minecraft.core.BlockPos.ZERO, bi.getBlock().defaultBlockState()) : null;
                if (type != null) {
                    ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type.getType());
                    if (key != null) be.putString("id", key.toString());
                }
                stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(be));
            }
            tag.remove("BlockEntityTag");
        }
    }

    private static void enchantmentsFrom(ItemStack stack, CompoundTag tag, String key, boolean stored) {
        if (!tag.contains(key, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        var lookup = MigrateContext.registries().lookup(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        if (lookup.isPresent()) {
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(c.getString("id"));
                if (id == null) continue;
                Holder<Enchantment> h = lookup.get().get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
                if (h != null) m.set(h, c.getShort("lvl"));
            }
        }
        stack.set(stored ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, m.toImmutable());
        tag.remove(key);
    }
}
