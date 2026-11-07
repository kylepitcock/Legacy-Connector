package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;
import legacyconnector.v1_7_10.net.minecraft.util.RegistryNamespaced;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.flatten.VanillaFlattening;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identity and state translation between legacy blocks and modern blocks.
 *
 * <ul>
 *     <li>Mod blocks: one modern {@link LegacyBlockAdapter} per legacy block; legacy metadata is the adapter's
 *     {@code meta} property, so {@code (block, meta)} maps 1:1 to a block state. A metadata value that has a subtype item of its
 *     own lives in a flattened subtype block ({@code mod:name_N}, see {@link BlockFlattening1710}) whose default state it is.</li>
 *     <li>Vanilla blocks: wrappers whose metadata is translated through {@link VanillaFlattening}.</li>
 *     <li>Modern-only blocks (new vanilla blocks, other modern mods): wrappers with a single metadata value.</li>
 * </ul>
 */
public final class BlockBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Blocks");
    /**
     * First numeric id handed to mod blocks (vanilla 1.7.10 used 0-197 for blocks). Blocks and items shared one id space in 1.7.10 (an ItemBlock had
     * its block's id), so mods look an item up by Block.getIdFromBlock (Thaumcraft's Thaumometer shows a scanned node that way): a block id inside
     * the vanilla item ids 256-422 and the records 2256-2267 resolved to that vanilla item (an aura node scanned as a golden sword).
     */
    private static final int FIRST_MOD_ID = 423;
    /** Ids of modern-only blocks that cannot share their item's id: this plus the modern block registry id (see ItemBridge.modernOnlyBlockId). */
    /**
     * Above every modern-only item id (ItemBridge's 20000 + item registry id): ATM10 has more than 20,000 items, and with a base of
     * 40000 the two ranges overlapped, so Thaumcraft's scans (block ids turned back into items) named unrelated blocks and items.
     */
    static final int MODERN_BLOCK_ID_BASE = 1_000_000;

    private static final CaseIndexedMap<Block> BY_NAME = new CaseIndexedMap<>();
    private static final Map<Block, String> NAMES = new IdentityHashMap<>();
    private static final Map<Integer, Block> BY_ID = new HashMap<>();
    private static final Map<Block, Integer> IDS = new IdentityHashMap<>();
    /** Wrappers for modern blocks without a legacy id, and vanilla wrappers by modern block. */
    private static final Map<net.minecraft.world.level.block.Block, Block> MODERN_WRAPPERS = new IdentityHashMap<>();
    private static final List<Block> MOD_BLOCKS = new ArrayList<>();
    private static int nextId = FIRST_MOD_ID;

    private BlockBridge() {}

    /** The registered objects, copied under the registry lock: other threads register items while a tab or JEI iterates. */
    private static synchronized List<Object> snapshot() {
        return new ArrayList<Object>(BY_NAME.values());
    }


    public static RegistryNamespaced legacyRegistry() {
        return new legacyconnector.v1_7_10.cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry<Block>(name -> byName(name), o -> nameOf((Block) o), BlockBridge::byId, o -> idOf((Block) o),
                () -> all().iterator());
    }

    /**
     * Every registered block, vanilla ones included: their wrappers are made when the legacy Blocks class first loads, so a
     * mod iterating the registry (MineTweaker's {@code <minecraft:...>} brackets) before anything touched them saw only mod blocks.
     */
    public static List<Object> all() {
        ensureVanilla();
        return snapshot();
    }

    public static List<Block> modBlocks() {
        return Collections.unmodifiableList(MOD_BLOCKS);
    }

    // ------------------------------------------------------------------ vanilla wrappers


    /** 1.7.10 runtime classes of vanilla blocks whose declared type is less specific (mods cast to these). */
    private static final Map<String, String> RUNTIME_CLASSES = Map.ofEntries(
            Map.entry("stone", "BlockStone"), Map.entry("dirt", "BlockDirt"), Map.entry("planks", "BlockWood"),
            Map.entry("sapling", "BlockSapling"), Map.entry("flowing_water", "BlockDynamicLiquid"), Map.entry("water", "BlockStaticLiquid"),
            Map.entry("flowing_lava", "BlockDynamicLiquid"), Map.entry("lava", "BlockStaticLiquid"), Map.entry("gravel", "BlockGravel"),
            Map.entry("gold_ore", "BlockOre"), Map.entry("iron_ore", "BlockOre"), Map.entry("coal_ore", "BlockOre"),
            Map.entry("log", "BlockOldLog"), Map.entry("log2", "BlockNewLog"), Map.entry("leaves", "BlockOldLeaf"), Map.entry("leaves2", "BlockNewLeaf"),
            Map.entry("sponge", "BlockSponge"), Map.entry("glass", "BlockGlass"), Map.entry("lapis_ore", "BlockOre"), Map.entry("lapis_block", "BlockCompressed"),
            Map.entry("dispenser", "BlockDispenser"), Map.entry("sandstone", "BlockSandStone"), Map.entry("noteblock", "BlockNote"),
            Map.entry("bed", "BlockBed"), Map.entry("golden_rail", "BlockRailPowered"), Map.entry("detector_rail", "BlockRailDetector"),
            Map.entry("web", "BlockWeb"), Map.entry("wool", "BlockColored"), Map.entry("brown_mushroom", "BlockMushroom"),
            Map.entry("red_mushroom", "BlockMushroom"), Map.entry("gold_block", "BlockCompressed"), Map.entry("iron_block", "BlockCompressed"),
            Map.entry("double_stone_slab", "BlockStoneSlab"), Map.entry("stone_slab", "BlockStoneSlab"), Map.entry("tnt", "BlockTNT"),
            Map.entry("bookshelf", "BlockBookshelf"), Map.entry("obsidian", "BlockObsidian"), Map.entry("torch", "BlockTorch"),
            Map.entry("mob_spawner", "BlockMobSpawner"), Map.entry("oak_stairs", "BlockStairs"), Map.entry("diamond_ore", "BlockOre"),
            Map.entry("diamond_block", "BlockCompressed"), Map.entry("crafting_table", "BlockWorkbench"), Map.entry("wheat", "BlockCrops"),
            Map.entry("farmland", "BlockFarmland"), Map.entry("furnace", "BlockFurnace"), Map.entry("lit_furnace", "BlockFurnace"),
            Map.entry("standing_sign", "BlockSign"), Map.entry("wooden_door", "BlockDoor"), Map.entry("ladder", "BlockLadder"),
            Map.entry("rail", "BlockRail"), Map.entry("stone_stairs", "BlockStairs"), Map.entry("wall_sign", "BlockSign"),
            Map.entry("lever", "BlockLever"), Map.entry("stone_pressure_plate", "BlockPressurePlate"), Map.entry("iron_door", "BlockDoor"),
            Map.entry("wooden_pressure_plate", "BlockPressurePlate"), Map.entry("redstone_ore", "BlockRedstoneOre"),
            Map.entry("lit_redstone_ore", "BlockRedstoneOre"), Map.entry("unlit_redstone_torch", "BlockRedstoneTorch"),
            Map.entry("redstone_torch", "BlockRedstoneTorch"), Map.entry("stone_button", "BlockButtonStone"), Map.entry("snow_layer", "BlockSnow"),
            Map.entry("ice", "BlockIce"), Map.entry("snow", "BlockSnowBlock"), Map.entry("cactus", "BlockCactus"), Map.entry("clay", "BlockClay"),
            Map.entry("reeds", "BlockReed"), Map.entry("jukebox", "BlockJukebox"), Map.entry("fence", "BlockFence"), Map.entry("pumpkin", "BlockPumpkin"),
            Map.entry("netherrack", "BlockNetherrack"), Map.entry("soul_sand", "BlockSoulSand"), Map.entry("glowstone", "BlockGlowstone"),
            Map.entry("lit_pumpkin", "BlockPumpkin"), Map.entry("cake", "BlockCake"), Map.entry("trapdoor", "BlockTrapDoor"),
            Map.entry("monster_egg", "BlockSilverfish"), Map.entry("stonebrick", "BlockStoneBrick"), Map.entry("brown_mushroom_block", "BlockHugeMushroom"),
            Map.entry("red_mushroom_block", "BlockHugeMushroom"), Map.entry("iron_bars", "BlockPane"), Map.entry("glass_pane", "BlockPane"),
            Map.entry("melon_block", "BlockMelon"), Map.entry("pumpkin_stem", "BlockStem"), Map.entry("melon_stem", "BlockStem"),
            Map.entry("vine", "BlockVine"), Map.entry("fence_gate", "BlockFenceGate"), Map.entry("brick_stairs", "BlockStairs"),
            Map.entry("stone_brick_stairs", "BlockStairs"), Map.entry("waterlily", "BlockLilyPad"), Map.entry("nether_brick_fence", "BlockFence"),
            Map.entry("nether_brick_stairs", "BlockStairs"), Map.entry("nether_wart", "BlockNetherWart"), Map.entry("enchanting_table", "BlockEnchantmentTable"),
            Map.entry("end_portal", "BlockEndPortal"), Map.entry("end_portal_frame", "BlockEndPortalFrame"), Map.entry("dragon_egg", "BlockDragonEgg"),
            Map.entry("redstone_lamp", "BlockRedstoneLight"), Map.entry("lit_redstone_lamp", "BlockRedstoneLight"),
            Map.entry("double_wooden_slab", "BlockWoodSlab"), Map.entry("wooden_slab", "BlockWoodSlab"), Map.entry("cocoa", "BlockCocoa"),
            Map.entry("sandstone_stairs", "BlockStairs"), Map.entry("emerald_ore", "BlockOre"), Map.entry("ender_chest", "BlockEnderChest"),
            Map.entry("tripwire", "BlockTripWire"), Map.entry("emerald_block", "BlockCompressed"), Map.entry("spruce_stairs", "BlockStairs"),
            Map.entry("birch_stairs", "BlockStairs"), Map.entry("jungle_stairs", "BlockStairs"), Map.entry("command_block", "BlockCommandBlock"),
            Map.entry("cobblestone_wall", "BlockWall"), Map.entry("flower_pot", "BlockFlowerPot"), Map.entry("carrots", "BlockCarrot"),
            Map.entry("potatoes", "BlockPotato"), Map.entry("wooden_button", "BlockButtonWood"), Map.entry("skull", "BlockSkull"),
            Map.entry("anvil", "BlockAnvil"), Map.entry("trapped_chest", "BlockChest"), Map.entry("light_weighted_pressure_plate", "BlockPressurePlateWeighted"),
            Map.entry("heavy_weighted_pressure_plate", "BlockPressurePlateWeighted"), Map.entry("redstone_block", "BlockCompressedPowered"),
            Map.entry("quartz_ore", "BlockOre"), Map.entry("quartz_block", "BlockQuartz"), Map.entry("quartz_stairs", "BlockStairs"),
            Map.entry("activator_rail", "BlockRailPowered"), Map.entry("dropper", "BlockDropper"), Map.entry("stained_hardened_clay", "BlockColored"),
            Map.entry("stained_glass_pane", "BlockStainedGlassPane"), Map.entry("acacia_stairs", "BlockStairs"), Map.entry("dark_oak_stairs", "BlockStairs"),
            Map.entry("hay_block", "BlockHay"), Map.entry("carpet", "BlockCarpet"), Map.entry("hardened_clay", "BlockHardenedClay"),
            Map.entry("packed_ice", "BlockPackedIce"), Map.entry("double_plant", "BlockDoublePlant"), Map.entry("stained_glass", "BlockStainedGlass"));

    /** The legacy runtime class for a vanilla block if it exists (hand-written or synthesized) and fits the declared type. */
    @SuppressWarnings("unchecked")
    private static <T extends Block> Class<? extends T> runtimeClass(String legacyName, Class<T> declared) {
        String simple = RUNTIME_CLASSES.get(legacyName);
        if (simple == null) return declared;
        for (String prefix : org.kylepitcock.legacyconnector.compat.jdk.LegacyPrefixes.api1710("net.minecraft.block.")) {
            try {
                Class<?> c = Class.forName(prefix + simple, false, Block.class.getClassLoader());
                if (declared.isAssignableFrom(c) && !java.lang.reflect.Modifier.isAbstract(c.getModifiers())) return (Class<? extends T>) c;
            } catch (ClassNotFoundException | LinkageError ignored) {
                // not part of this modset's API
            }
        }
        return declared;
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends Block> T vanilla(String legacyName, Class<T> type) {
        String full = legacyName.indexOf(':') < 0 ? "minecraft:" + legacyName : legacyName;
        Block existing = BY_NAME.get(full);
        if (existing != null) return (T) existing;
        int id = VanillaFlattening.blockId(full);
        VanillaInfo info = new VanillaInfo(full, id, null);
        try {
            Class<? extends T> cls = runtimeClass(full.substring(full.indexOf(':') + 1), type);
            Constructor<? extends T> c;
            try {
                c = cls.getConstructor(VanillaInfo.class);
            } catch (NoSuchMethodException e) {
                c = type.getConstructor(VanillaInfo.class);
            }
            T block = c.newInstance(info);
            BY_NAME.put(full, block);
            NAMES.put(block, full);
            if (id >= 0) {
                BY_ID.putIfAbsent(id, block);
                IDS.put(block, id);
            }
            return block;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create vanilla wrapper " + type + " for " + legacyName, e);
        }
    }

    /** A mod-made block that replaced a vanilla singleton takes over the vanilla block's identity. */
    public static synchronized void adoptVanilla(Block replacement, Block original) {
        VanillaInfo info = original.mcc$vanilla;
        replacement.mcc$vanilla = info;
        BY_NAME.put(info.legacyName(), replacement);
        NAMES.put(replacement, info.legacyName());
        if (info.legacyId() >= 0) {
            BY_ID.put(info.legacyId(), replacement);
            IDS.put(replacement, info.legacyId());
        }
    }

    /** Called from the wrapper constructor: copies modern properties into the legacy fields. */
    public static void initVanilla(Block block, VanillaInfo info) {
        BlockState state = stateOfVanilla(info, 0);
        float hardness = state.getBlock().defaultDestroyTime();
        float resistance = state.getBlock().getExplosionResistance();
        block.mcc$setVanillaProperties(hardness, resistance, state.getLightEmission(), state.canOcclude() ? 255 : 0, state.isRandomlyTicking());
        MODERN_WRAPPERS.putIfAbsent(state.getBlock(), block);
    }

    public static Material vanillaMaterial(VanillaInfo info) {
        BlockState s = stateOfVanilla(info, 0);
        if (s.isAir()) return Material.air;
        // Tags (fluid tags too) are not bound while mods load: lava is told by its fluid type. Mods compare materials by identity
        // (Thaumcraft's crucible heats only over Material.lava or Material.fire), so these must be the shared constants.
        if (s.liquid()) return s.getFluidState().getType().isSame(net.minecraft.world.level.material.Fluids.LAVA) ? Material.lava : Material.water;
        // Block tags are not bound while mods load, so the material comes from the block's class, sound and name.
        var block = s.getBlock();
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
        if (block instanceof net.minecraft.world.level.block.BaseFireBlock) return Material.fire;
        if (block instanceof net.minecraft.world.level.block.NetherPortalBlock || block instanceof net.minecraft.world.level.block.EndPortalBlock) return Material.portal;
        if (block instanceof net.minecraft.world.level.block.TntBlock) return Material.tnt;
        if (block instanceof net.minecraft.world.level.block.CactusBlock) return Material.cactus;
        if (block instanceof net.minecraft.world.level.block.WebBlock) return Material.web;
        if (block instanceof net.minecraft.world.level.block.CakeBlock) return Material.cake;
        if (block instanceof net.minecraft.world.level.block.AnvilBlock) return Material.anvil;
        if (block instanceof net.minecraft.world.level.block.piston.PistonBaseBlock || block instanceof net.minecraft.world.level.block.piston.PistonHeadBlock) return Material.piston;
        if (block instanceof net.minecraft.world.level.block.CarpetBlock) return Material.carpet;
        if (block instanceof net.minecraft.world.level.block.SpongeBlock || block instanceof net.minecraft.world.level.block.WetSpongeBlock) return Material.sponge;
        if (block instanceof net.minecraft.world.level.block.DragonEggBlock) return Material.dragonEgg;
        if (path.equals("packed_ice") || path.equals("blue_ice")) return Material.packedIce;
        if (block instanceof net.minecraft.world.level.block.IceBlock || path.equals("ice")) return Material.ice;
        if (block instanceof net.minecraft.world.level.block.SnowLayerBlock) return Material.snow;
        if (path.equals("snow_block")) return Material.craftedSnow;
        if (path.equals("pumpkin") || path.equals("carved_pumpkin") || path.equals("jack_o_lantern") || path.equals("melon")) return Material.gourd;
        if (block instanceof net.minecraft.world.level.block.RedstoneLampBlock) return Material.redstoneLight;
        if (block instanceof net.minecraft.world.level.block.RedStoneWireBlock || block instanceof net.minecraft.world.level.block.DiodeBlock
                || block instanceof net.minecraft.world.level.block.LeverBlock || block instanceof net.minecraft.world.level.block.ButtonBlock
                || block instanceof net.minecraft.world.level.block.TorchBlock) {
            return Material.circuits;
        }
        var sound = s.getSoundType();
        if (block instanceof net.minecraft.world.level.block.LeavesBlock) return Material.leaves;
        if (block instanceof net.minecraft.world.level.block.SpreadingSnowyDirtBlock) return Material.grass;
        if (block instanceof net.minecraft.world.level.block.ColoredFallingBlock || path.endsWith("sand") && !path.contains("stone")) return Material.sand;
        if (sound == net.minecraft.world.level.block.SoundType.WOOL) return Material.cloth;
        if (path.equals("dirt") || path.equals("coarse_dirt") || path.equals("podzol") || path.equals("farmland") || path.equals("dirt_path")
                || path.equals("rooted_dirt")) {
            return Material.ground;
        }
        if (path.equals("clay")) return Material.clay;
        if (path.equals("gravel")) return Material.sand;
        if (sound == net.minecraft.world.level.block.SoundType.WOOD && block.defaultBlockState().blocksMotion()) return Material.wood;
        if (s.canBeReplaced() && !s.blocksMotion()) return Material.vine;
        if (!s.blocksMotion()) return Material.plants;
        if (s.getSoundType() == net.minecraft.world.level.block.SoundType.GLASS) return Material.glass;
        if (s.getSoundType() == net.minecraft.world.level.block.SoundType.METAL) return Material.iron;
        if (s.getSoundType() == net.minecraft.world.level.block.SoundType.WOOD) return Material.wood;
        return Material.rock;
    }

    private static BlockState stateOfVanilla(VanillaInfo info, int meta) {
        if (info.modern() instanceof net.minecraft.world.level.block.Block b) return b.defaultBlockState();
        return info.legacyId() >= 0 ? VanillaFlattening.state(info.legacyId(), meta) : Blocks.AIR.defaultBlockState();
    }

    // ------------------------------------------------------------------ mod registration

    public static synchronized void register(Block block, Class<? extends ItemBlock> itemClass, String name, String modId, Object[] itemArgs) {
        if (NAMES.containsKey(block)) {
            LOG.warn("Legacy block {} registered twice (as {})", block, name);
            return;
        }
        if (name == null) {
            // 1.5.2 and 1.6.4 let a mod register a block under its unlocalized name or no name at all (FML derived one)
            String unlocalized = block.getUnlocalizedName();
            name = unlocalized != null && !unlocalized.isEmpty() ? unlocalized.replaceFirst("^tile\\.", "") : block.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        }
        LegacyModContainer1710 owner = LegacyModContainer1710.active();
        String legacyDomain = modId != null ? modId : owner != null ? owner.getModId() : "minecraft";
        String namespace = owner != null && modId == null ? owner.record().modernId : Ids.modernModId(legacyDomain);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, Ids.normalizePath(name).replace('/', '_'));
        String legacyName = legacyDomain + ":" + name;

        LegacyBlockAdapter adapter = (LegacyBlockAdapter) LegacyRegistrar.register(BuiltInRegistries.BLOCK, id, (net.minecraft.world.level.block.Block) LegacyBlockAdapter.create(block));
        block.mcc$modern = adapter;
        BY_NAME.put(legacyName, block);
        NAMES.put(block, legacyName);
        int numeric = nextId++;
        if (numeric >= 2256 && numeric <= 2267) {
            nextId = 2268;
            numeric = nextId++;
        }
        BY_ID.put(numeric, block);
        IDS.put(block, numeric);
        MOD_BLOCKS.add(block);
        VanillaEquivalents.consider(block, legacyName);
        registerDeepVariant(block, adapter, id);

        if (itemClass != null) {
            ItemBlock item = instantiateItemBlock(itemClass, block, itemArgs);
            if (item != null) ItemBridge.registerBlockItem(item, block, id, legacyName);
        }
    }

    /**
     * Ores that generate deep (legacy y=0 is modern y=-64) get a deepslate twin, placed instead of the ore where the rock is
     * deepslate. The twin is the same legacy block (drops, hardness, behaviour), without an item of its own.
     */
    private static void registerDeepVariant(Block block, LegacyBlockAdapter adapter, ResourceLocation id) {
        if (!isOre(block, id.getPath()) || adapter instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyEntityBlockAdapter) return;
        try {
            LegacyBlockAdapter twin = LegacyBlockAdapter.create(block);
            LegacyRegistrar.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_deepslate"),
                    (net.minecraft.world.level.block.Block) twin);
            adapter.linkDeepVariant(twin);
            DEEP_VARIANTS.add(block);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("No deepslate variant for {}: {}", id, e.toString());
        }
    }

    private static final List<Block> DEEP_VARIANTS = new ArrayList<>();

    /** Blocks that have a deepslate twin. */
    public static synchronized List<Block> blocksWithDeepVariant() {
        return new ArrayList<>(DEEP_VARIANTS);
    }

    /**
     * A full rock block whose name has "ore" as a word ({@code oreCopper}, {@code copper_ore}, {@code tile.ore.tin}, not {@code core}).
     * The names are checked first: the block's own methods run during registration only for ore-named blocks (RotaryCraft's
     * getRenderType initialises its machine registry, which needs blocks not registered yet: the class failed for good and the mod with it).
     */
    static boolean isOre(Block block, String name) {
        if (!oreNamed(block, name)) return false;
        try {
            return block.getMaterial() == Material.rock && block.getRenderType() == 0;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private static boolean oreNamed(Block block, String name) {
        for (Class<?> c = block.getClass(); c != null && c != Block.class; c = c.getSuperclass()) {
            String n = c.getSimpleName();
            if (n.endsWith("Ore") || n.endsWith("Ores") || n.startsWith("BlockOre")) return true;
        }
        for (String word : name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(java.util.Locale.ROOT).split("[^a-z0-9]+")) {
            if (word.equals("ore") || word.equals("ores")) return true;
        }
        return false;
    }

    private static ItemBlock instantiateItemBlock(Class<? extends ItemBlock> cls, Block block, Object[] args) {
        Object[] full = new Object[(args == null ? 0 : args.length) + 1];
        full[0] = block;
        if (args != null) System.arraycopy(args, 0, full, 1, args.length);
        outer:
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length != full.length) continue;
            for (int i = 0; i < p.length; i++) {
                if (full[i] != null && !box(p[i]).isInstance(full[i])) continue outer;
            }
            try {
                c.setAccessible(true);
                return (ItemBlock) c.newInstance(full);
            } catch (ReflectiveOperationException e) {
                LOG.error("Could not construct item block {} for {}", cls.getName(), block, e);
                return null;
            }
        }
        // A 1.6.4 ItemBlock subclass takes the id of its block minus 256 where 1.7.10's takes the block (Forge 9.11's registerBlock(block, itemClass, name)).
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length != full.length || p[0] != int.class) continue;
            Object[] byId = full.clone();
            byId[0] = legacyconnector.v1_6_4.net.minecraft.util.Ids.blockId(block) - 256;
            boolean fits = true;
            for (int i = 1; i < p.length; i++) fits &= byId[i] == null || box(p[i]).isInstance(byId[i]);
            if (!fits) continue;
            try {
                c.setAccessible(true);
                return (ItemBlock) c.newInstance(byId);
            } catch (ReflectiveOperationException e) {
                LOG.error("Could not construct item block {} for {}", cls.getName(), block, e);
                return null;
            }
        }
        LOG.error("No matching constructor on {} for (Block{} extra args)", cls.getName(), args == null ? "" : ", " + args.length);
        return null;
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == boolean.class) return Boolean.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        if (c == long.class) return Long.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        return Character.class;
    }

    // ------------------------------------------------------------------ lookups

    public static synchronized Block byName(String name) {
        String full = name.indexOf(':') < 0 ? "minecraft:" + name : name;
        Block b = BY_NAME.get(full);
        if (b != null) return b;
        // Case-insensitive legacy ids ("IC2:blockOre") and modern names of other mods.
        Block ci = BY_NAME.getIgnoreCase(full);
        if (ci != null) return ci;
        // Configs name blocks by their unlocalized name ("Railcraft:tile.railcraft.ore") where the block was registered as "ore".
        int colon = full.indexOf(':');
        String path = full.substring(colon + 1);
        if (path.startsWith("tile.")) {
            String ns = full.substring(0, colon + 1);
            String bare = path.substring("tile.".length());
            Block stripped = BY_NAME.getIgnoreCase(ns + bare);
            if (stripped == null) {
                int dot = bare.indexOf('.');
                if (dot > 0 && bare.substring(0, dot).equalsIgnoreCase(full.substring(0, colon))) stripped = BY_NAME.getIgnoreCase(ns + bare.substring(dot + 1));
            }
            if (stripped != null) return stripped;
        }
        ResourceLocation rl = ResourceLocation.tryParse(full.toLowerCase(java.util.Locale.ROOT));
        if (rl != null && BuiltInRegistries.BLOCK.containsKey(rl)) return legacyBlock(BuiltInRegistries.BLOCK.get(rl).defaultBlockState());
        return null;
    }

    public static synchronized String nameOf(Block block) {
        String n = NAMES.get(block);
        if (n != null) return n;
        if (block != null && block.mcc$vanilla != null) return block.mcc$vanilla.legacyName();
        return null;
    }

    public static int idOf(Block block) {
        Integer id;
        synchronized (BlockBridge.class) {
            id = IDS.get(block);
        }
        if (id != null) return id;
        // A modern-only block (beehive, bee nest, other modern mods' blocks) gets an id that byId resolves back (its item's
        // where possible, as 1.7.10 ItemBlocks shared their block's id); -1 made Thaumcraft's scans look up a null item.
        // Looked up outside this class's lock (it takes ItemBridge's).
        return block != null && block.mcc$vanilla != null && block.mcc$vanilla.legacyId() < 0 ? ItemBridge.modernOnlyBlockId(block) : -1;
    }

    /** The legacy id registered for a block, or -1 (no modern-only id: ItemBridge.idOf uses it and must not recurse). */
    static synchronized int registeredIdOf(Block block) {
        Integer id = IDS.get(block);
        return id == null ? -1 : id;
    }

    /** The block registered under a numeric id, without resolving item ids (ItemBridge.byId uses it: the two must not call each other). */
    public static Block registeredById(int id) {
        Block b;
        synchronized (BlockBridge.class) {
            ensureVanilla();
            b = BY_ID.get(id);
        }
        return b != null ? b : modernById(id);
    }

    /** A modern-only block by an id in the {@link #MODERN_BLOCK_ID_BASE} range, or null. */
    private static Block modernById(int id) {
        if (id < MODERN_BLOCK_ID_BASE) return null;
        net.minecraft.world.level.block.Block modern = BuiltInRegistries.BLOCK.byId(id - MODERN_BLOCK_ID_BASE);
        if (modern == null || modern instanceof LegacyBlockAdapter) return null;
        Block b = legacyBlock(modern.defaultBlockState());
        return idOf(b) == id ? b : null;
    }

    public static Block byId(int id) {
        Block b = registeredById(id);
        if (b != null) return b;
        // 1.7.10 ItemBlocks share their block's id, so mods probe Block.blockRegistry with item ids
        // (ForgeMultipart's IMC check); item ids here are separate, so resolve them through the item.
        return id >= 4096 ? ItemBridge.blockOf(ItemBridge.byId(id)) : null;
    }

    /** The generated Blocks class creates every vanilla wrapper; make sure it ran before any lookup. */
    private static void ensureVanilla() {
        try {
            Class.forName("legacyconnector.v1_7_10.net.minecraft.init.Blocks", true, BlockBridge.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 1.7.10's {@code Block.getBlockFromItem} was {@code getBlockById(Item.getIdFromItem(item))} on FML's block registry, which
     * answers its default ({@code Blocks.air}) for an id without a block: an item that places no block (or null) gives air, never
     * null. Mods rely on it: DragonAPI's {@code BlockKey} throws on a null block, so its wood/ore lookups by item
     * ({@code ModWoodList.getModWood(ItemStack)}) failed for every plain item (ChromatiCraft's crystal furnace NEI handler).
     */
    public static Block fromItem(Item item) {
        Block b = item instanceof ItemBlock ib ? ib.mcc$block() : item == null ? null : ItemBridge.blockOf(item);
        if (b != null) return b;
        synchronized (BlockBridge.class) {
            ensureVanilla();
            return BY_ID.get(0);
        }
    }

    // ------------------------------------------------------------------ state translation

    /**
     * The legacy block of a modern state. Called for every legacy block read (World.getBlock) on the server thread and on every
     * world generation thread: the answer for a modern block never changes once made, so it is kept on the block itself
     * ({@code perf.blockLookupCache}) and only the first lookup per modern block takes the bridge's lock.
     */
    public static Block legacyBlock(BlockState state) {
        var modern = state.getBlock();
        if (modern instanceof LegacyBlockAdapter a) return (Block) a.legacy();
        if (modern == net.minecraft.world.level.block.Blocks.LAVA || modern == net.minecraft.world.level.block.Blocks.WATER) {
            // A source block is the "still" block, as 1.7.10 worlds held it (Blocks.lava, Blocks.water; only flowing liquid was flowing_*): legacy structure
            // checks compare against Blocks.lava (Thaumcraft's infernal furnace never formed, its middle lava was read as flowing_lava).
            Block still = stillLiquid(state);
            if (still != null) return still;
        }
        if (org.kylepitcock.legacyconnector.core.perf.Perf.BLOCK_LOOKUP_CACHE) {
            Object known = ((org.kylepitcock.legacyconnector.core.perf.LegacySlot) modern).legacyconnector$slot1710();
            if (known != null) return (Block) known;
            Block made = legacyBlockLocked(state);
            ((org.kylepitcock.legacyconnector.core.perf.LegacySlot) modern).legacyconnector$setSlot1710(made);
            return made;
        }
        return legacyBlockLocked(state);
    }

    private static Block stillLiquid(BlockState state) {
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) || state.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL) != 0) return null;
        ensureVanilla();
        synchronized (BlockBridge.class) {
            return BY_NAME.get(state.getBlock() == net.minecraft.world.level.block.Blocks.LAVA ? "minecraft:lava" : "minecraft:water");
        }
    }

    private static synchronized Block legacyBlockLocked(BlockState state) {
        if (state.getBlock() instanceof LegacyBlockAdapter a) return (Block) a.legacy();
        Block wrapper = MODERN_WRAPPERS.get(state.getBlock());
        if (wrapper != null) return wrapper;
        // void and cave air (above the build height, in caves) are 1.7.10's air: as wrappers they had a full collision box
        // (Mystcraft's linking raised the player through "solid" void air forever)
        if (state.isAir() && state.getBlock() != net.minecraft.world.level.block.Blocks.AIR) {
            ensureVanilla();
            Block air = BY_ID.get(0);
            if (air != null) {
                MODERN_WRAPPERS.put(state.getBlock(), air);
                return air;
            }
        }
        ensureVanilla();
        int key = VanillaFlattening.legacyKey(state);
        Block b = key >= 0 ? BY_ID.get(key >> 4) : null;
        if (b == null) {
            // Modern-only block (new vanilla content, other modern mods): a single-meta wrapper.
            ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            b = new Block(new VanillaInfo(rl.toString(), -1, state.getBlock()));
            BY_NAME.putIfAbsent(rl.toString(), b);
            NAMES.putIfAbsent(b, rl.toString());
        }
        MODERN_WRAPPERS.put(state.getBlock(), b);
        return b;
    }

    public static int legacyMeta(BlockState state) {
        if (state.getBlock() instanceof LegacyBlockAdapter) return LegacyBlockAdapter.metaOf(state);
        if (!org.kylepitcock.legacyconnector.core.perf.Perf.BLOCK_LOOKUP_CACHE) return vanillaMeta(state);
        // a vanilla state's metadata depends on the state alone (the flattening table): remembered on the state, plus one
        var slot = (org.kylepitcock.legacyconnector.core.perf.LegacyStateSlot) state;
        int known = slot.legacyconnector$meta1710();
        if (known != 0) return known - 1;
        int meta = vanillaMeta(state);
        slot.legacyconnector$setMeta1710(meta + 1);
        return meta;
    }

    private static int vanillaMeta(BlockState state) {
        Block wrapper = legacyBlock(state);
        if (wrapper.mcc$vanilla == null || wrapper.mcc$vanilla.legacyId() < 0) return 0;
        // nearest: a state 1.7.10 never had as such (a lower door half hinged right, a waterlogged stair) reads as the legacy state
        // sharing most of its properties, not as the block's first metadata (a door's facing would read as east)
        int key = VanillaFlattening.nearestLegacyKey(state);
        return key >= 0 && key >> 4 == wrapper.mcc$vanilla.legacyId() ? key & 15 : 0;
    }

    public static BlockState toModern(Block block, int meta) {
        if (block == null) return Blocks.AIR.defaultBlockState();
        var equivalent = VanillaEquivalents.block(block, meta);
        if (equivalent != null) return equivalent.defaultBlockState();
        if (block.mcc$modern instanceof LegacyBlockAdapter a) return a.stateForMeta(meta);
        if (block.mcc$vanilla != null) return stateOfVanilla(block.mcc$vanilla, meta);
        LOG.warn("Legacy block {} used in the world before it was registered; placing air", block);
        return Blocks.AIR.defaultBlockState();
    }

    /**
     * The legacy stack a pick-block of a vanilla (or modern-only) block state gives: the modern block's clone stack (birch leaves,
     * sugar cane, crop seeds, the wool of its colour) as a legacy stack, which maps back to the 1.7.10 item and damage that
     * mods key data by (Thaumcraft's aspects for leaves:2 or the reeds item). Null when the block has no item (fire, portals).
     */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemStack legacyPick(BlockState state, net.minecraft.world.level.LevelReader level,
            net.minecraft.core.BlockPos pos) {
        net.minecraft.world.item.ItemStack modern;
        try {
            modern = level != null ? state.getBlock().getCloneItemStack(level, pos, state) : new net.minecraft.world.item.ItemStack(state.getBlock());
        } catch (RuntimeException e) {
            modern = new net.minecraft.world.item.ItemStack(state.getBlock());
        }
        if (modern.isEmpty()) return null;
        return legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(modern.copyWithCount(1));
    }

    public static BlockState withMeta(BlockState current, int meta) {
        if (current.getBlock() instanceof LegacyBlockAdapter a) return a.stateForMeta(meta);
        return toModern(legacyBlock(current), meta);
    }

    public static net.minecraft.world.level.block.Block modernBlock(Block block) {
        return toModern(block, 0).getBlock();
    }
}
