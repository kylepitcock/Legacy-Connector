package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12.2 vanilla blocks and items as legacy objects over the modern ones. A field name maps to a modern registry name (mostly its
 * lower case; the 1.13 flattening renamed the rest, and variants selected by metadata become their first variant).
 */
public final class Vanilla1122 {
    private static final Map<String, String> BLOCK_NAMES = new HashMap<>();
    private static final Map<String, String> ITEM_NAMES = new HashMap<>();
    private static final Map<net.minecraft.world.item.Item, Item> ITEM_REVERSE = new IdentityHashMap<>();
    private static final Map<net.minecraft.world.level.block.Block, Block> BLOCK_REVERSE = new IdentityHashMap<>();

    static {
        String[] blocks = {"GRASS", "grass_block", "LOG", "oak_log", "LOG2", "acacia_log", "PLANKS", "oak_planks", "LEAVES", "oak_leaves",
                "LEAVES2", "acacia_leaves", "SAPLING", "oak_sapling", "WOOL", "white_wool", "STAINED_GLASS", "white_stained_glass",
                "STAINED_GLASS_PANE", "white_stained_glass_pane", "STAINED_HARDENED_CLAY", "white_terracotta", "HARDENED_CLAY", "terracotta",
                "STONEBRICK", "stone_bricks", "BRICK_BLOCK", "bricks", "MOB_SPAWNER", "spawner", "WEB", "cobweb", "TALLGRASS", "short_grass",
                "DEADBUSH", "dead_bush", "YELLOW_FLOWER", "dandelion", "RED_FLOWER", "poppy", "LIT_FURNACE", "furnace", "REEDS", "sugar_cane",
                "FENCE", "oak_fence", "FENCE_GATE", "oak_fence_gate", "WOODEN_DOOR", "oak_door", "WOODEN_SLAB", "oak_slab", "DOUBLE_WOODEN_SLAB",
                "oak_slab", "STONE_SLAB", "smooth_stone_slab", "DOUBLE_STONE_SLAB", "smooth_stone_slab", "WOODEN_BUTTON", "oak_button",
                "WOODEN_PRESSURE_PLATE", "oak_pressure_plate", "FLOWING_WATER", "water", "FLOWING_LAVA", "lava", "SNOW_LAYER", "snow", "SNOW",
                "snow_block", "QUARTZ_ORE", "nether_quartz_ore", "LIT_REDSTONE_LAMP", "redstone_lamp", "LIT_REDSTONE_ORE", "redstone_ore",
                "UNLIT_REDSTONE_TORCH", "redstone_torch", "POWERED_REPEATER", "repeater", "UNPOWERED_REPEATER", "repeater", "POWERED_COMPARATOR",
                "comparator", "UNPOWERED_COMPARATOR", "comparator", "DAYLIGHT_DETECTOR_INVERTED", "daylight_detector", "STANDING_SIGN", "oak_sign",
                "WALL_SIGN", "oak_wall_sign", "FROSTED_ICE", "frosted_ice", "PISTON_EXTENSION", "moving_piston", "WATERLILY", "lily_pad",
                "MELON_BLOCK", "melon", "GOLDEN_RAIL", "powered_rail", "NOTEBLOCK", "note_block", "TRAPDOOR", "oak_trapdoor", "END_BRICKS",
                "end_stone_bricks", "GRASS_PATH", "dirt_path", "CONCRETE", "white_concrete", "CONCRETE_POWDER", "white_concrete_powder",
                "CARPET", "white_carpet", "RED_SANDSTONE_STAIRS", "red_sandstone_stairs", "OAK_STAIRS", "oak_stairs", "BREWING_STAND",
                "brewing_stand", "COBBLESTONE_WALL", "cobblestone_wall", "SKULL", "skeleton_skull", "BED", "red_bed", "FLOWER_POT", "flower_pot",
                "PUMPKIN", "carved_pumpkin", "LIT_PUMPKIN", "jack_o_lantern", "MONSTER_EGG", "infested_stone", "SILVER_SHULKER_BOX", "light_gray_shulker_box",
                "PURPLE_SHULKER_BOX", "purple_shulker_box"};
        for (int i = 0; i < blocks.length; i += 2) BLOCK_NAMES.put(blocks[i], blocks[i + 1]);
        String[] items = {"REEDS", "sugar_cane", "NETHERBRICK", "nether_brick", "SPECKLED_MELON", "glistering_melon_slice", "MELON", "melon_slice",
                "SKULL", "skeleton_skull", "FIREWORK_CHARGE", "firework_star", "FIREWORKS", "firework_rocket", "COOKED_FISH", "cooked_cod",
                "FISH", "cod", "EMPTY_MAP", "map", "EYE_OF_ENDER", "ender_eye", "POTIONITEM", "potion", "SIGN", "oak_sign", "BED", "red_bed",
                "BOAT", "oak_boat", "OAK_DOOR", "oak_door", "WOODEN_DOOR", "oak_door", "DYE", "bone_meal", "COMMAND_BLOCK_MINECART", "command_block_minecart",
                "CHORUS_FRUIT_POPPED", "popped_chorus_fruit", "BANNER", "white_banner", "BOOK", "book", "WRITABLE_BOOK", "writable_book",
                "RECORD_13", "music_disc_13", "RECORD_CAT", "music_disc_cat", "RECORD_BLOCKS", "music_disc_blocks", "RECORD_CHIRP", "music_disc_chirp",
                "RECORD_FAR", "music_disc_far", "RECORD_MALL", "music_disc_mall", "RECORD_MELLOHI", "music_disc_mellohi", "RECORD_STAL", "music_disc_stal",
                "RECORD_STRAD", "music_disc_strad", "RECORD_WARD", "music_disc_ward", "RECORD_11", "music_disc_11", "RECORD_WAIT", "music_disc_wait",
                "SPAWN_EGG", "pig_spawn_egg", "MUTTON", "mutton", "BEEF", "beef", "CHICKEN", "chicken", "PORKCHOP", "porkchop", "REDSTONE", "redstone",
                "COMPARATOR", "comparator", "REPEATER", "repeater", "TOTEM_OF_UNDYING", "totem_of_undying", "ENCHANTED_BOOK", "enchanted_book",
                "MILK_BUCKET", "milk_bucket", "SKELETON_SKULL", "skeleton_skull"};
        for (int i = 0; i < items.length; i += 2) ITEM_NAMES.put(items[i], items[i + 1]);
    }

    private Vanilla1122() {}

    private static ResourceLocation find(net.minecraft.core.Registry<?> registry, String path) {
        ResourceLocation id = ResourceLocation.withDefaultNamespace(path);
        if (registry.containsKey(id)) return id;
        for (String prefix : new String[] {"oak_", "white_"}) {
            id = ResourceLocation.withDefaultNamespace(prefix + path);
            if (registry.containsKey(id)) return id;
        }
        return null;
    }

    public static Block block(String field) {
        String legacy = field.toLowerCase(java.util.Locale.ROOT);
        VanillaBlock b = new VanillaBlock(field.equals("AIR") ? Material.AIR : Material.ROCK);
        b.setRegistryName("minecraft", legacy);
        ResourceLocation id = find(BuiltInRegistries.BLOCK, BLOCK_NAMES.getOrDefault(field, legacy));
        if (id != null) {
            var modern = BuiltInRegistries.BLOCK.get(id);
            b.mcc$modern = modern;
            synchronized (Vanilla1122.class) {
                BLOCK_REVERSE.putIfAbsent(modern, b);
            }
        }
        Registries1122.BLOCKS.registerVanilla(b);
        return b;
    }

    public static synchronized Item item(String field) {
        String legacy = field.toLowerCase(java.util.Locale.ROOT);
        VanillaItem i = new VanillaItem();
        i.setRegistryName("minecraft", legacy);
        ResourceLocation id = find(BuiltInRegistries.ITEM, ITEM_NAMES.getOrDefault(field, legacy));
        if (id != null) {
            var modern = BuiltInRegistries.ITEM.get(id);
            i.mcc$modern = modern;
            ITEM_REVERSE.putIfAbsent(modern, i);
            i.mcc$vanillaModern = modern;
        }
        Registries1122.ITEMS.registerVanilla(i);
        return i;
    }

    /** A vanilla sound: the field name (BLOCK_STONE_BREAK) is the sound id with dots (block.stone.break), or underscores inside a word. */
    public static legacyconnector.v1_12_2.net.minecraft.util.SoundEvent sound(String field) {
        String[] words = field.toLowerCase(java.util.Locale.ROOT).split("_");
        String id = joinSound(words, 1, words[0]);
        var name = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft", id == null ? field.toLowerCase(java.util.Locale.ROOT).replace('_', '.') : id);
        var sound = new legacyconnector.v1_12_2.net.minecraft.util.SoundEvent(name);
        sound.setRegistryName(name);
        if (id != null) sound.mcc$modern = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.withDefaultNamespace(id));
        return sound;
    }

    /** Tries "." and "_" between the remaining words until the result is a sound that exists. */
    private static String joinSound(String[] words, int index, String soFar) {
        if (index == words.length) {
            return BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.withDefaultNamespace(soFar)) ? soFar : null;
        }
        String r = joinSound(words, index + 1, soFar + "." + words[index]);
        return r != null ? r : joinSound(words, index + 1, soFar + "_" + words[index]);
    }

    public static legacyconnector.v1_12_2.net.minecraft.potion.Potion effect(String field) {
        var p = new VanillaPotion();
        p.setRegistryName("minecraft", field.toLowerCase(java.util.Locale.ROOT));
        return p;
    }

    public static legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment enchantment(String field) {
        var e = new VanillaEnchantment();
        e.setRegistryName("minecraft", field.toLowerCase(java.util.Locale.ROOT));
        return e;
    }

    /** A vanilla potion effect as a legacy object. */
    public static final class VanillaPotion extends legacyconnector.v1_12_2.net.minecraft.potion.Potion {
        VanillaPotion() {
            super(false, 0);
        }
    }

    /** A vanilla enchantment as a legacy object. */
    public static final class VanillaEnchantment extends legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment {
        VanillaEnchantment() {
            super(Rarity.COMMON, legacyconnector.v1_12_2.net.minecraft.enchantment.EnumEnchantmentType.ALL,
                    new legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot[0]);
        }
    }

    /**
     * Vanilla constants typed with a class only the synthesizer provides (Blocks.GRASS is a BlockGrass): an instance of that class,
     * made like the plain wrappers. Called by the synthesized static-field accessor on first read.
     */
    public static Object resolveStatic(String key, Class<?> accessor) {
        // the accessor classes of the other legacy versions share these keys
        if (!accessor.getName().startsWith("legacyconnector_synth.v1_12_2.")) return null;
        boolean blocks = key.startsWith("net.minecraft.init.Blocks.");
        boolean items = key.startsWith("net.minecraft.init.Items.");
        String field = key.substring(key.lastIndexOf('.') + 1);
        java.lang.reflect.Method getter = null;
        for (var m : accessor.getDeclaredMethods()) if (m.getName().equals("sget$" + field)) getter = m;
        if (getter == null) return null;
        Class<?> type = getter.getReturnType();
        if (field.equals("REGISTRY") && key.equals("net.minecraft.potion.PotionType.REGISTRY")) return Registries1122.view(Registries1122.POTION_TYPES, "empty");
        if (type.getSuperclass() == Enum.class && type.getName().startsWith("legacyconnector_synth.v1_12_2.")) return enumConstant(type, field);
        if (accessor.getName().equals(type.getName() + "$$Missing") && !blocks && !items && type.getName().startsWith("legacyconnector_synth.v1_12_2.")
                && !java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
            // a constant of its own class (BiomeDictionary.Type.FOREST, ...): an instance per name keeps identity and equality working
            return enumConstant(type, field);
        }
        if (!blocks && !items && key.startsWith("net.minecraft.init.") && type.getName().startsWith("legacyconnector_synth.v1_12_2.")
                && !java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
            // Biomes.PLAINS, PotionTypes.WATER, ...: a distinct inert object per name
            return enumConstant(type, field);
        }
        if (!blocks && !items) {
            // a state property of a vanilla block class (BlockFarmland.MOISTURE): mods read these from the states of their own blocks
            String name = field.toLowerCase(java.util.Locale.ROOT);
            if (key.startsWith("net.minecraft.block.")) {
                if (type == legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger.class) {
                    return legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger.create(name, 0, 15);
                }
                if (type == legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool.class) {
                    return legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool.create(name);
                }
                if (type == legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection.class) {
                    return legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection.create(name);
                }
            }
            return null;
        }
        Object o = instantiate(type);
        String legacy = field.toLowerCase(java.util.Locale.ROOT);
        if (o instanceof Block b) {
            b.setRegistryName("minecraft", legacy);
            ResourceLocation id = find(BuiltInRegistries.BLOCK, BLOCK_NAMES.getOrDefault(field, legacy));
            if (id != null) {
                var modern = BuiltInRegistries.BLOCK.get(id);
                b.mcc$modern = modern;
                synchronized (Vanilla1122.class) {
                    BLOCK_REVERSE.putIfAbsent(modern, b);
                }
            }
            Registries1122.BLOCKS.registerVanilla(b);
        } else if (o instanceof Item i) {
            i.setRegistryName("minecraft", legacy);
            ResourceLocation id = find(BuiltInRegistries.ITEM, ITEM_NAMES.getOrDefault(field, legacy));
            if (id != null) {
                var modern = BuiltInRegistries.ITEM.get(id);
                i.mcc$modern = modern;
                i.mcc$vanillaModern = modern;
                synchronized (Vanilla1122.class) {
                    ITEM_REVERSE.putIfAbsent(modern, i);
                }
            }
            Registries1122.ITEMS.registerVanilla(i);
        }
        return o;
    }

    private static final Map<Class<?>, Map<String, Object>> ENUM_CONSTANTS = new HashMap<>();
    private static Map<String, java.util.List<String>> enumFieldNames;

    /** All constants of a synthesized enum: the static fields of its own type that the legacy field list names. */
    @SuppressWarnings("unchecked")
    public static synchronized <T> T[] enumValues(Class<T> type) {
        if (enumFieldNames == null) {
            enumFieldNames = new HashMap<>();
            for (ClassLoader loader : new ClassLoader[] {Vanilla1122.class.getClassLoader(), Thread.currentThread().getContextClassLoader()}) {
                if (loader == null) continue;
                try (var in = loader.getResourceAsStream("legacyconnector/legacy/1.12.2/fields.txt")) {
                    if (in == null) continue;
                    var reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] p = line.split(" ");
                        if (p.length >= 4 && p[3].equals("L" + p[1] + ";")) {
                            var names = enumFieldNames.computeIfAbsent(p[1], k -> new java.util.ArrayList<>());
                            if (!names.contains(p[2])) names.add(p[2]);
                        }
                    }
                    break;
                } catch (java.io.IOException e) {
                    // no constants known
                }
            }
        }
        String internal = type.getName().replace("legacyconnector_synth.v1_12_2.", "").replace('.', '/');
        var names = enumFieldNames.get(internal);
        if (names == null) return (T[]) java.lang.reflect.Array.newInstance(type, 0);
        T[] out = (T[]) java.lang.reflect.Array.newInstance(type, names.size());
        for (int i = 0; i < out.length; i++) out[i] = (T) enumConstant(type, names.get(i));
        return out;
    }


    /** A constant of a synthesized enum (BlockPlanks.EnumType.ACACIA): created without a constructor, named and numbered in first-use order. */
    private static synchronized Object enumConstant(Class<?> type, String name) {
        Map<String, Object> constants = ENUM_CONSTANTS.computeIfAbsent(type, k -> new java.util.LinkedHashMap<>());
        Object existing = constants.get(name);
        if (existing != null) return existing;
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
            Object constant = unsafe.allocateInstance(type);
            if (Enum.class.isAssignableFrom(type)) {
                unsafe.putObject(constant, unsafe.objectFieldOffset(Enum.class.getDeclaredField("name")), name);
                unsafe.putInt(constant, unsafe.objectFieldOffset(Enum.class.getDeclaredField("ordinal")), constants.size());
            }
            constants.put(name, constant);
            return constant;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static final Map<Class<?>, Class<?>> CONCRETE = new HashMap<>();

    /** A concrete subclass of an abstract synthesized class (Blocks.LEAVES is a BlockLeaves, which is abstract), defined next to it. */
    private static synchronized Class<?> concreteSubclass(Class<?> parent) {
        Class<?> existing = CONCRETE.get(parent);
        if (existing != null) return existing;
        try {
            java.lang.reflect.Constructor<?>[] ctors = parent.getDeclaredConstructors();
            java.lang.reflect.Constructor<?> smallest = null;
            for (var c : ctors) {
                if (!java.lang.reflect.Modifier.isPrivate(c.getModifiers()) && (smallest == null || c.getParameterCount() < smallest.getParameterCount())) smallest = c;
            }
            if (smallest == null) return null;
            String name = parent.getName().replace('.', '/') + "$Concrete";
            String superName = parent.getName().replace('.', '/');
            org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES | org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
            cw.visit(org.objectweb.asm.Opcodes.V21, org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_SUPER, name, null, superName, null);
            var mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
            for (Class<?> p : smallest.getParameterTypes()) {
                if (p == boolean.class || p == int.class || p == short.class || p == byte.class || p == char.class) mv.visitInsn(org.objectweb.asm.Opcodes.ICONST_0);
                else if (p == long.class) mv.visitInsn(org.objectweb.asm.Opcodes.LCONST_0);
                else if (p == float.class) mv.visitInsn(org.objectweb.asm.Opcodes.FCONST_0);
                else if (p == double.class) mv.visitInsn(org.objectweb.asm.Opcodes.DCONST_0);
                else mv.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
            }
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, superName, "<init>", org.objectweb.asm.Type.getConstructorDescriptor(smallest), false);
            mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            cw.visitEnd();
            Class<?> defined = java.lang.invoke.MethodHandles.privateLookupIn(parent, java.lang.invoke.MethodHandles.lookup()).defineClass(cw.toByteArray());
            CONCRETE.put(parent, defined);
            return defined;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** A new instance of a synthesized class: its smallest constructor with default arguments, or an uninitialized object. */
    private static Object instantiate(Class<?> type) {
        if (java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
            Class<?> concrete = concreteSubclass(type);
            if (concrete == null) return null;
            type = concrete;
        }
        var ctors = type.getDeclaredConstructors();
        java.util.Arrays.sort(ctors, java.util.Comparator.comparingInt(java.lang.reflect.Constructor::getParameterCount));
        for (var c : ctors) {
            try {
                Class<?>[] p = c.getParameterTypes();
                Object[] args = new Object[p.length];
                for (int k = 0; k < p.length; k++) {
                    if (p[k] == boolean.class) args[k] = false;
                    else if (p[k] == float.class) args[k] = 0F;
                    else if (p[k] == double.class) args[k] = 0D;
                    else if (p[k] == long.class) args[k] = 0L;
                    else if (p[k].isPrimitive()) args[k] = (byte) 0;
                    else args[k] = null;
                }
                if (p.length > 0) {
                    for (int k = 0; k < p.length; k++) {
                        if (p[k] == int.class) args[k] = 0;
                        if (p[k] == short.class) args[k] = (short) 0;
                        if (p[k] == byte.class) args[k] = (byte) 0;
                        if (p[k] == char.class) args[k] = (char) 0;
                    }
                }
                c.setAccessible(true);
                return c.newInstance(args);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                // try the next constructor
            }
        }
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return ((sun.misc.Unsafe) f.get(null)).allocateInstance(type);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** A vanilla biome as a legacy object (named by its field; properties are not tracked). */
    public static legacyconnector.v1_12_2.net.minecraft.world.biome.Biome biome(String field) {
        var b = new legacyconnector.v1_12_2.net.minecraft.world.biome.Biome(new legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.BiomeProperties(field.toLowerCase(java.util.Locale.ROOT)));
        b.setRegistryName("minecraft", field.toLowerCase(java.util.Locale.ROOT));
        return b;
    }

    private static boolean loaded;

    /** Creates the generated Blocks/Items wrappers once (they register themselves by name). */
    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            Class.forName("legacyconnector.v1_12_2.net.minecraft.init.Blocks", true, Vanilla1122.class.getClassLoader());
            Class.forName("legacyconnector.v1_12_2.net.minecraft.init.Items", true, Vanilla1122.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            // the lookups simply find nothing
        }
    }

    /** A vanilla block by its 1.12 registry path (stone, grass, log...). */
    public static Block blockByName(String legacyPath) {
        String field = legacyPath.toUpperCase(java.util.Locale.ROOT);
        ResourceLocation modern = find(BuiltInRegistries.BLOCK, BLOCK_NAMES.getOrDefault(field, legacyPath));
        if (modern == null || BuiltInRegistries.BLOCK.get(modern) == net.minecraft.world.level.block.Blocks.AIR && !legacyPath.equals("air")) return null;
        return block(field);
    }

    public static Item itemByName(String legacyPath) {
        String field = legacyPath.toUpperCase(java.util.Locale.ROOT);
        ResourceLocation modern = find(BuiltInRegistries.ITEM, ITEM_NAMES.getOrDefault(field, legacyPath));
        if (modern == null || BuiltInRegistries.ITEM.get(modern) == net.minecraft.world.item.Items.AIR && !legacyPath.equals("air")) return null;
        return item(field);
    }

    public static legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState airState() {
        return legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR.getDefaultState();
    }

    public static synchronized Block legacyBlockOf(net.minecraft.world.level.block.Block modern) {
        return BLOCK_REVERSE.get(modern);
    }

    /** The legacy wrapper of a modern vanilla item, or null. */
    public static synchronized Item legacyOf(net.minecraft.world.item.Item modern) {
        return ITEM_REVERSE.get(modern);
    }

    /** A vanilla block as a legacy object. */
    public static final class VanillaBlock extends Block {
        VanillaBlock(Material material) {
            super(material);
        }
    }

    /** A vanilla item as a legacy object. */
    public static final class VanillaItem extends Item {
    }
}
