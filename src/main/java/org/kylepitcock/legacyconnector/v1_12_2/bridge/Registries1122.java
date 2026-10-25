package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The 1.12.2 Forge registries mods register into from {@code RegistryEvent.Register}: each keeps the legacy objects by name
 * and hands a new entry to a bridge that creates and registers the modern block or item.
 */
public final class Registries1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Registry");

    public static final Reg<Block> BLOCKS = new Reg<>(Block.class, BlockBridge1122::register);
    public static final Reg<Item> ITEMS = new Reg<>(Item.class, ItemBridge1122::register);
    public static final Reg<legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe> RECIPES =
            new Reg<>(legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe.class, RecipeBridge1122::add);
    /** Registries whose entry classes only the synthesizer provides (biomes, entities, villager professions, potion types): typed when their event is posted. */
    public static final Reg<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome> BIOMES =
            new Reg<>(legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.class, b -> {});
    public static final Reg<legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.EntityEntry> ENTITIES =
            new Reg<>(legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.EntityEntry.class, e -> {});
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final Reg VILLAGER_PROFESSIONS = new Reg(null, o -> {});
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final Reg POTION_TYPES = new Reg(null, o -> {});
    public static final Reg<legacyconnector.v1_12_2.net.minecraft.potion.Potion> POTIONS = new Reg<>(legacyconnector.v1_12_2.net.minecraft.potion.Potion.class, p -> {});
    public static final Reg<legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment> ENCHANTMENTS =
            new Reg<>(legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment.class, e -> {});
    public static final Reg<legacyconnector.v1_12_2.net.minecraft.util.SoundEvent> SOUND_EVENTS =
            new Reg<>(legacyconnector.v1_12_2.net.minecraft.util.SoundEvent.class, s -> {});

    private static final java.util.List<Reg<?>> CUSTOM = new java.util.ArrayList<>();

    /** A registry a mod made with RegistryBuilder. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static synchronized <V extends IForgeRegistryEntry<V>> Reg<V> custom(ResourceLocation name, Class<V> type) {
        Reg<V> reg = new Reg<>(type, v -> {});
        CUSTOM.add(reg);
        return reg;
    }

    public static synchronized java.util.List<Reg<?>> customRegistries() {
        return new java.util.ArrayList<>(CUSTOM);
    }

    /** A legacy-style numbered view of a Forge registry (Item.REGISTRY, Block.REGISTRY, ...), live over the registered objects. */
    public static <V extends legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry<V>>
            legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespacedDefaultedByKey<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, V> view(Reg<V> reg, String defaultKey) {
        return new legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespacedDefaultedByKey<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, V>(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(defaultKey)) {
            @Override
            public synchronized V getObject(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation name) {
                V v = reg.getValue(name);
                return v != null ? v : super.getObject(name);
            }

            @Override
            public synchronized legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getNameForObject(V value) {
                return reg.getKey(value);
            }

            @Override
            public synchronized java.util.Set<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation> getKeys() {
                return reg.getKeys();
            }

            @Override
            public synchronized int getIDForObject(V value) {
                return new java.util.ArrayList<>(reg.getValues()).indexOf(value);
            }

            @Override
            public synchronized V getObjectById(int id) {
                var all = new java.util.ArrayList<>(reg.getValues());
                return id >= 0 && id < all.size() ? all.get(id) : null;
            }

            @Override
            public boolean containsKey(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation key) {
                return reg.containsKey(key);
            }

            @Override
            public java.util.Iterator<V> iterator() {
                return reg.getValues().iterator();
            }
        };
    }

    private Registries1122() {}

    /** The item registered for a block (its ItemBlock), or null. */
    public static Item itemOf(Block block) {
        return ItemBridge1122.itemOf(block);
    }

    public static final class Reg<V extends IForgeRegistryEntry<V>> extends legacyconnector.v1_12_2.net.minecraftforge.registries.ForgeRegistry<V> {
        private Class<V> type;
        private final Map<ResourceLocation, V> byName = new LinkedHashMap<>();
        private final Map<V, ResourceLocation> byValue = new IdentityHashMap<>();
        private final Consumer<V> onRegister;

        Reg(Class<V> type, Consumer<V> onRegister) {
            this.type = type;
            this.onRegister = onRegister;
        }

        /** Adds a vanilla object so lookups by name find it; it is not registered with the modern registries. */
        public synchronized void registerVanilla(V value) {
            ResourceLocation name = value.getRegistryName();
            byName.putIfAbsent(name, value);
            byValue.putIfAbsent(value, name);
        }

        /** Types a registry whose entry class is synthesized: the class is looked up by its legacy name once. */
        @SuppressWarnings("unchecked")
        public void mcc$type(String legacyClass) {
            if (type != null) return;
            try {
                type = (Class<V>) Class.forName("legacyconnector_synth.v1_12_2." + legacyClass, false, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException | LinkageError e) {
                try {
                    type = (Class<V>) Class.forName("legacyconnector.v1_12_2." + legacyClass, false, Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException | LinkageError e2) {
                    // stays untyped: its event is not posted
                }
            }
        }

        @Override
        public synchronized int getID(V value) {
            return new java.util.ArrayList<>(byName.values()).indexOf(value);
        }

        @Override
        public synchronized int getID(ResourceLocation name) {
            int i = 0;
            for (ResourceLocation k : byName.keySet()) {
                if (k.equals(name)) return i;
                i++;
            }
            return -1;
        }

        @Override
        public synchronized V getValue(int id) {
            var all = new java.util.ArrayList<>(byName.values());
            return id >= 0 && id < all.size() ? all.get(id) : null;
        }

        @Override
        public boolean isLocked() {
            return false;
        }

        @Override
        public void unfreeze() {
        }

        @Override
        public synchronized V remove(ResourceLocation key) {
            V v = byName.remove(key);
            if (v != null) byValue.remove(v);
            return v;
        }

        @Override
        public Class<V> getRegistrySuperType() {
            return type;
        }

        @Override
        public synchronized void register(V value) {
            if (value == null) return; // a synthesized builder that produced nothing
            ResourceLocation name = value.getRegistryName();
            if (name == null) {
                LOG.warn("Ignoring an unnamed {} registration: {}", type.getSimpleName(), value);
                return;
            }
            if (byName.containsKey(name)) {
                LOG.warn("{} {} registered twice; keeping the first", type.getSimpleName(), name);
                return;
            }
            byName.put(name, value);
            byValue.put(value, name);
            onRegister.accept(value);
        }

        @SafeVarargs
        @Override
        public final void registerAll(V... values) {
            for (V v : values) register(v);
        }

        @Override
        public synchronized boolean containsKey(ResourceLocation key) {
            return getValue(key) != null;
        }

        @Override
        public synchronized boolean containsValue(V value) {
            return byValue.containsKey(value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public synchronized V getValue(ResourceLocation key) {
            Vanilla1122.ensureLoaded();
            V v = byName.get(key);
            if (v == null && key != null && "minecraft".equals(key.getNamespace()) && (type == Block.class || type == Item.class)) {
                // a vanilla name nobody asked for through Blocks/Items yet
                Object made = type == Block.class ? Vanilla1122.blockByName(key.getPath()) : Vanilla1122.itemByName(key.getPath());
                v = (V) made;
            }
            return v;
        }

        @Override
        public synchronized ResourceLocation getKey(V value) {
            return byValue.get(value);
        }

        @Override
        public synchronized Set<ResourceLocation> getKeys() {
            return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(byName.keySet()));
        }

        @Override
        public synchronized Collection<V> getValues() {
            return Collections.unmodifiableList(new ArrayList<>(byName.values()));
        }

        @Override
        public java.util.Iterator<V> iterator() {
            return getValues().iterator();
        }

        @Override
        public synchronized Set<Map.Entry<ResourceLocation, V>> getEntries() {
            return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(byName.entrySet()));
        }
    }
}
