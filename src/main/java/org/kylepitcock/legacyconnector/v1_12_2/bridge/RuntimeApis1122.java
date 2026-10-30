package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentData;
import legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializer;
import org.kylepitcock.legacyconnector.core.LegacyEnums;
import org.kylepitcock.legacyconnector.core.synth.Synth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.12.2 registration and lookup APIs of classes only the synthesizer provides, answered with their real behaviour through
 * {@link Synth#addStaticMethod} / {@link Synth#addInstanceMethod}: what mods register is kept in the same registries vanilla and
 * Forge kept (criteria triggers, item predicates, stats, data serializers and entity data keys, villager trades, Forge's biome
 * lists, spawn placements, parrot mimic sounds, enderman-carriable blocks), and the lookups read them back. Content the modern
 * game reads from data (enderman-carriable blocks) is also turned into generated data by {@link #generate}.
 */
public final class RuntimeApis1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/1.12.2");
    private static final String SYNTH = "legacyconnector_synth.v1_12_2.";

    private RuntimeApis1122() {}

    // ------------------------------------------------------------------ synthesized classes at runtime

    private static final Map<String, Class<?>> SYNTH_CLASSES = new ConcurrentHashMap<>();

    /**
     * The synthesized class standing in for {@code legacyName} (dotted, nested classes with $), loaded by the class loader of the
     * synthesized code that is calling (a stub or helper on the stack), or null when no mod uses it.
     */
    static Class<?> synthClass(String legacyName) {
        Class<?> known = SYNTH_CLASSES.get(legacyName);
        if (known != null) return known;
        String name = SYNTH + legacyName;
        List<ClassLoader> loaders = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(s -> s
                .map(StackWalker.StackFrame::getDeclaringClass)
                .filter(c -> !c.getName().startsWith("org.kylepitcock.") && !c.getName().startsWith("java.") && !c.getName().startsWith("jdk."))
                .sorted((a, b) -> Boolean.compare(!a.getName().startsWith(SYNTH), !b.getName().startsWith(SYNTH)))
                .map(Class::getClassLoader).filter(java.util.Objects::nonNull).distinct().limit(4).toList());
        List<ClassLoader> all = new ArrayList<>(loaders);
        if (Thread.currentThread().getContextClassLoader() != null) all.add(Thread.currentThread().getContextClassLoader());
        for (ClassLoader loader : all) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                SYNTH_CLASSES.put(legacyName, c);
                return c;
            } catch (ClassNotFoundException | LinkageError e) {
                // next loader
            }
        }
        return null;
    }

    /** An inert instance of a synthesized class (no constructor runs), or null. */
    static Object blank(Class<?> type) {
        if (type == null || type.isInterface() || java.lang.reflect.Modifier.isAbstract(type.getModifiers())) return null;
        try {
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return ((sun.misc.Unsafe) f.get(null)).allocateInstance(type);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Calls a public no-argument method of a mod object (its own implementation of a synthesized interface), or null. */
    static Object invoke(Object target, String name) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(name);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(String legacyClass, String name) {
        Class<?> type = synthClass(legacyClass);
        if (type == null || !type.isEnum()) return null;
        Object[] constants = type.getEnumConstants();
        if (constants != null) for (Object c : constants) if (((Enum<?>) c).name().equals(name)) return c;
        return LegacyEnums.add((Class) type, name, new Class<?>[0], new Object[0]);
    }

    private static String enumName(Object o) {
        return o instanceof Enum<?> e ? e.name() : String.valueOf(o);
    }

    // ------------------------------------------------------------------ registries

    /** Forge's ItemPredicates: name -> factory of an item predicate from its JSON. */
    private static final Map<Object, Object> ITEM_PREDICATES = new LinkedHashMap<>();
    /** StatList.ALL_STATS / BASIC_STATS of the stats mods register. */
    private static final Set<Object> STATS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Object> BASIC_STATS = Collections.newSetFromMap(new IdentityHashMap<>());
    /** The serializers DataSerializers.registerSerializer added, numbered after vanilla's 14. */
    private static final List<Object> DATA_SERIALIZERS = new ArrayList<>();
    private static final int VANILLA_SERIALIZERS = 14;
    /** EntityDataManager.NEXT_ID_MAP: the last data parameter id of each entity class. */
    private static final Map<Class<?>, Integer> NEXT_DATA_ID = new HashMap<>();
    /** VillagerCareer trades: career -> level (from 1) -> trade lists. */
    private static final Map<Object, List<List<Object>>> CAREER_TRADES = new IdentityHashMap<>();
    /** World types whose info notice is enabled. */
    private static final Set<Object> INFO_NOTICE = Collections.newSetFromMap(new IdentityHashMap<>());
    /** BlockMatcher -> the block it matches. */
    private static final Map<Object, Block> BLOCK_MATCHERS = new IdentityHashMap<>();
    /** EnumHelper.addSpawnPlacementType: the predicate of each added placement type. */
    private static final Map<Object, BiPredicate<Object, Object>> PLACEMENT_PREDICATES = new IdentityHashMap<>();
    /** EntitySpawnPlacementRegistry: entity class -> placement type. */
    private static final Map<Class<?>, Object> PLACEMENTS = new HashMap<>();
    /** EntityParrot's mimic sounds: entity class -> sound. */
    private static final Map<Class<?>, Object> MIMIC_SOUNDS = new LinkedHashMap<>();
    /** EntityEnderman's carriable blocks (mods' additions and removals; vanilla's are the modern enderman_holdable tag). */
    private static final Map<Block, Boolean> CARRIABLE = new IdentityHashMap<>();
    private static final Set<String> VANILLA_CARRIABLE = Set.of("grass", "dirt", "sand", "gravel", "yellow_flower", "red_flower", "brown_mushroom",
            "red_mushroom", "tnt", "cactus", "clay", "pumpkin", "melon_block", "mycelium", "netherrack");

    /** A horse armor type's texture, hash and protection (vanilla's, and those EnumHelper.addHorseArmor added). */
    private record HorseArmor(String texture, String hash, int protection) {}

    private static final Map<String, HorseArmor> VANILLA_HORSE_ARMOR = Map.of("NONE", new HorseArmor(null, "", 0),
            "IRON", new HorseArmor("textures/entity/horse/armor/horse_armor_iron.png", "meo", 5),
            "GOLD", new HorseArmor("textures/entity/horse/armor/horse_armor_gold.png", "goo", 7),
            "DIAMOND", new HorseArmor("textures/entity/horse/armor/horse_armor_diamond.png", "dio", 11));
    private static final Map<Object, HorseArmor> HORSE_ARMOR = new IdentityHashMap<>();

    /**
     * The last data parameter id each vanilla entity class uses (1.12.2's {@code EntityDataManager.createKey} calls in vanilla's static
     * initializers), so the ids of a mod entity's parameters follow its vanilla superclass's like they did.
     */
    private static final Map<String, Integer> VANILLA_DATA_IDS = new HashMap<>();

    static {
        String e = "net.minecraft.entity.";
        Object[][] ids = {
                {"Entity", 5}, {"EntityLivingBase", 10}, {"EntityLiving", 11}, {"EntityAgeable", 12}, {"passive.EntityTameable", 14},
                {"player.EntityPlayer", 16}, {"item.EntityArmorStand", 17}, {"item.EntityItem", 6}, {"item.EntityItemFrame", 7},
                {"item.EntityFireworkRocket", 7}, {"item.EntityTNTPrimed", 6}, {"item.EntityFallingBlock", 6}, {"item.EntityEnderCrystal", 7},
                {"item.EntityBoat", 11}, {"item.EntityMinecart", 11}, {"item.EntityMinecartFurnace", 12}, {"item.EntityMinecartCommandBlock", 13},
                {"projectile.EntityArrow", 6}, {"projectile.EntityTippedArrow", 7}, {"projectile.EntityPotion", 6},
                {"projectile.EntityFishHook", 6}, {"projectile.EntityWitherSkull", 6}, {"EntityAreaEffectCloud", 11},
                {"passive.AbstractHorse", 14}, {"passive.AbstractChestHorse", 15}, {"passive.EntityHorse", 17}, {"passive.EntityLlama", 18},
                {"passive.EntityPig", 14}, {"passive.EntitySheep", 13}, {"passive.EntityRabbit", 13}, {"passive.EntityVillager", 13},
                {"passive.EntityWolf", 17}, {"passive.EntityOcelot", 15}, {"passive.EntityParrot", 15}, {"passive.EntityBat", 12},
                {"monster.EntityPolarBear", 13}, {"monster.EntityIronGolem", 12}, {"monster.EntitySnowman", 12}, {"monster.EntityCreeper", 14},
                {"monster.EntityZombie", 14}, {"monster.EntityZombieVillager", 16}, {"monster.AbstractSkeleton", 12}, {"monster.EntitySpider", 12},
                {"monster.EntityEnderman", 13}, {"monster.EntitySlime", 12}, {"monster.EntityGhast", 12}, {"monster.EntityBlaze", 12},
                {"monster.EntityWitch", 12}, {"monster.EntityGuardian", 13}, {"monster.EntityShulker", 15}, {"monster.EntityVex", 12},
                {"monster.AbstractIllager", 12}, {"monster.EntitySpellcasterIllager", 13}, {"boss.EntityWither", 15}, {"boss.EntityDragon", 12}};
        for (Object[] row : ids) VANILLA_DATA_IDS.put(e + row[0], (Integer) row[1]);
    }

    private static String legacyName(Class<?> c) {
        String n = c.getName();
        if (n.startsWith(SYNTH)) return n.substring(SYNTH.length());
        if (n.startsWith("legacyconnector.v1_12_2.")) return n.substring("legacyconnector.v1_12_2.".length());
        return n;
    }

    /** EntityDataManager.createKey: the next id of the class's hierarchy (after its nearest superclass with parameters). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static synchronized DataParameter<?> createKey(Class<?> clazz, DataSerializer serializer) {
        int id;
        if (clazz == null) {
            id = 0;
        } else if (NEXT_DATA_ID.containsKey(clazz)) {
            id = NEXT_DATA_ID.get(clazz) + 1;
        } else {
            id = 0;
            for (Class<?> c = clazz.getSuperclass(); c != null && c != Object.class; c = c.getSuperclass()) {
                Integer last = NEXT_DATA_ID.get(c);
                if (last == null) last = VANILLA_DATA_IDS.get(legacyName(c));
                if (last != null) {
                    id = last + 1;
                    break;
                }
            }
        }
        if (id > 254) throw new IllegalArgumentException("Data value id is too big with " + id + "! (Max is 254)");
        if (clazz != null) NEXT_DATA_ID.put(clazz, id);
        if (serializer == null) return new DataParameter<>(id, null);
        DataParameter<?> key = serializer.createKey(id);
        return key != null ? key : new DataParameter<>(id, serializer);
    }

    // ------------------------------------------------------------------ registration of the resolvers

    public static void register() {
        // CriteriaTriggers is a real shim class (register/get/getAll live there)
        String preds = "net.minecraftforge.advancements.critereon.ItemPredicates.";
        Synth.addStaticMethod(preds + "register", args -> {
            if (args.length < 2 || args[0] == null) return null;
            synchronized (ITEM_PREDICATES) {
                if (ITEM_PREDICATES.containsKey(args[0])) LOG.warn("An item predicate with name {} is already registered; keeping the first", args[0]);
                else ITEM_PREDICATES.put(args[0], args[1]);
            }
            return null;
        });
        Synth.addStaticMethod(preds + "get", args -> {
            synchronized (ITEM_PREDICATES) {
                return args.length > 0 && args[0] != null ? ITEM_PREDICATES.get(args[0]) : null;
            }
        });

        // StatBase.registerStat adds the stat to StatList.ALL_STATS; StatBasic's also to BASIC_STATS
        Synth.addInstanceMethod("net.minecraft.stats.StatBase.registerStat", (self, args) -> {
            synchronized (STATS) {
                STATS.add(self);
            }
            return self;
        });
        Synth.addInstanceMethod("net.minecraft.stats.StatBasic.registerStat", (self, args) -> {
            synchronized (STATS) {
                STATS.add(self);
                BASIC_STATS.add(self);
            }
            return self;
        });

        String ds = "net.minecraft.network.datasync.DataSerializers.";
        Synth.addStaticMethod(ds + "registerSerializer", args -> {
            if (args.length > 0 && args[0] != null) {
                synchronized (DATA_SERIALIZERS) {
                    if (!DATA_SERIALIZERS.contains(args[0])) DATA_SERIALIZERS.add(args[0]);
                }
            }
            return null;
        });
        Synth.addStaticMethod(ds + "getSerializer", args -> {
            int id = args.length > 0 ? Synth.toInt(args[0]) - VANILLA_SERIALIZERS : -1;
            synchronized (DATA_SERIALIZERS) {
                return id >= 0 && id < DATA_SERIALIZERS.size() ? DATA_SERIALIZERS.get(id) : Synth.NONE;
            }
        });
        Synth.addStaticMethod(ds + "getSerializerId", args -> {
            synchronized (DATA_SERIALIZERS) {
                int i = args.length > 0 ? DATA_SERIALIZERS.indexOf(args[0]) : -1;
                return i < 0 ? Synth.NONE : VANILLA_SERIALIZERS + i;
            }
        });
        Synth.addStaticMethod("net.minecraft.network.datasync.EntityDataManager.createKey", args -> {
            if (args.length < 2) return Synth.NONE;
            return createKey(args[0] instanceof Class<?> c ? c : null, args[1] instanceof DataSerializer<?> s ? s : null);
        });

        // VillagerRegistry.VillagerCareer: trades by level (addTrade's level starts at 1, getTrades' index at 0)
        String career = "net.minecraftforge.fml.common.registry.VillagerRegistry$VillagerCareer.";
        Synth.addInstanceMethod(career + "addTrade", (self, args) -> {
            int level = args.length > 0 ? Synth.toInt(args[0]) : 0;
            if (level <= 0) throw new IllegalArgumentException("Levels start at 1");
            if (args.length > 1 && args[1] instanceof Object[] trades) {
                synchronized (CAREER_TRADES) {
                    List<List<Object>> levels = CAREER_TRADES.computeIfAbsent(self, k -> new ArrayList<>());
                    while (levels.size() < level) levels.add(new ArrayList<>());
                    for (Object t : trades) if (t != null) levels.get(level - 1).add(t);
                }
            }
            return self;
        });
        Synth.addInstanceMethod(career + "getTrades", (self, args) -> {
            int level = args.length > 0 ? Synth.toInt(args[0]) : -1;
            synchronized (CAREER_TRADES) {
                List<List<Object>> levels = CAREER_TRADES.get(self);
                return levels != null && level >= 0 && level < levels.size() ? Collections.unmodifiableList(new ArrayList<>(levels.get(level))) : null;
            }
        });

        String worldType = "net.minecraft.world.WorldType.";
        Synth.addInstanceMethod(worldType + "enableInfoNotice", (self, args) -> {
            synchronized (INFO_NOTICE) {
                INFO_NOTICE.add(self);
            }
            return self;
        });
        Synth.addInstanceMethod(worldType + "hasInfoNotice", (self, args) -> {
            synchronized (INFO_NOTICE) {
                return INFO_NOTICE.contains(self);
            }
        });

        // BlockMatcher.forBlock(block): a predicate on states of that block (world generators' replaceable-block tests)
        String matcher = "net.minecraft.block.state.pattern.BlockMatcher";
        Synth.addStaticMethod(matcher + ".forBlock", args -> {
            Object m = blank(synthClass(matcher));
            if (m == null) return Synth.NONE;
            synchronized (BLOCK_MATCHERS) {
                BLOCK_MATCHERS.put(m, args.length > 0 && args[0] instanceof Block b ? b : null);
            }
            return m;
        });
        Synth.addInstanceMethod(matcher + ".apply", (self, args) -> {
            Block block;
            synchronized (BLOCK_MATCHERS) {
                if (!BLOCK_MATCHERS.containsKey(self)) return Synth.NONE;
                block = BLOCK_MATCHERS.get(self);
            }
            return args.length > 0 && args[0] instanceof IBlockState s && s.getBlock() == block;
        });

        registerEnums();
        registerEntities();
        registerItems();
    }

    /** Enum constants Forge added at runtime (spawn placement and horse armor types, plant types) and what they carry. */
    private static void registerEnums() {
        String helper = "net.minecraftforge.common.util.EnumHelper.";
        String placement = "net.minecraft.entity.EntityLiving$SpawnPlacementType";
        Synth.addStaticMethod(helper + "addSpawnPlacementType", args -> {
            if (args.length < 1 || !(args[0] instanceof String name)) return Synth.NONE;
            Object type = enumConstant(placement, name);
            if (type == null) return Synth.NONE;
            if (args.length > 1 && args[1] instanceof BiPredicate<?, ?> p) {
                @SuppressWarnings("unchecked")
                BiPredicate<Object, Object> predicate = (BiPredicate<Object, Object>) p;
                synchronized (PLACEMENT_PREDICATES) {
                    PLACEMENT_PREDICATES.put(type, predicate);
                }
            }
            return type;
        });
        Synth.addInstanceMethod(placement + ".canSpawnAt", (self, args) -> {
            BiPredicate<Object, Object> p;
            synchronized (PLACEMENT_PREDICATES) {
                p = PLACEMENT_PREDICATES.get(self);
            }
            return p == null || args.length < 2 ? Synth.NONE : p.test(args[0], args[1]);
        });

        String horse = "net.minecraft.entity.passive.HorseArmorType";
        Synth.addStaticMethod(helper + "addHorseArmor", args -> {
            if (args.length < 3 || !(args[0] instanceof String name)) return Synth.NONE;
            Object type = enumConstant(horse, name);
            if (type == null) return Synth.NONE;
            synchronized (HORSE_ARMOR) {
                // Forge's constructor for added types: (defaultTextureLocation, armorStrength), hash "forge"
                HORSE_ARMOR.put(type, new HorseArmor((String) args[1], "forge", Synth.toInt(args[2])));
            }
            return type;
        });
        Synth.addInstanceMethod(horse + ".getProtection", (self, args) -> {
            HorseArmor a = horseArmor(self);
            return a == null ? Synth.NONE : a.protection();
        });
        Synth.addInstanceMethod(horse + ".getTextureName", (self, args) -> {
            HorseArmor a = horseArmor(self);
            return a == null ? Synth.NONE : a.texture();
        });
        Synth.addInstanceMethod(horse + ".getHash", (self, args) -> {
            HorseArmor a = horseArmor(self);
            return a == null ? Synth.NONE : a.hash();
        });
        Synth.addInstanceMethod(horse + ".getOrdinal", (self, args) -> self instanceof Enum<?> e ? e.ordinal() : Synth.NONE);
    }

    private static HorseArmor horseArmor(Object type) {
        synchronized (HORSE_ARMOR) {
            HorseArmor a = HORSE_ARMOR.get(type);
            if (a != null) return a;
        }
        return type instanceof Enum<?> e ? VANILLA_HORSE_ARMOR.get(e.name()) : null;
    }

    /** Entity registries mods add to: spawn placements, parrot mimic sounds, enderman-carriable blocks. */
    private static void registerEntities() {
        String spawn = "net.minecraft.entity.EntitySpawnPlacementRegistry.";
        Synth.addStaticMethod(spawn + "setPlacementType", args -> {
            if (args.length > 1 && args[0] instanceof Class<?> c) {
                synchronized (PLACEMENTS) {
                    if (args[1] == null) PLACEMENTS.remove(c);
                    else PLACEMENTS.put(c, args[1]);
                }
            }
            return null;
        });
        Synth.addStaticMethod(spawn + "getPlacementForEntity", args -> {
            if (args.length < 1 || !(args[0] instanceof Class<?> c)) return Synth.NONE;
            synchronized (PLACEMENTS) {
                Object type = PLACEMENTS.get(c);
                return type != null ? type : Synth.NONE;
            }
        });

        Synth.addStaticMethod("net.minecraft.entity.passive.EntityParrot.registerMimicSound", args -> {
            if (args.length > 1 && args[0] instanceof Class<?> c && args[1] != null) {
                synchronized (MIMIC_SOUNDS) {
                    MIMIC_SOUNDS.put(c, args[1]);
                }
            }
            return null;
        });

        String enderman = "net.minecraft.entity.monster.EntityEnderman.";
        Synth.addStaticMethod(enderman + "setCarriable", args -> {
            if (args.length > 1 && args[0] instanceof Block b) {
                synchronized (CARRIABLE) {
                    CARRIABLE.put(b, Synth.toBoolean(args[1]));
                }
            }
            return null;
        });
        Synth.addStaticMethod(enderman + "getCarriable", args -> args.length > 0 && args[0] instanceof Block b && isCarriable(b));
    }

    private static boolean isCarriable(Block block) {
        synchronized (CARRIABLE) {
            Boolean set = CARRIABLE.get(block);
            if (set != null) return set;
        }
        var name = block.getRegistryName();
        return name != null && "minecraft".equals(name.getNamespace()) && VANILLA_CARRIABLE.contains(name.getPath());
    }

    /** For the real EntityEnderman class: the same carriable-block registry the synthesized statics used. */
    public static void mcc$setCarriable(Block block, boolean carriable) {
        if (block == null) return;
        synchronized (CARRIABLE) {
            CARRIABLE.put(block, carriable);
        }
    }

    public static boolean mcc$isCarriable(Block block) {
        return block != null && isCarriable(block);
    }

    /** For the real EntityParrot class: the same mimic sound registry the synthesized static used. */
    public static void mcc$registerMimicSound(Class<?> entityClass, Object sound) {
        if (entityClass == null || sound == null) return;
        synchronized (MIMIC_SOUNDS) {
            MIMIC_SOUNDS.put(entityClass, sound);
        }
    }

    /** The mimic sound mods gave an entity class (or one of its superclasses), or null. */
    public static Object mimicSound(Class<?> entityClass) {
        synchronized (MIMIC_SOUNDS) {
            for (Class<?> c = entityClass; c != null; c = c.getSuperclass()) {
                Object s = MIMIC_SOUNDS.get(c);
                if (s != null) return s;
            }
        }
        return null;
    }

    /** Enchanted books and furnace fuel. */
    private static void registerItems() {
        String book = "net.minecraft.item.ItemEnchantedBook.";
        Synth.addStaticMethod(book + "getEnchantedItemStack", args -> args.length > 0 && args[0] instanceof EnchantmentData d
                ? EnchantmentHelper.mcc$enchantedBook(d) : Synth.NONE);
        Synth.addStaticMethod(book + "addEnchantment", args -> {
            if (args.length < 2 || !(args[0] instanceof ItemStack stack) || !(args[1] instanceof EnchantmentData d) || d.enchantment == null) return null;
            Map<Enchantment, Integer> stored = EnchantmentHelper.getEnchantments(stack);
            Integer level = stored.get(d.enchantment);
            if (level == null || level < d.enchantmentLevel) stored.put(d.enchantment, d.enchantmentLevel);
            EnchantmentHelper.setEnchantments(stored, stack);
            return null;
        });
        Synth.addStaticMethod(book + "getEnchantments", args -> {
            if (args.length < 1 || !(args[0] instanceof ItemStack stack)) return new NBTTagList();
            NBTTagCompound tag = stack.getTagCompound();
            return tag != null ? tag.getTagList("StoredEnchantments", 10) : new NBTTagList();
        });

        // TileEntityFurnace.getItemBurnTime: the modern burn time (item overrides, fuel data maps, and the burn time event)
        String furnace = "net.minecraft.tileentity.TileEntityFurnace.";
        Synth.addStaticMethod(furnace + "getItemBurnTime", args -> args.length > 0 && args[0] instanceof ItemStack s ? burnTime(s) : 0);
        Synth.addStaticMethod(furnace + "isItemFuel", args -> args.length > 0 && args[0] instanceof ItemStack s && burnTime(s) > 0);
    }

    private static int burnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        try {
            return Math.max(0, Stacks1122.toModern(stack).getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING));
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ generated data

    /** The blocks mods made carriable, added to the modern enderman_holdable tag (removals of vanilla blocks cannot be expressed by a tag). */
    public static void generate(Map<String, java.util.function.Supplier<byte[]>> out) {
        List<String> ids = new ArrayList<>();
        synchronized (CARRIABLE) {
            for (Map.Entry<Block, Boolean> e : CARRIABLE.entrySet()) {
                if (!e.getValue() || !(e.getKey().mcc$modern instanceof net.minecraft.world.level.block.Block modern)) continue;
                var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(modern);
                if (id != null && !ids.contains(id.toString())) ids.add(id.toString());
            }
        }
        if (ids.isEmpty()) return;
        com.google.gson.JsonArray values = new com.google.gson.JsonArray();
        for (String id : ids) {
            com.google.gson.JsonObject v = new com.google.gson.JsonObject();
            v.addProperty("id", id);
            v.addProperty("required", false);
            values.add(v);
        }
        com.google.gson.JsonObject tag = new com.google.gson.JsonObject();
        tag.addProperty("replace", false);
        tag.add("values", values);
        byte[] bytes = tag.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.merge("data/minecraft/tags/block/enderman_holdable.json", () -> bytes, (a, b) -> a);
    }

    /** The BiomeManager entries mods added for a biome type (DESERT, WARM, COOL, ICY), as legacy BiomeEntry objects. */
    public static List<Object> mcc$biomeEntries(String type) {
        return new ArrayList<>(legacyconnector.v1_12_2.net.minecraftforge.common.BiomeManager.mcc$modEntries(type));
    }
}
