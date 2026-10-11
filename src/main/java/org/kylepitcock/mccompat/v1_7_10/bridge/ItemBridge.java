package org.kylepitcock.mccompat.v1_7_10.bridge;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemBlock;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagList;
import mccompat.v1_7_10.net.minecraft.util.RegistryNamespaced;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.kylepitcock.mccompat.core.Ids;
import org.kylepitcock.mccompat.core.data.CompatComponents;
import org.kylepitcock.mccompat.core.flatten.VanillaFlattening;
import org.kylepitcock.mccompat.core.registry.LegacyRegistrar;
import org.kylepitcock.mccompat.v1_7_10.LegacyModContainer1710;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockItemAdapter;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyItemAdapter;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyItemHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Item identity and metadata translation.
 *
 * <p><b>Metadata flattening.</b> 1.7.10 used the damage value for two unrelated things: tool wear and item
 * subtypes ({@code dye:4} is lapis). Modern Minecraft has neither subtypes nor numeric damage-as-id, so - like the
 * vanilla 1.13 flattening - every subtype a legacy item reports through {@code getSubItems} becomes its own modern
 * item ({@code mymod:gem}, {@code mymod:gem_1}, ...). That makes tags, recipes, JEI and every modern system see
 * them as distinct items. Tool wear maps to the {@code DAMAGE} component, and unexpected subtype values that were
 * never registered ride along in the {@code mccompat:legacy_meta} component.</p>
 */
public final class ItemBridge {
    private static final Logger LOG = LoggerFactory.getLogger("MCCompat/Items");
    private static final int FIRST_MOD_ID = 4096;

    private static final Map<String, Item> BY_NAME = new LinkedHashMap<>();
    private static final Map<Item, String> NAMES = new IdentityHashMap<>();
    private static final Map<Integer, Item> BY_ID = new HashMap<>();
    private static final Map<Item, Integer> IDS = new IdentityHashMap<>();
    private static final Map<Block, Item> BLOCK_ITEMS = new IdentityHashMap<>();
    private static final Map<Item, Block> ITEM_BLOCKS = new IdentityHashMap<>();
    private static final Map<net.minecraft.world.item.Item, Object[]> VANILLA_REVERSE = new IdentityHashMap<>();
    private static final List<Item> MOD_ITEMS = new ArrayList<>();
    private static final Map<String, ItemStack> CUSTOM_STACKS = new HashMap<>();
    private static int nextId = FIRST_MOD_ID;
    /** Item whose subtypes are being discovered; its stacks carry metadata in a placeholder. */
    private static Item discovering;

    private ItemBridge() {}

    public static RegistryNamespaced legacyRegistry() {
        return new RegistryNamespaced(ItemBridge::byName, o -> nameOf((Item) o), ItemBridge::byId, o -> idOf((Item) o),
                () -> new ArrayList<Object>(BY_NAME.values()).iterator());
    }

    public static List<Item> modItems() {
        return Collections.unmodifiableList(MOD_ITEMS);
    }

    // ------------------------------------------------------------------ vanilla wrappers

    @SuppressWarnings("unchecked")
    public static synchronized <T extends Item> T vanilla(String legacyName, Class<T> type) {
        String full = legacyName.indexOf(':') < 0 ? "minecraft:" + legacyName : legacyName;
        Item existing = BY_NAME.get(full);
        if (existing != null) return (T) existing;
        int id = VanillaFlattening.itemId(full);
        try {
            Constructor<T> c = type.getConstructor(VanillaInfo.class);
            T item = c.newInstance(new VanillaInfo(full, id, null));
            index(full, id, item);
            return item;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create vanilla item wrapper " + type + " for " + legacyName, e);
        }
    }

    private static void index(String name, int id, Item item) {
        BY_NAME.put(name, item);
        NAMES.put(item, name);
        if (id >= 0) {
            BY_ID.putIfAbsent(id, item);
            IDS.put(item, id);
        }
    }

    public static void initVanilla(Item item, VanillaInfo info) {
        net.minecraft.world.item.Item modern = info.modern() instanceof net.minecraft.world.item.Item m ? m : VanillaFlattening.item(info.legacyName(), 0);
        boolean subtypes = false;
        if (info.modern() == null && info.legacyId() >= 0) {
            net.minecraft.world.item.Item first = VanillaFlattening.item(info.legacyName(), 0);
            for (int meta = 1; meta < 16 && !subtypes; meta++) {
                net.minecraft.world.item.Item other = VanillaFlattening.item(info.legacyName(), meta);
                subtypes = other != Items.AIR && other != first;
            }
        }
        var stack = new net.minecraft.world.item.ItemStack(modern);
        item.mcc$setVanillaProperties(stack.getMaxStackSize(), stack.getMaxDamage(), subtypes);
    }

    private static void ensureVanilla() {
        try {
            Class.forName("mccompat.v1_7_10.net.minecraft.init.Items", true, ItemBridge.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ mod registration

    public static synchronized void register(Item item, String name, String modId) {
        if (NAMES.containsKey(item)) {
            LOG.warn("Legacy item {} registered twice (as {})", item, name);
            return;
        }
        LegacyModContainer1710 owner = LegacyModContainer1710.active();
        String domain = modId != null ? modId : owner != null ? owner.getModId() : "minecraft";
        String namespace = owner != null && modId == null ? owner.record().modernId : Ids.modernModId(domain);
        ResourceLocation base = ResourceLocation.fromNamespaceAndPath(namespace, Ids.normalizePath(name).replace('/', '_'));
        String legacyName = domain + ":" + name;
        index(legacyName, nextId++, item);
        MOD_ITEMS.add(item);
        for (int meta : discoverMetas(item)) {
            LegacyItemAdapter adapter = new LegacyItemAdapter(item, meta, properties(item));
            item.mcc$variants.put(meta, LegacyRegistrar.register(BuiltInRegistries.ITEM, variantId(base, meta), adapter));
        }
    }

    public static synchronized void registerBlockItem(ItemBlock item, Block block, ResourceLocation blockId, String legacyName) {
        index(legacyName, nextId++, item);
        MOD_ITEMS.add(item);
        BLOCK_ITEMS.put(block, item);
        ITEM_BLOCKS.put(item, block);
        LegacyBlockAdapter adapter = (LegacyBlockAdapter) block.mcc$modern;
        List<Integer> metas = new ArrayList<>(discoverMetas(item));
        Collections.reverse(metas); // meta 0 last, so it becomes the block's canonical BlockItem
        for (int meta : metas) {
            LegacyBlockItemAdapter modern = new LegacyBlockItemAdapter(adapter, item, meta, properties(item));
            item.mcc$variants.put(meta, LegacyRegistrar.register(BuiltInRegistries.ITEM, variantId(blockId, meta), modern));
        }
    }

    private static ResourceLocation variantId(ResourceLocation base, int meta) {
        return meta == 0 ? base : ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "_" + meta);
    }

    private static net.minecraft.world.item.Item.Properties properties(Item item) {
        var p = new net.minecraft.world.item.Item.Properties();
        if (item.isDamageable()) p.durability(item.getMaxDamage());
        else p.stacksTo(Math.max(1, Math.min(99, item.getItemStackLimit())));
        float damage;
        try {
            damage = item.mcc$attackDamage();
        } catch (RuntimeException e) {
            damage = 0;
        }
        var modifiers = net.minecraft.world.item.component.ItemAttributeModifiers.builder();
        boolean anyModifier = false;
        if (item instanceof mccompat.v1_7_10.net.minecraft.item.ItemArmor armor && armor.armorType >= 0 && armor.armorType < 4
                && armor.damageReduceAmount > 0) {
            var group = new net.minecraft.world.entity.EquipmentSlotGroup[] {net.minecraft.world.entity.EquipmentSlotGroup.HEAD,
                    net.minecraft.world.entity.EquipmentSlotGroup.CHEST, net.minecraft.world.entity.EquipmentSlotGroup.LEGS,
                    net.minecraft.world.entity.EquipmentSlotGroup.FEET}[armor.armorType];
            modifiers.add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mccompat", "legacy_armor_" + armor.armorType),
                            armor.damageReduceAmount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), group);
            anyModifier = true;
        }
        if (anyModifier && damage <= 0) p.attributes(modifiers.build());
        if (damage > 0) {
            // Legacy weapon damage is the total; the modern attribute adds to the player's base 1.
            p.attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                    .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                            new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID,
                                    damage, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                            net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                    .build());
        }
        if (item.hasContainerItem() && item.getContainerItem() != null && item.getContainerItem().mcc$variants.containsKey(0)) {
            p.craftRemainder(item.getContainerItem().mcc$variants.get(0));
        }
        return p;
    }

    /** Asks the legacy item which subtypes exist ({@code getSubItems}); always includes 0. */
    private static List<Integer> discoverMetas(Item item) {
        TreeSet<Integer> metas = new TreeSet<>();
        metas.add(0);
        if (item.getHasSubtypes()) {
            List<Object> list = new ArrayList<>();
            discovering = item;
            try {
                item.getSubItems(item, item.getCreativeTab(), list);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("getSubItems failed for {} while discovering subtypes; registering metadata 0 only", item, e);
            } finally {
                discovering = null;
            }
            for (Object o : list) if (o instanceof ItemStack s && s.getItem() == item) metas.add(s.getItemDamage() & 0x7FFF);
            if (metas.size() > 256) {
                LOG.warn("{} reports {} subtypes; flattening only the first 256", item, metas.size());
                while (metas.size() > 256) metas.pollLast();
            }
        }
        return new ArrayList<>(metas);
    }

    // ------------------------------------------------------------------ legacy <-> modern stacks

    public static net.minecraft.world.item.ItemStack toModern(Item item, int count, int meta) {
        if (item == null) return net.minecraft.world.item.ItemStack.EMPTY;
        if (item == discovering) {
            var probe = new net.minecraft.world.item.ItemStack(Items.BARRIER, Math.max(count, 1));
            probe.set(CompatComponents.legacyMeta(), meta);
            return probe;
        }
        if (item.mcc$vanilla != null) {
            VanillaInfo info = item.mcc$vanilla;
            net.minecraft.world.item.Item modern = info.modern() instanceof net.minecraft.world.item.Item m ? m
                    : VanillaFlattening.item(info.legacyName(), item.getHasSubtypes() ? meta : 0);
            var stack = new net.minecraft.world.item.ItemStack(modern, count);
            if (stack.isDamageableItem() && meta > 0 && meta != Short.MAX_VALUE) stack.setDamageValue(meta);
            return stack;
        }
        if (item.mcc$variants.isEmpty()) {
            LOG.debug("ItemStack created for unregistered legacy item {}", item);
            var probe = new net.minecraft.world.item.ItemStack(Items.BARRIER, Math.max(count, 1));
            probe.set(CompatComponents.legacyMeta(), meta);
            return probe;
        }
        if (item.isDamageable()) {
            var stack = new net.minecraft.world.item.ItemStack(item.mcc$variants.get(0), count);
            if (meta > 0 && meta != Short.MAX_VALUE) stack.setDamageValue(meta);
            return stack;
        }
        net.minecraft.world.item.Item variant = item.mcc$variants.get(meta);
        if (variant != null) return new net.minecraft.world.item.ItemStack(variant, count);
        var stack = new net.minecraft.world.item.ItemStack(item.mcc$variants.values().iterator().next(), count);
        stack.set(CompatComponents.legacyMeta(), meta);
        return stack;
    }

    public static Item legacyItemOf(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.world.item.Item modern = stack.getItem();
        if (modern instanceof LegacyItemHolder h) return (Item) h.legacyItem();
        return legacyOfModern(modern);
    }

    private static synchronized Item legacyOfModern(net.minecraft.world.item.Item modern) {
        Object[] reverse = vanillaReverse(modern);
        if (reverse != null) return (Item) reverse[0];
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(modern);
        String name = rl.toString();
        Item existing = BY_NAME.get(name);
        if (existing != null) return existing;
        Item wrapper;
        if (modern instanceof net.minecraft.world.item.BlockItem bi) {
            Block block = BlockBridge.legacyBlock(bi.getBlock().defaultBlockState());
            wrapper = new ItemBlock(new VanillaInfo(name, -1, modern), block);
        } else {
            wrapper = new Item(new VanillaInfo(name, -1, modern));
        }
        index(name, -1, wrapper);
        return wrapper;
    }

    /** modern vanilla item -> {legacy wrapper, meta}; built from every vanilla legacy name at metadata 0-15. */
    private static Object[] vanillaReverse(net.minecraft.world.item.Item modern) {
        if (VANILLA_REVERSE.isEmpty()) {
            ensureVanilla();
            BlockBridge.byId(0); // vanilla block wrappers exist
            List<String> names = new ArrayList<>();
            for (String n : BY_NAME.keySet()) if (n.startsWith("minecraft:") && BY_NAME.get(n).mcc$vanilla != null && BY_NAME.get(n).mcc$vanilla.legacyId() >= 0) names.add(n);
            for (var e : VanillaFlattening.reverseItems(names).entrySet()) {
                VANILLA_REVERSE.put(e.getKey(), new Object[] {BY_NAME.get((String) e.getValue()[0]), e.getValue()[1]});
            }
            // Block items of vanilla blocks without an Items field ("stone", "wool", ...).
            for (int id = 1; id < 256; id++) {
                Block b = BlockBridge.byId(id);
                if (b == null || b.mcc$vanilla == null) continue;
                String n = b.mcc$vanilla.legacyName();
                for (int meta = 0; meta < 16; meta++) {
                    net.minecraft.world.item.Item it = VanillaFlattening.item(n, meta);
                    if (it == Items.AIR || VANILLA_REVERSE.containsKey(it)) continue;
                    VANILLA_REVERSE.put(it, new Object[] {fromBlock(b), meta});
                }
            }
        }
        return VANILLA_REVERSE.get(modern);
    }

    public static int legacyDamage(Item item, net.minecraft.world.item.ItemStack stack) {
        Integer explicit = stack.get(CompatComponents.legacyMeta());
        if (explicit != null) return explicit;
        if (item == null) return 0;
        if (stack.getItem() instanceof LegacyItemHolder h) {
            return item.isDamageable() ? stack.getDamageValue() : h.legacyMeta();
        }
        if (stack.isDamageableItem()) return stack.getDamageValue();
        Object[] reverse = vanillaReverse(stack.getItem());
        return reverse != null ? (Integer) reverse[1] : 0;
    }

    public static net.minecraft.world.item.ItemStack withLegacyDamage(Item item, net.minecraft.world.item.ItemStack stack, int damage) {
        if (stack.isDamageableItem()) {
            stack.setDamageValue(Math.max(0, damage));
            return stack;
        }
        if (item == null || legacyDamage(item, stack) == damage) return stack;
        // A different subtype is a different modern item: transmute, keeping count and components.
        net.minecraft.world.item.ItemStack replacement = toModern(item, stack.getCount(), damage);
        net.minecraft.world.item.ItemStack moved = stack.transmuteCopy(replacement.getItem(), stack.getCount());
        Integer meta = replacement.get(CompatComponents.legacyMeta());
        if (meta != null) moved.set(CompatComponents.legacyMeta(), meta);
        else moved.remove(CompatComponents.legacyMeta());
        return moved;
    }

    // ------------------------------------------------------------------ legacy NBT format

    /** 1.7.10 layout {id:short, Count:byte, Damage:short, tag:{}} plus the modern item name for portability. */
    public static void writeLegacyNBT(ItemStack stack, NBTTagCompound nbt) {
        nbt.setShort("id", (short) idOf(stack.getItem()));
        nbt.setByte("Count", (byte) stack.mcc$count());
        nbt.setShort("Damage", (short) stack.getItemDamage());
        nbt.setString("mcc:item", BuiltInRegistries.ITEM.getKey(stack.mcc$modern().getItem()).toString());
        String legacyName = nameOf(stack.getItem());
        if (legacyName != null) nbt.setString("mcc:legacy", legacyName);
        if (stack.hasTagCompound()) nbt.setTag("tag", stack.getTagCompound().copy());
    }

    public static ItemStack readLegacyNBT(NBTTagCompound nbt) {
        Item item = null;
        if (nbt.hasKey("mcc:legacy")) item = byName(nbt.getString("mcc:legacy"));
        if (item == null && nbt.hasKey("mcc:item")) {
            ResourceLocation rl = ResourceLocation.tryParse(nbt.getString("mcc:item"));
            if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) item = legacyOfModern(BuiltInRegistries.ITEM.get(rl));
        }
        if (item == null && nbt.getTagId("id") == 8) item = byName(nbt.getString("id"));
        if (item == null) item = byId(nbt.getShort("id"));
        if (item == null) return null;
        ItemStack stack = new ItemStack(item, nbt.getByte("Count"), Math.max(0, nbt.getShort("Damage")));
        if (nbt.hasKey("tag", 10)) stack.setTagCompound((NBTTagCompound) nbt.getCompoundTag("tag").copy());
        return stack;
    }

    /** 1.7.10 numeric enchantment ids by modern enchantment path. */
    private static final Map<String, Integer> ENCHANT_IDS = Map.ofEntries(
            Map.entry("protection", 0), Map.entry("fire_protection", 1), Map.entry("feather_falling", 2), Map.entry("blast_protection", 3),
            Map.entry("projectile_protection", 4), Map.entry("respiration", 5), Map.entry("aqua_affinity", 6), Map.entry("thorns", 7),
            Map.entry("sharpness", 16), Map.entry("smite", 17), Map.entry("bane_of_arthropods", 18), Map.entry("knockback", 19),
            Map.entry("fire_aspect", 20), Map.entry("looting", 21), Map.entry("efficiency", 32), Map.entry("silk_touch", 33),
            Map.entry("unbreaking", 34), Map.entry("fortune", 35), Map.entry("power", 48), Map.entry("punch", 49), Map.entry("flame", 50),
            Map.entry("infinity", 51), Map.entry("luck_of_the_sea", 61), Map.entry("lure", 62));

    public static NBTTagList legacyEnchantments(net.minecraft.world.item.ItemStack stack) {
        NBTTagList list = new NBTTagList();
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchants.entrySet()) {
            Holder<Enchantment> h = entry.getKey();
            String path = h.unwrapKey().map(k -> k.location().getPath()).orElse("");
            NBTTagCompound e = new NBTTagCompound();
            e.setShort("id", ENCHANT_IDS.getOrDefault(path, -1).shortValue());
            e.setShort("lvl", (short) entry.getIntValue());
            list.appendTag(e);
        }
        return list.tagCount() == 0 ? null : list;
    }

    // ------------------------------------------------------------------ lookups

    public static synchronized Item byName(String name) {
        ensureVanilla();
        String full = name.indexOf(':') < 0 ? "minecraft:" + name : name;
        Item i = BY_NAME.get(full);
        if (i != null) return i;
        for (var e : BY_NAME.entrySet()) if (e.getKey().equalsIgnoreCase(full)) return e.getValue();
        Block b = BlockBridge.byName(full);
        if (b != null) return fromBlock(b);
        ResourceLocation rl = ResourceLocation.tryParse(full.toLowerCase(java.util.Locale.ROOT));
        if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) return legacyOfModern(BuiltInRegistries.ITEM.get(rl));
        return null;
    }

    public static synchronized String nameOf(Item item) {
        String n = NAMES.get(item);
        if (n != null) return n;
        return item != null && item.mcc$vanilla != null ? item.mcc$vanilla.legacyName() : null;
    }

    public static synchronized int idOf(Item item) {
        Integer id = IDS.get(item);
        if (id != null) return id;
        if (item instanceof ItemBlock ib) return BlockBridge.idOf(ib.mcc$block());
        return -1;
    }

    public static synchronized Item byId(int id) {
        ensureVanilla();
        Item i = BY_ID.get(id);
        if (i != null) return i;
        Block b = BlockBridge.byId(id);
        return b == null ? null : fromBlock(b);
    }

    public static synchronized Item fromBlock(Block block) {
        if (block == null) return null;
        Item i = BLOCK_ITEMS.get(block);
        if (i != null) return i;
        if (block.mcc$vanilla == null) return null;
        String name = block.mcc$vanilla.legacyName();
        Item existing = BY_NAME.get(name);
        if (existing instanceof ItemBlock) {
            BLOCK_ITEMS.put(block, existing);
            return existing;
        }
        BlockStateHolder probe = new BlockStateHolder(block);
        if (probe.modernItem() == Items.AIR) return null;
        ItemBlock wrapper = new ItemBlock(new VanillaInfo(name, block.mcc$vanilla.legacyId(),
                block.mcc$vanilla.legacyId() < 0 ? probe.modernItem() : null), block);
        BLOCK_ITEMS.put(block, wrapper);
        ITEM_BLOCKS.put(wrapper, block);
        if (!BY_NAME.containsKey(name)) index(name, block.mcc$vanilla.legacyId(), wrapper);
        return wrapper;
    }

    private record BlockStateHolder(Block block) {
        net.minecraft.world.item.Item modernItem() {
            return BlockBridge.toModern(block, 0).getBlock().asItem();
        }
    }

    public static synchronized Block blockOf(Item item) {
        return ITEM_BLOCKS.get(item);
    }

    public static synchronized void registerCustomStack(String name, ItemStack stack) {
        CUSTOM_STACKS.put(name, stack);
    }

    /** Ensures custom data components exist before stacks are serialized with the legacy tag. */
    static CustomData data(NBTTagCompound tag) {
        return CustomData.of(tag.mcc$modern());
    }
}
