package org.kylepitcock.mccompat.v1_7_10.bridge;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.block.material.Material;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemBlock;
import mccompat.v1_7_10.net.minecraft.util.RegistryNamespaced;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.mccompat.core.Ids;
import org.kylepitcock.mccompat.core.flatten.VanillaFlattening;
import org.kylepitcock.mccompat.core.registry.LegacyRegistrar;
import org.kylepitcock.mccompat.v1_7_10.LegacyModContainer1710;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identity and state translation between legacy blocks and modern blocks.
 *
 * <ul>
 *     <li>Mod blocks: one modern {@link LegacyBlockAdapter} per legacy block; legacy metadata is the adapter's
 *     {@code meta} property, so {@code (block, meta)} maps 1:1 to a block state.</li>
 *     <li>Vanilla blocks: wrappers whose metadata is translated through {@link VanillaFlattening}.</li>
 *     <li>Modern-only blocks (new vanilla blocks, other modern mods): wrappers with a single metadata value.</li>
 * </ul>
 */
public final class BlockBridge {
    private static final Logger LOG = LoggerFactory.getLogger("MCCompat/Blocks");
    /** First numeric id handed to mod blocks (vanilla 1.7.10 used 0-197). */
    private static final int FIRST_MOD_ID = 256;

    private static final Map<String, Block> BY_NAME = new LinkedHashMap<>();
    private static final Map<Block, String> NAMES = new IdentityHashMap<>();
    private static final Map<Integer, Block> BY_ID = new HashMap<>();
    private static final Map<Block, Integer> IDS = new IdentityHashMap<>();
    /** Wrappers for modern blocks without a legacy id, and vanilla wrappers by modern block. */
    private static final Map<net.minecraft.world.level.block.Block, Block> MODERN_WRAPPERS = new IdentityHashMap<>();
    private static final List<Block> MOD_BLOCKS = new ArrayList<>();
    private static int nextId = FIRST_MOD_ID;

    private BlockBridge() {}

    public static RegistryNamespaced legacyRegistry() {
        return new RegistryNamespaced(name -> byName(name), o -> nameOf((Block) o), BlockBridge::byId, o -> idOf((Block) o),
                () -> new ArrayList<Object>(BY_NAME.values()).iterator());
    }

    public static List<Block> modBlocks() {
        return Collections.unmodifiableList(MOD_BLOCKS);
    }

    // ------------------------------------------------------------------ vanilla wrappers

    @SuppressWarnings("unchecked")
    public static synchronized <T extends Block> T vanilla(String legacyName, Class<T> type) {
        String full = legacyName.indexOf(':') < 0 ? "minecraft:" + legacyName : legacyName;
        Block existing = BY_NAME.get(full);
        if (existing != null) return (T) existing;
        int id = VanillaFlattening.blockId(full);
        VanillaInfo info = new VanillaInfo(full, id, null);
        try {
            Constructor<T> c = type.getConstructor(VanillaInfo.class);
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
        if (s.liquid()) return s.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) ? Material.lava : Material.water;
        if (s.is(net.minecraft.tags.BlockTags.LEAVES)) return Material.leaves;
        if (s.is(net.minecraft.tags.BlockTags.LOGS) || s.is(net.minecraft.tags.BlockTags.PLANKS)) return Material.wood;
        if (s.is(net.minecraft.tags.BlockTags.SAND)) return Material.sand;
        if (s.is(net.minecraft.tags.BlockTags.WOOL)) return Material.cloth;
        if (s.is(net.minecraft.tags.BlockTags.DIRT)) return s.is(Blocks.GRASS_BLOCK) ? Material.grass : Material.ground;
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
        BY_ID.put(numeric, block);
        IDS.put(block, numeric);
        MOD_BLOCKS.add(block);

        if (itemClass != null) {
            ItemBlock item = instantiateItemBlock(itemClass, block, itemArgs);
            if (item != null) ItemBridge.registerBlockItem(item, block, id, legacyName);
        }
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
        for (var e : BY_NAME.entrySet()) if (e.getKey().equalsIgnoreCase(full)) return e.getValue();
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

    public static synchronized int idOf(Block block) {
        Integer id = IDS.get(block);
        return id == null ? -1 : id;
    }

    public static synchronized Block byId(int id) {
        ensureVanilla();
        return BY_ID.get(id);
    }

    /** The generated Blocks class creates every vanilla wrapper; make sure it ran before any lookup. */
    private static void ensureVanilla() {
        try {
            Class.forName("mccompat.v1_7_10.net.minecraft.init.Blocks", true, BlockBridge.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Block fromItem(Item item) {
        if (item instanceof ItemBlock ib) return ib.mcc$block();
        return ItemBridge.blockOf(item);
    }

    // ------------------------------------------------------------------ state translation

    public static synchronized Block legacyBlock(BlockState state) {
        if (state.getBlock() instanceof LegacyBlockAdapter a) return (Block) a.legacy();
        Block wrapper = MODERN_WRAPPERS.get(state.getBlock());
        if (wrapper != null) return wrapper;
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
        if (state.getBlock() instanceof LegacyBlockAdapter) return state.getValue(LegacyBlockAdapter.META);
        Block wrapper = legacyBlock(state);
        if (wrapper.mcc$vanilla == null || wrapper.mcc$vanilla.legacyId() < 0) return 0;
        int key = VanillaFlattening.legacyKey(state);
        return key >= 0 && key >> 4 == wrapper.mcc$vanilla.legacyId() ? key & 15 : 0;
    }

    public static BlockState toModern(Block block, int meta) {
        if (block == null) return Blocks.AIR.defaultBlockState();
        if (block.mcc$modern instanceof LegacyBlockAdapter a) return a.stateForMeta(meta);
        if (block.mcc$vanilla != null) return stateOfVanilla(block.mcc$vanilla, meta);
        LOG.warn("Legacy block {} used in the world before it was registered; placing air", block);
        return Blocks.AIR.defaultBlockState();
    }

    public static BlockState withMeta(BlockState current, int meta) {
        if (current.getBlock() instanceof LegacyBlockAdapter a) return a.stateForMeta(meta);
        return toModern(legacyBlock(current), meta);
    }

    public static net.minecraft.world.level.block.Block modernBlock(Block block) {
        return toModern(block, 0).getBlock();
    }
}
