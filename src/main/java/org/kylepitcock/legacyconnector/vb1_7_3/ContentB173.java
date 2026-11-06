package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;

/**
 * What the b1.7.3 mods registered while they ran: blocks and items (by the mod that constructed them), names, texture overrides, tile
 * entities, entities and tick hooks. ModLoader mods register from their constructors, so the mod being constructed or notified is the
 * owner of everything created meanwhile ({@link #enter}).
 */
public final class ContentB173 {
    /** A texture override: {@code ModLoader.addOverride(sheet, path, index)}. */
    public record TextureOverride(String sheet, String path, int index, LegacyModRecord mod) {}

    /** {@code ModLoader.RegisterTileEntity} / {@code RegisterEntityID}. */
    public record Registration(Class<?> type, String name, int id, LegacyModRecord mod) {}

    private static LegacyModRecord current;
    private static final List<Block> BLOCKS = new ArrayList<>();
    private static final List<Item> ITEMS = new ArrayList<>();
    private static final Map<Object, String> NAMES = new IdentityHashMap<>();
    private static final Map<String, String> LOCALIZATION = new LinkedHashMap<>();
    private static final List<TextureOverride> OVERRIDES = new ArrayList<>();
    private static final List<Registration> TILE_ENTITIES = new ArrayList<>();
    private static final List<Registration> ENTITIES = new ArrayList<>();
    private static final List<BaseMod> MODS = new ArrayList<>();
    private static final Map<BaseMod, boolean[]> TICK_HOOKS = new IdentityHashMap<>();
    private static final List<String> ARMOR = new ArrayList<>(List.of("cloth", "chain", "iron", "diamond", "gold"));

    private ContentB173() {}

    /** Everything created until {@link #exit()} belongs to {@code mod}. */
    public static synchronized void enter(LegacyModRecord mod) {
        current = mod;
    }

    public static synchronized void exit() {
        current = null;
    }

    public static synchronized LegacyModRecord current() {
        return current;
    }

    public static synchronized void addMod(BaseMod mod) {
        MODS.add(mod);
    }

    public static synchronized List<BaseMod> loadedMods() {
        return Collections.unmodifiableList(new ArrayList<>(MODS));
    }

    /** ModLoader compared class names; the mods' classes moved to a package, so the simple name counts too. */
    public static synchronized boolean isModLoaded(String name) {
        for (BaseMod mod : MODS) {
            if (mod.getClass().getName().equals(name) || mod.getClass().getSimpleName().equals(name)) return true;
        }
        return false;
    }

    public static synchronized Object claimBlock(Block block) {
        BLOCKS.add(block);
        return owner(block.getClass());
    }

    public static synchronized Object claimItem(Item item) {
        ITEMS.add(item);
        return owner(item.getClass());
    }

    private static final Map<Module, LegacyModRecord> BY_MODULE = new IdentityHashMap<>();

    /**
     * The mod whose jar defines the class (RedPower Array's ModsLoaded initialises RedPower Logic and Wiring, whose blocks are theirs);
     * for classes of the shim or the synthesizer ({@code new Block(...)} itself) the mod running.
     */
    private static LegacyModRecord owner(Class<?> type) {
        String n = type.getName();
        if (n.startsWith("legacyconnector.") || n.startsWith("legacyconnector_synth.")) return current;
        Module module = type.getModule();
        if (!module.isNamed()) return current;
        LegacyModRecord hit = BY_MODULE.get(module);
        if (hit != null) return hit;
        for (LegacyModRecord m : org.kylepitcock.legacyconnector.core.LegacyMods.all()) {
            if (!PlatformB173.VERSION.equals(m.version)) continue;
            try {
                Class<?> main = m.mainClass != null ? m.mainClass : Class.forName(m.mainClassName, false, type.getClassLoader());
                BY_MODULE.putIfAbsent(main.getModule(), m);
                if (main.getModule() == module) return m;
            } catch (ClassNotFoundException | LinkageError e) {
                // a mod that cannot load owns nothing
            }
        }
        return current;
    }

    private static final Map<Integer, Object> RESERVED = new java.util.HashMap<>();

    /** Block id {@code id} is a mod's {@code @MLProp} value: Forge configs of the other mods do not hand it out ({@link MLPropsB173}). */
    public static synchronized void reserve(int id, Object owner) {
        RESERVED.putIfAbsent(id, owner);
    }

    /** True if another mod than the one running reserved block id {@code id}. */
    public static synchronized boolean reservedByOther(int id) {
        Object owner = RESERVED.get(id);
        return owner != null && owner != current;
    }

    /** Marks where a mod's construction attempt starts, for {@link #release}. */
    public static synchronized int[] mark() {
        return new int[] {BLOCKS.size(), ITEMS.size()};
    }

    /**
     * Frees the block and item ids taken since {@code mark} (a failed construction attempt): other mods, or the next attempt, may use
     * them. Returns how many were freed.
     */
    public static synchronized int release(int[] mark) {
        int freed = 0;
        for (int i = BLOCKS.size() - 1; i >= mark[0]; i--) {
            Block b = BLOCKS.remove(i);
            if (b.id >= 0 && b.id < Block.BLOCKS.length && Block.BLOCKS[b.id] == b) {
                Block.BLOCKS[b.id] = null;
                freed++;
            }
        }
        for (int i = ITEMS.size() - 1; i >= mark[1]; i--) {
            Item it = ITEMS.remove(i);
            if (it.id >= 0 && it.id < Item.ITEMS.length && Item.ITEMS[it.id] == it) {
                Item.ITEMS[it.id] = null;
                freed++;
            }
        }
        return freed;
    }

    /** {@code ModLoader.RegisterBlock}: nothing to do beyond the item ModLoader creates; the bridge takes every constructed block. */
    public static void registeredBlock(Block block) {}

    public static synchronized List<Block> blocks() {
        return new ArrayList<>(BLOCKS);
    }

    public static synchronized List<Item> items() {
        return new ArrayList<>(ITEMS);
    }

    public static synchronized void addName(Object object, String name) {
        if (object == null) return;
        NAMES.put(object, name);
        String key = null;
        if (object instanceof Block b) key = b.getTranslationKey();
        else if (object instanceof Item i) key = i.getTranslationKey();
        else if (object instanceof legacyconnector.vb1_7_3.net.minecraft.item.ItemStack s) key = s.getTranslationKey();
        if (key != null) LOCALIZATION.put(key + ".name", name);
    }

    public static synchronized String name(Object object) {
        return NAMES.get(object);
    }

    public static synchronized void addLocalization(String key, String value) {
        LOCALIZATION.put(key, value);
    }

    public static synchronized String translate(String key) {
        return LOCALIZATION.getOrDefault(key, key);
    }

    public static synchronized Map<String, String> localization() {
        return new LinkedHashMap<>(LOCALIZATION);
    }

    public static synchronized void addTextureOverride(String sheet, String path, int index) {
        OVERRIDES.add(new TextureOverride(sheet, path, index, current));
    }

    public static synchronized List<TextureOverride> textureOverrides() {
        return new ArrayList<>(OVERRIDES);
    }

    public static synchronized void registerTileEntity(Class<?> type, String name) {
        TILE_ENTITIES.add(new Registration(type, name, -1, current));
    }

    public static synchronized void registerEntity(Class<?> type, String name, int id) {
        ENTITIES.add(new Registration(type, name, id, current));
    }

    public static synchronized List<Registration> tileEntities() {
        return new ArrayList<>(TILE_ENTITIES);
    }

    public static synchronized List<Registration> entities() {
        return new ArrayList<>(ENTITIES);
    }

    public static synchronized void setTickHook(BaseMod mod, boolean enable, boolean useClock, boolean gui) {
        boolean[] hooks = TICK_HOOKS.computeIfAbsent(mod, k -> new boolean[2]);
        hooks[gui ? 1 : 0] = enable;
    }

    /** ModLoader asked every mod for the burn time of a fuel id; the hook is per mod ({@code BaseMod.AddFuel}), asked by the bridge. */
    public static synchronized int fuel(int id) {
        for (BaseMod mod : MODS) {
            int t = mod.AddFuel(id);
            if (t != 0) return t;
        }
        return 0;
    }

    /** {@code ModLoader.AddArmor}: the armour texture prefix index (cloth, chain, iron, diamond, gold, then the mods'). */
    public static synchronized int armorTexture(String name) {
        int i = ARMOR.indexOf(name);
        if (i >= 0) return i;
        ARMOR.add(name);
        return ARMOR.size() - 1;
    }

    /** A declared field of the class by name, as ModLoader looked it up (obfuscated vanilla names are not mapped yet). */
    public static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        return owner.getDeclaredField(name);
    }
}
