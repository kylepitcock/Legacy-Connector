package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_7_10.net.minecraft.util.RegistryNamespaced;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.data.CompatComponents;
import org.kylepitcock.legacyconnector.core.flatten.VanillaFlattening;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockItemAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyItemAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyItemHolder;
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
 * never registered ride along in the {@code legacyconnector:legacy_meta} component.</p>
 */
public final class ItemBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Items");
    private static final int FIRST_MOD_ID = 4096;
    /**
     * Numeric ids of items with no 1.7.10 equivalent (modern vanilla content such as the beehive, other modern mods' items) are
     * this base plus the item's modern registry id, which the server syncs to clients: the same on both sides, and resolved
     * back by {@link #byId}. 1.7.10 code passes items around by number (Thaumcraft's Thaumometer sends Item.getIdFromItem of a
     * scanned block to the server and reads Item.getItemById back); -1 gave a null item, which Thaumcraft then registered
     * aspects for and dereferenced on every later scan.
     */
    private static final int MODERN_ID_BASE = 20000;

    private static final CaseIndexedMap<Item> BY_NAME = new CaseIndexedMap<>();
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

    /** The registered objects, copied under the registry lock: other threads register items while a tab or JEI iterates. */
    private static synchronized List<Object> snapshot() {
        return new ArrayList<Object>(BY_NAME.values());
    }


    public static RegistryNamespaced legacyRegistry() {
        return new legacyconnector.v1_7_10.cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry<Item>(ItemBridge::byName, o -> nameOf((Item) o), ItemBridge::byId, o -> idOf((Item) o),
                () -> all().iterator(), ItemBridge::addDirect);
    }

    private static volatile boolean vanillaListed;

    /**
     * Every registered item, vanilla ones included. Vanilla wrappers are made on first use (the legacy Items class, a block's
     * item on {@link #fromBlock}), so a mod iterating the registry (MineTweaker's {@code <minecraft:dirt>} brackets) before
     * anything touched them saw only mod items: they are all made before the first iteration.
     */
    private static List<Object> all() {
        if (!vanillaListed) {
            ensureVanilla();
            for (Object o : BlockBridge.all()) {
                if (o instanceof Block b && b.mcc$vanilla != null) {
                    try {
                        fromBlock(b);
                    } catch (RuntimeException | LinkageError e) {
                        LOG.debug("No legacy item for vanilla block {}: {}", b, e.toString());
                    }
                }
            }
            vanillaListed = true;
        }
        return snapshot();
    }

    /**
     * {@code Item.itemRegistry.addObject(id, name, item)} by a mod while it loads (Fisk's Superheroes registers its slabs' items
     * so, after {@code registerBlock} with no item class): an ItemBlock of a block that has no item yet becomes that block's
     * item, any other new mod item is registered as through GameRegistry. Returns false for writes left to the default.
     */
    static synchronized boolean addDirect(String name, Object obj) {
        if (!(obj instanceof Item item) || NAMES.containsKey(item) || name == null) return false;
        try {
            if (item instanceof ItemBlock ib && ib.mcc$block() != null && BlockBridge.nameOf(ib.mcc$block()) != null && !BLOCK_ITEMS.containsKey(ib.mcc$block())) {
                Block block = ib.mcc$block();
                registerBlockItem(ib, block, BuiltInRegistries.BLOCK.getKey((net.minecraft.world.level.block.Block) block.mcc$modern), BlockBridge.nameOf(block));
                return true;
            }
            int colon = name.indexOf(':');
            String domain = colon < 0 ? null : name.substring(0, colon);
            if (domain == null || domain.equals("minecraft") || item instanceof ItemBlock) return false;
            register(item, name.substring(colon + 1), domain);
            return true;
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Could not register {} written directly to the item registry: {}", name, e.toString());
            return false;
        }
    }

    public static List<Item> modItems() {
        return Collections.unmodifiableList(MOD_ITEMS);
    }

    // ------------------------------------------------------------------ vanilla wrappers


    /** 1.7.10 runtime classes of vanilla items whose declared type is less specific (mods cast to these). */
    private static final Map<String, String> RUNTIME_CLASSES = Map.ofEntries(
            Map.entry("apple", "ItemFood"),
            Map.entry("baked_potato", "ItemFood"),
            Map.entry("bed", "ItemBed"),
            Map.entry("beef", "ItemFood"),
            Map.entry("boat", "ItemBoat"),
            Map.entry("book", "ItemBook"),
            Map.entry("bow", "ItemBow"),
            Map.entry("bread", "ItemFood"),
            Map.entry("brewing_stand", "ItemReed"),
            Map.entry("bucket", "ItemBucket"),
            Map.entry("cake", "ItemReed"),
            Map.entry("carrot", "ItemSeedFood"),
            Map.entry("carrot_on_a_stick", "ItemCarrotOnAStick"),
            Map.entry("cauldron", "ItemReed"),
            Map.entry("chainmail_boots", "ItemArmor"),
            Map.entry("chainmail_chestplate", "ItemArmor"),
            Map.entry("chainmail_helmet", "ItemArmor"),
            Map.entry("chainmail_leggings", "ItemArmor"),
            Map.entry("chest_minecart", "ItemMinecart"),
            Map.entry("chicken", "ItemFood"),
            Map.entry("coal", "ItemCoal"),
            Map.entry("command_block_minecart", "ItemMinecart"),
            Map.entry("comparator", "ItemReed"),
            Map.entry("cooked_beef", "ItemFood"),
            Map.entry("cooked_chicken", "ItemFood"),
            Map.entry("cooked_fished", "ItemFishFood"),
            Map.entry("cooked_porkchop", "ItemFood"),
            Map.entry("cookie", "ItemFood"),
            Map.entry("diamond_axe", "ItemAxe"),
            Map.entry("diamond_boots", "ItemArmor"),
            Map.entry("diamond_chestplate", "ItemArmor"),
            Map.entry("diamond_helmet", "ItemArmor"),
            Map.entry("diamond_hoe", "ItemHoe"),
            Map.entry("diamond_leggings", "ItemArmor"),
            Map.entry("diamond_pickaxe", "ItemPickaxe"),
            Map.entry("diamond_shovel", "ItemSpade"),
            Map.entry("diamond_sword", "ItemSword"),
            Map.entry("dye", "ItemDye"),
            Map.entry("egg", "ItemEgg"),
            Map.entry("enchanted_book", "ItemEnchantedBook"),
            Map.entry("ender_eye", "ItemEnderEye"),
            Map.entry("ender_pearl", "ItemEnderPearl"),
            Map.entry("experience_bottle", "ItemExpBottle"),
            Map.entry("filled_map", "ItemMap"),
            Map.entry("fire_charge", "ItemFireball"),
            Map.entry("firework_charge", "ItemFireworkCharge"),
            Map.entry("fireworks", "ItemFirework"),
            Map.entry("fish", "ItemFishFood"),
            Map.entry("fishing_rod", "ItemFishingRod"),
            Map.entry("flint_and_steel", "ItemFlintAndSteel"),
            Map.entry("flower_pot", "ItemReed"),
            Map.entry("furnace_minecart", "ItemMinecart"),
            Map.entry("glass_bottle", "ItemGlassBottle"),
            Map.entry("golden_apple", "ItemAppleGold"),
            Map.entry("golden_carrot", "ItemFood"),
            Map.entry("golden_axe", "ItemAxe"),
            Map.entry("golden_boots", "ItemArmor"),
            Map.entry("golden_chestplate", "ItemArmor"),
            Map.entry("golden_helmet", "ItemArmor"),
            Map.entry("golden_hoe", "ItemHoe"),
            Map.entry("golden_leggings", "ItemArmor"),
            Map.entry("golden_pickaxe", "ItemPickaxe"),
            Map.entry("golden_shovel", "ItemSpade"),
            Map.entry("golden_sword", "ItemSword"),
            Map.entry("hopper_minecart", "ItemMinecart"),
            Map.entry("iron_axe", "ItemAxe"),
            Map.entry("iron_boots", "ItemArmor"),
            Map.entry("iron_chestplate", "ItemArmor"),
            Map.entry("iron_door", "ItemDoor"),
            Map.entry("iron_helmet", "ItemArmor"),
            Map.entry("iron_hoe", "ItemHoe"),
            Map.entry("iron_leggings", "ItemArmor"),
            Map.entry("iron_pickaxe", "ItemPickaxe"),
            Map.entry("iron_shovel", "ItemSpade"),
            Map.entry("iron_sword", "ItemSword"),
            Map.entry("item_frame", "ItemHangingEntity"),
            Map.entry("lava_bucket", "ItemBucket"),
            Map.entry("lead", "ItemLead"),
            Map.entry("leather_boots", "ItemArmor"),
            Map.entry("leather_chestplate", "ItemArmor"),
            Map.entry("leather_helmet", "ItemArmor"),
            Map.entry("leather_leggings", "ItemArmor"),
            Map.entry("map", "ItemEmptyMap"),
            Map.entry("melon", "ItemFood"),
            Map.entry("melon_seeds", "ItemSeeds"),
            Map.entry("milk_bucket", "ItemBucketMilk"),
            Map.entry("minecart", "ItemMinecart"),
            Map.entry("mushroom_stew", "ItemSoup"),
            Map.entry("name_tag", "ItemNameTag"),
            Map.entry("nether_star", "ItemSimpleFoiled"),
            Map.entry("nether_wart", "ItemSeeds"),
            Map.entry("painting", "ItemHangingEntity"),
            Map.entry("poisonous_potato", "ItemFood"),
            Map.entry("porkchop", "ItemFood"),
            Map.entry("potato", "ItemSeedFood"),
            Map.entry("potion", "ItemPotion"),
            Map.entry("pumpkin_pie", "ItemFood"),
            Map.entry("pumpkin_seeds", "ItemSeeds"),
            Map.entry("record_11", "ItemRecord"),
            Map.entry("record_13", "ItemRecord"),
            Map.entry("record_blocks", "ItemRecord"),
            Map.entry("record_cat", "ItemRecord"),
            Map.entry("record_chirp", "ItemRecord"),
            Map.entry("record_far", "ItemRecord"),
            Map.entry("record_mall", "ItemRecord"),
            Map.entry("record_mellohi", "ItemRecord"),
            Map.entry("record_stal", "ItemRecord"),
            Map.entry("record_strad", "ItemRecord"),
            Map.entry("record_wait", "ItemRecord"),
            Map.entry("record_ward", "ItemRecord"),
            Map.entry("redstone", "ItemRedstone"),
            Map.entry("reeds", "ItemReed"),
            Map.entry("repeater", "ItemReed"),
            Map.entry("rotten_flesh", "ItemFood"),
            Map.entry("saddle", "ItemSaddle"),
            Map.entry("shears", "ItemShears"),
            Map.entry("sign", "ItemSign"),
            Map.entry("skull", "ItemSkull"),
            Map.entry("snowball", "ItemSnowball"),
            Map.entry("spawn_egg", "ItemMonsterPlacer"),
            Map.entry("spider_eye", "ItemFood"),
            Map.entry("stone_axe", "ItemAxe"),
            Map.entry("stone_hoe", "ItemHoe"),
            Map.entry("stone_pickaxe", "ItemPickaxe"),
            Map.entry("stone_shovel", "ItemSpade"),
            Map.entry("stone_sword", "ItemSword"),
            Map.entry("tnt_minecart", "ItemMinecart"),
            Map.entry("water_bucket", "ItemBucket"),
            Map.entry("wheat_seeds", "ItemSeeds"),
            Map.entry("wooden_axe", "ItemAxe"),
            Map.entry("wooden_door", "ItemDoor"),
            Map.entry("wooden_hoe", "ItemHoe"),
            Map.entry("wooden_pickaxe", "ItemPickaxe"),
            Map.entry("wooden_shovel", "ItemSpade"),
            Map.entry("wooden_sword", "ItemSword"),
            Map.entry("writable_book", "ItemWritableBook"),
            Map.entry("written_book", "ItemEditableBook"));

    /** The legacy runtime class for a vanilla item if it exists (hand-written or synthesized) and fits the declared type. */
    @SuppressWarnings("unchecked")
    private static <T extends Item> Class<? extends T> runtimeClass(String legacyName, Class<T> declared) {
        String simple = RUNTIME_CLASSES.get(legacyName);
        if (simple == null) return declared;
        for (String prefix : new String[] {"legacyconnector.v1_7_10.net.minecraft.item.", "legacyconnector_synth.v1_7_10.net.minecraft.item."}) {
            try {
                Class<?> c = Class.forName(prefix + simple, false, Item.class.getClassLoader());
                if (declared.isAssignableFrom(c) && !java.lang.reflect.Modifier.isAbstract(c.getModifiers())) return (Class<? extends T>) c;
            } catch (ClassNotFoundException | LinkageError ignored) {
                // not part of this modset's API
            }
        }
        return declared;
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends Item> T vanilla(String legacyName, Class<T> type) {
        String full = legacyName.indexOf(':') < 0 ? "minecraft:" + legacyName : legacyName;
        Item existing = BY_NAME.get(full);
        if (existing != null) return (T) existing;
        int id = VanillaFlattening.itemId(full);
        try {
            Class<? extends T> cls = runtimeClass(full.substring(full.indexOf(':') + 1), type);
            Constructor<? extends T> c;
            try {
                c = cls.getConstructor(VanillaInfo.class);
            } catch (NoSuchMethodException e) {
                c = type.getConstructor(VanillaInfo.class);
            }
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

    /**
     * The modern item of a 1.7.10 vanilla name and damage: the game's own 1.12 upgrade tables, except where 1.7.10 differed from
     * 1.12 (beds had no colours and were red; 1.12 reads damage 0 as white).
     */
    static net.minecraft.world.item.Item vanillaItem(String legacyName, int meta) {
        if ("bed".equals(legacyName) || "minecraft:bed".equals(legacyName)) return Items.RED_BED;
        return VanillaFlattening.item(legacyName, meta);
    }

    /** The item a vanilla item leaves behind when used up in crafting or fluid handling (bucket for the full buckets), null for none. */
    public static Item vanillaContainerItem(VanillaInfo info) {
        net.minecraft.world.item.Item modern = info.modern() instanceof net.minecraft.world.item.Item m ? m : vanillaItem(info.legacyName(), 0);
        if (modern == null || modern == Items.AIR) return null;
        net.minecraft.world.item.Item remainder = modern.getCraftingRemainingItem();
        if (remainder == null || remainder == Items.AIR) return null;
        return legacyItemOf(new net.minecraft.world.item.ItemStack(remainder));
    }

    public static void initVanilla(Item item, VanillaInfo info) {
        net.minecraft.world.item.Item modern = info.modern() instanceof net.minecraft.world.item.Item m ? m : vanillaItem(info.legacyName(), 0);
        boolean subtypes = false;
        if (info.modern() == null && info.legacyId() >= 0) {
            net.minecraft.world.item.Item first = vanillaItem(info.legacyName(), 0);
            for (int meta = 1; meta < 16 && !subtypes; meta++) {
                net.minecraft.world.item.Item other = vanillaItem(info.legacyName(), meta);
                subtypes = other != Items.AIR && other != first;
            }
        }
        var stack = new net.minecraft.world.item.ItemStack(modern);
        item.mcc$setVanillaProperties(stack.getMaxStackSize(), stack.getMaxDamage(), subtypes);
    }

    private static void ensureVanilla() {
        try {
            Class.forName("legacyconnector.v1_7_10.net.minecraft.init.Items", true, ItemBridge.class.getClassLoader());
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
        // An ItemBlock registered under its block's name is that block's item (FML linked them by name): AE2 builds
        // its item blocks itself and registers them with registerItem.
        if (item instanceof ItemBlock ib && ib.mcc$block() != null && ib.mcc$block().mcc$modern != null
                && !BLOCK_ITEMS.containsKey(ib.mcc$block()) && legacyName.equalsIgnoreCase(String.valueOf(BlockBridge.nameOf(ib.mcc$block())))) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey((net.minecraft.world.level.block.Block) ib.mcc$block().mcc$modern);
            registerBlockItem(ib, ib.mcc$block(), blockId, legacyName);
            return;
        }
        index(legacyName, nextId++, item);
        MOD_ITEMS.add(item);
        for (int meta : discoverMetas(item)) {
            LegacyItemAdapter adapter = new LegacyItemAdapter(item, meta, properties(item));
            item.mcc$variants.put(meta, LegacyRegistrar.register(BuiltInRegistries.ITEM, variantId(base, meta), adapter));
        }
        if (discoveryFailed) {
            discoveryFailed = false;
            retryLater(() -> {
                for (int meta : discoverMetas(item)) {
                    if (item.mcc$variants.containsKey(meta)) continue;
                    LegacyItemAdapter adapter = new LegacyItemAdapter(item, meta, properties(item));
                    item.mcc$variants.put(meta, LegacyRegistrar.register(BuiltInRegistries.ITEM, variantId(base, meta), adapter));
                }
            });
        }
    }

    /** Items whose getSubItems failed while mods were still registering (it may need content created later). */
    private static final List<Runnable> RETRY = new ArrayList<>();
    /** The mod that registered each {@link #RETRY} entry (modern id), so its subtypes are listed after its own init. */
    private static final List<String> RETRY_OWNER = new ArrayList<>();

    private static void retryLater(Runnable r) {
        var c = org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710.active();
        RETRY.add(r);
        RETRY_OWNER.add(c == null ? null : c.record().modernId);
    }
    private static boolean discoveryFailed;
    private static boolean deferring;
    private static boolean retrying;

    /** While set, subtype discovery waits for {@link #retryFailedDiscovery()} (the end of the registering mod's init). */
    public static synchronized void setDeferDiscovery(boolean on) {
        deferring = on;
    }

    /** Called once every legacy mod has loaded: subtypes that could not be listed during registration. */
    public static synchronized void retryFailedDiscovery() {
        retryFailedDiscovery(null);
    }

    /** The entries registered by one mod (its modern id), or all of them for null. */
    public static synchronized void retryFailedDiscovery(String owner) {
        List<Runnable> todo = new ArrayList<>();
        for (int i = 0; i < RETRY.size(); i++) {
            if (owner == null || owner.equals(RETRY_OWNER.get(i))) {
                todo.add(RETRY.remove(i));
                RETRY_OWNER.remove(i);
                i--;
            }
        }
        int before = BuiltInRegistries.ITEM.size();
        retrying = true;
        try {
            for (Runnable r : todo) {
                try {
                    r.run();
                } catch (RuntimeException | LinkageError e) {
                    LOG.warn("Subtype discovery failed again", e);
                }
            }
        } finally {
            retrying = false;
        }
        if (!todo.isEmpty()) LOG.debug("Late subtype discovery: {} item(s), {} more item variant(s)", todo.size(), BuiltInRegistries.ITEM.size() - before);
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
        if (discoveryFailed) {
            discoveryFailed = false;
            retryLater(() -> {
                for (int meta : discoverMetas(item)) {
                    if (item.mcc$variants.containsKey(meta)) continue;
                    LegacyBlockItemAdapter modern = new LegacyBlockItemAdapter(adapter, item, meta, properties(item));
                    item.mcc$variants.put(meta, LegacyRegistrar.register(BuiltInRegistries.ITEM, variantId(blockId, meta), modern));
                }
            });
        }
    }

    private static ResourceLocation variantId(ResourceLocation base, int meta) {
        return meta == 0 ? base : ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "_" + meta);
    }

    /**
     * The attack damage the legacy item declares through {@code getAttributeModifiers}/{@code getItemAttributeModifiers} (mods override
     * them to give their tools and weapons damage); falls back to the item's own damage field.
     */
    @SuppressWarnings("rawtypes")
    private static float attackDamageFromModifiers(Item item, float fallback) {
        try {
            com.google.common.collect.Multimap map = item.getAttributeModifiers(new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, 1, 0));
            if (map == null) return fallback;
            String key = legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName();
            java.util.Collection values = map.get(key);
            if (values == null || values.isEmpty()) return fallback;
            double sum = 0;
            for (Object o : values) {
                if (o instanceof legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.AttributeModifier m && m.getOperation() == 0) sum += m.getAmount();
            }
            return sum > 0 ? (float) sum : fallback;
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
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
        damage = attackDamageFromModifiers(item, damage);
        var modifiers = net.minecraft.world.item.component.ItemAttributeModifiers.builder();
        boolean anyModifier = false;
        if (item instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemArmor armor && armor.armorType >= 0 && armor.armorType < 4
                && armor.damageReduceAmount > 0) {
            var group = new net.minecraft.world.entity.EquipmentSlotGroup[] {net.minecraft.world.entity.EquipmentSlotGroup.HEAD,
                    net.minecraft.world.entity.EquipmentSlotGroup.CHEST, net.minecraft.world.entity.EquipmentSlotGroup.LEGS,
                    net.minecraft.world.entity.EquipmentSlotGroup.FEET}[armor.armorType];
            modifiers.add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_armor_" + armor.armorType),
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
    private static void registerIconsForDiscovery(Item item) {
        try {
            if (item instanceof ItemBlock ib && ib.mcc$block() != null) {
                ib.mcc$block().registerBlockIcons(new legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap(0, "textures/blocks"));
            }
            int sprites = item.getSpriteNumber();
            item.registerIcons(new legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap(sprites, sprites == 0 ? "textures/blocks" : "textures/items"));
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("Icon registration for subtype discovery failed for {}", item, e);
        }
    }

    private static List<Integer> discoverMetas(Item item) {
        TreeSet<Integer> metas = new TreeSet<>();
        metas.add(0);
        if (item.getHasSubtypes() && deferring && !retrying) {
            // Forge asked for subtypes when the creative tabs were filled, long after the mod's content existed;
            // some mods' getSubItems read content their preInit has not created yet.
            discoveryFailed = true;
        } else if (item.getHasSubtypes()) {
            List<Object> list = new ArrayList<>();
            discovering = item;
            try {
                try {
                    item.getSubItems(item, item.getCreativeTab(), list);
                } catch (RuntimeException firstFailure) {
                    // Many getSubItems read arrays that registerIcons fills (icons.length), which only runs on the client in
                    // 1.7.10; run it against a detached register and ask again.
                    list.clear();
                    registerIconsForDiscovery(item);
                    item.getSubItems(item, item.getCreativeTab(), list);
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("getSubItems failed for {} while discovering subtypes; registering metadata 0 only", item, e);
                discoveryFailed = true;
            } finally {
                discovering = null;
            }
            for (Object o : list) if (o instanceof ItemStack s && s.getItem() == item) metas.add(s.getItemDamage() & 0x7FFF);
            addUnlistedSubtypes(item, metas);
            if (metas.size() > 256) {
                LOG.warn("{} reports {} subtypes; flattening only the first 256", item, metas.size());
                while (metas.size() > 256) metas.pollLast();
            }
        }
        return new ArrayList<>(metas);
    }

    /**
     * Subtypes a mod keeps out of its creative tab but makes in play: Witchery's bound waystone (meta 13, made by a ritual),
     * rowan berries and artichoke are hidden {@code ItemGeneral} subtypes. Without a variant of their own their stacks are the
     * item's first variant and show its icon (the candelabra). A gap between listed metas is a subtype when the item names it
     * differently from every listed one; probed only for items whose listing is nearly dense (few gaps), so items that name
     * any damage value ("item.x.37") do not grow hundreds of variants.
     */
    private static void addUnlistedSubtypes(Item item, TreeSet<Integer> metas) {
        if (metas.size() < 2) return;
        int max = metas.last();
        int gaps = max + 1 - metas.size();
        if (max > 1024 || gaps == 0 || gaps > metas.size()) return;
        java.util.Set<String> names = new java.util.HashSet<>();
        Item outer = discovering;
        discovering = item;
        try {
            for (int m : metas) {
                String n = subtypeName(item, m);
                if (n != null) names.add(n);
            }
        } finally {
            discovering = outer;
        }
        if (names.size() < metas.size() / 2) return; // listed subtypes do not have names of their own
        try {
            String base = item.getUnlocalizedName();
            if (base != null) names.add(base);
        } catch (RuntimeException | LinkageError ignored) {
        }
        discovering = item;
        try {
            for (int m = 1; m < max; m++) {
                if (metas.contains(m)) continue;
                String n = subtypeName(item, m);
                if (n != null && names.add(n)) metas.add(m);
            }
        } finally {
            discovering = outer;
        }
    }

    private static String subtypeName(Item item, int meta) {
        try {
            return item.getUnlocalizedName(new ItemStack(item, 1, meta));
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
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
                    : vanillaItem(info.legacyName(), item.getHasSubtypes() ? meta : 0);
            var stack = new net.minecraft.world.item.ItemStack(modern, count);
            if (modern instanceof net.minecraft.world.item.MapItem) {
                // a filled map's legacy damage is its map id
                if (meta >= 0 && meta != Short.MAX_VALUE) stack.set(net.minecraft.core.component.DataComponents.MAP_ID, new net.minecraft.world.level.saveddata.maps.MapId(meta));
                return stack;
            }
            if (meta == Short.MAX_VALUE) {
                // The wildcard (OreDictionary.WILDCARD_VALUE) reads back as itself: the ore dictionary's own vanilla entries
                // (treeLeaves = leaves:*) and mods' registrations (Thaumcraft's aspects for wool:*, red_flower:*) are keyed by it.
                // It read back as 0, so only the first subtype (oak leaves, white wool) matched.
                if (!stack.isEmpty()) stack.set(CompatComponents.legacyMeta(), meta);
            } else if (stack.isDamageableItem() && meta > 0) {
                stack.setDamageValue(meta);
            } else if (!stack.isDamageableItem() && meta > 0) {
                // A metadata value the flattening has no item for (log2 with 2, 3) still reads back as it was given.
                Object[] reverse = vanillaReverse(modern);
                if (reverse == null || !Integer.valueOf(meta).equals(reverse[1])) stack.set(CompatComponents.legacyMeta(), meta);
            }
            return stack;
        }
        if (item instanceof ItemBlock ib && ib.mcc$block() != null) {
            var vanillaItem = VanillaEquivalents.item(ib.mcc$block(), meta);
            if (vanillaItem != null) return new net.minecraft.world.item.ItemStack(vanillaItem, count);
        }
        if (item.mcc$variants.isEmpty()) {
            LOG.debug("ItemStack created for unregistered legacy item {}", item);
            var probe = new net.minecraft.world.item.ItemStack(Items.BARRIER, Math.max(count, 1));
            probe.set(CompatComponents.legacyMeta(), meta);
            return probe;
        }
        if (item.isDamageable()) {
            var stack = new net.minecraft.world.item.ItemStack(item.mcc$variants.get(0), count);
            if (meta == Short.MAX_VALUE) stack.set(CompatComponents.legacyMeta(), meta);
            else if (meta > 0) stack.setDamageValue(meta);
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
            // the same item and block pair fromBlock makes, whichever is asked first
            if (block != null && block.mcc$vanilla != null && block.mcc$vanilla.legacyId() < 0 && !BLOCK_ITEMS.containsKey(block)) {
                BLOCK_ITEMS.put(block, wrapper);
                ITEM_BLOCKS.put(wrapper, block);
            }
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
                    net.minecraft.world.item.Item it = vanillaItem(n, meta);
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
        if (stack.getItem() instanceof net.minecraft.world.item.MapItem) {
            var id = stack.get(net.minecraft.core.component.DataComponents.MAP_ID);
            return id == null ? 0 : id.id();
        }
        if (stack.getItem() instanceof LegacyItemHolder h) {
            return item.isDamageable() ? stack.getDamageValue() : h.legacyMeta();
        }
        if (stack.isDamageableItem()) return stack.getDamageValue();
        Object[] reverse = vanillaReverse(stack.getItem());
        return reverse != null ? (Integer) reverse[1] : 0;
    }

    /**
     * A stack made for a subtype before the item's subtypes were registered is the item's first variant carrying the metadata as
     * a component: mods build stacks of their subtypes in preInit/init while subtype discovery waits for the end of their init
     * (Witchery's rituals, kettle and brew recipes keep {@code itemWaystoneBound.createStack()} and copy it on use, so bound
     * waystones, charged attuned stones and brews showed the meta 0 icon, the candelabra). Once the subtype exists the stack
     * becomes that variant; anything else comes back unchanged.
     */
    public static net.minecraft.world.item.ItemStack upgradeVariant(Item item, net.minecraft.world.item.ItemStack stack) {
        if (item == null || item.mcc$vanilla != null || stack.isEmpty() || !(stack.getItem() instanceof LegacyItemHolder)) return stack;
        Integer meta = stack.get(CompatComponents.legacyMeta());
        if (meta == null) return stack;
        net.minecraft.world.item.Item variant = item.mcc$variants.get(meta);
        if (variant == null || variant == stack.getItem()) return stack;
        net.minecraft.world.item.ItemStack moved = stack.transmuteCopy(variant, stack.getCount());
        moved.remove(CompatComponents.legacyMeta());
        return moved;
    }

    public static net.minecraft.world.item.ItemStack withLegacyDamage(Item item, net.minecraft.world.item.ItemStack stack, int damage) {
        if (stack.getItem() instanceof net.minecraft.world.item.MapItem && stack.get(CompatComponents.legacyMeta()) == null) {
            stack.set(net.minecraft.core.component.DataComponents.MAP_ID, new net.minecraft.world.level.saveddata.maps.MapId(Math.max(0, damage)));
            return stack;
        }
        if (stack.isDamageableItem()) {
            if (damage == Short.MAX_VALUE) {
                stack.set(CompatComponents.legacyMeta(), damage);
                return stack;
            }
            stack.remove(CompatComponents.legacyMeta()); // no longer the wildcard
            stack.setDamageValue(Math.max(0, damage));
            return stack;
        }
        if (item == null || legacyDamage(item, stack) == damage) return stack;
        // A different subtype is a different modern item: transmute, keeping count and components.
        net.minecraft.world.item.ItemStack replacement = toModern(item, stack.getCount(), damage);
        if (replacement.getItem() == stack.getItem()) {
            // Same modern item (an item without subtypes keeps its damage as the meta component): change the stack in place, so
            // a view of a slot's stack changes the slot. Witchery's taglock kit becomes "full" with setItemDamage(1) on the held
            // stack; a copy left the slot's kit empty and the Rite of Transposition never found a bound taglock.
            Integer meta = replacement.get(CompatComponents.legacyMeta());
            if (meta != null) stack.set(CompatComponents.legacyMeta(), meta);
            else stack.remove(CompatComponents.legacyMeta());
            return stack;
        }
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
        Item ci = BY_NAME.getIgnoreCase(full);
        if (ci != null) return ci;
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
        if (item instanceof ItemBlock ib) {
            int block = BlockBridge.registeredIdOf(ib.mcc$block());
            if (block >= 0) return block;
        }
        return modernOnlyId(item);
    }

    /** {@link #MODERN_ID_BASE} plus the modern registry id for a wrapper of a modern-only item, else -1. */
    private static int modernOnlyId(Item item) {
        if (item == null || item.mcc$vanilla == null || item.mcc$vanilla.legacyId() >= 0) return -1;
        if (!(item.mcc$vanilla.modern() instanceof net.minecraft.world.item.Item modern) || modern == Items.AIR) return -1;
        int raw = BuiltInRegistries.ITEM.getId(modern);
        return raw < 0 ? -1 : MODERN_ID_BASE + raw;
    }

    /**
     * The id of a modern-only block. Like a 1.7.10 ItemBlock and its block, it is its item's id when that item resolves back to
     * the block (beehive, bee nest); blocks that share another block's item or have none (wall signs and banners, coloured
     * beds whose item is the legacy bed, bubble columns) get {@link BlockBridge#MODERN_BLOCK_ID_BASE} plus the modern block
     * registry id, so Block.getBlockById returns the same block.
     */
    static synchronized int modernOnlyBlockId(Block block) {
        int id = modernOnlyId(fromBlock(block));
        if (id >= 0 && blockOf(byId(id)) == block) return id;
        if (!(block.mcc$vanilla.modern() instanceof net.minecraft.world.level.block.Block modern)) return -1;
        int raw = BuiltInRegistries.BLOCK.getId(modern);
        return raw < 0 ? -1 : BlockBridge.MODERN_BLOCK_ID_BASE + raw;
    }

    public static synchronized Item byId(int id) {
        ensureVanilla();
        Item i = BY_ID.get(id);
        if (i != null) return i;
        Block b = BlockBridge.registeredById(id);
        if (b != null) return fromBlock(b);
        if (id >= MODERN_ID_BASE && id < BlockBridge.MODERN_BLOCK_ID_BASE) {
            net.minecraft.world.item.Item modern = BuiltInRegistries.ITEM.byId(id - MODERN_ID_BASE);
            if (modern != null && modern != Items.AIR && !(modern instanceof LegacyItemHolder)) {
                Item wrapper = legacyOfModern(modern);
                if (modernOnlyId(wrapper) == id) return wrapper;
            }
        }
        return null;
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
        // 1.7.10 has an ItemBlock for every block but air, including the ones modern Minecraft has no item for
        // (water, lava, fire, portals); mods enumerate them and reject a null item.
        if (probe.modernItem() == Items.AIR && name.equals("minecraft:air")) return null;
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

    /** Stacks mods publish by name (FML's customItemStacks table), keyed "modid:name". */
    public static synchronized void registerCustomStack(String name, ItemStack stack) {
        var owner = org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710.active();
        CUSTOM_STACKS.put((owner != null ? owner.getModId() : "minecraft") + ":" + name, stack);
    }

    public static synchronized ItemStack customStack(String modId, String name, int size) {
        ItemStack found = CUSTOM_STACKS.get(modId + ":" + name);
        if (found == null) {
            String wanted = (modId + ":" + name).toLowerCase(java.util.Locale.ROOT);
            for (var e : CUSTOM_STACKS.entrySet()) {
                if (e.getKey().toLowerCase(java.util.Locale.ROOT).equals(wanted)) {
                    found = e.getValue();
                    break;
                }
            }
        }
        if (found == null) return null;
        ItemStack copy = found.copy();
        copy.mcc$setCount(size);
        return copy;
    }

    /** Ensures custom data components exist before stacks are serialized with the legacy tag. */
    static CustomData data(NBTTagCompound tag) {
        return CustomData.of(tag.mcc$modern());
    }
}
