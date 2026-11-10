package org.kylepitcock.legacyconnector.vb1_7_3;

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
    private static final Map<BaseMod, boolean[]> TICK_HOOKS = new LinkedHashMap<>();
    private static final Map<BaseMod,long[]> TICK_REVISIONS=new IdentityHashMap<>();
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
        // A jar may contain several entrypoints sharing one module. FML discovery order differs by side;
        // use the same dependency/id order as platform initialization when selecting that module's owner.
        for (LegacyModRecord m : org.kylepitcock.legacyconnector.core.LegacyMods.ordered()) {
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
        try {
            legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity.create(type, name);
        } catch (IllegalArgumentException e) {
            // registered twice (a mod and its addon): the first id stays, as ModLoader's own exception was caught by the mod
        }
    }

    private static final Map<Class<?>, Object> TILE_RENDERERS = new LinkedHashMap<>();

    public static synchronized void registerTileEntityRenderer(Class<?> type, Object renderer) {
        if (renderer != null) TILE_RENDERERS.put(type, renderer);
    }

    public static synchronized Map<Class<?>, Object> tileEntityRenderers() {
        return new LinkedHashMap<>(TILE_RENDERERS);
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
        boolean[] hooks = TICK_HOOKS.computeIfAbsent(mod, k -> new boolean[4]);
        hooks[gui ? 1 : 0] = enable;
        hooks[gui ? 3 : 2] = useClock;
        TICK_REVISIONS.computeIfAbsent(mod,key->new long[2])[gui?1:0]++;
    }
    public static synchronized boolean hasClientHooks(){
        for(boolean[] hooks:TICK_HOOKS.values())if(hooks[0]||hooks[1])return true;
        return false;
    }

    /** ModLoader hooks returning false unregister themselves. The clock flag limits them to world ticks. */
    public static synchronized void clientHooks(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft minecraft,
            boolean gui,boolean clockAdvanced){
        int index=gui?1:0;
        for(var entry:new ArrayList<>(TICK_HOOKS.entrySet())){
            boolean[] flags=entry.getValue();if(!flags[index]||flags[index+2]&&!clockAdvanced)continue;
            try{
                boolean retain=gui?entry.getKey().OnTickInGUI(minecraft,minecraft.currentScreen):entry.getKey().OnTickInGame(minecraft);
                if(!retain)flags[index]=false;
            }catch(RuntimeException|LinkageError error){
                flags[index]=false;
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Beta ModLoader client hook failed for {}",entry.getKey().getClass().getName(),error);
            }
        }
    }

    /** Dedicated hook retention belongs to its actor; returning false or failing cannot unregister another player. */
    public static final class ActorHooks {
        private static final class State {final long[] revision={Long.MIN_VALUE,Long.MIN_VALUE};final boolean[] stopped=new boolean[2];}
        private final Map<BaseMod,State> states=new IdentityHashMap<>();
        void invoke(BaseMod mod,boolean gui,long revision,boolean enabled,boolean clock,boolean advanced,java.util.function.BooleanSupplier callback){
            int index=gui?1:0;var state=states.computeIfAbsent(mod,key->new State());
            if(state.revision[index]!=revision){state.revision[index]=revision;state.stopped[index]=false;}
            if(!enabled||state.stopped[index]||clock&&!advanced)return;
            try{if(!callback.getAsBoolean())state.stopped[index]=true;}
            catch(RuntimeException|LinkageError error){state.stopped[index]=true;org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Beta actor hook failed for {}",mod.getClass().getName(),error);}
        }
    }
    public static synchronized void serverHooks(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft minecraft,
            boolean gui,boolean clockAdvanced,ActorHooks actor){
        int index=gui?1:0;
        for(var entry:new ArrayList<>(TICK_HOOKS.entrySet())){
            var mod=entry.getKey();boolean[] flags=entry.getValue().clone();long[] revisions=TICK_REVISIONS.get(mod);
            actor.invoke(mod,gui,revisions==null?0:revisions[index],flags[index],flags[index+2],clockAdvanced,
                    ()->gui?mod.OnTickInGUI(minecraft,minecraft.currentScreen):mod.OnTickInGame(minecraft));
        }
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
    public static synchronized String armorTexture(int index){return ARMOR.get(index);}

}
