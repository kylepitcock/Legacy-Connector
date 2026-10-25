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

    static {
        ENTITIES.vanilla = Registries1122::vanillaEntities;
    }

    /** 1.12 entity ids the 1.13+ renames changed (legacy -> modern). */
    private static final Map<String, String> ENTITY_RENAMES = Map.of("zombie_pigman", "zombified_piglin", "snowman", "snow_golem",
            "villager_golem", "iron_golem", "commandblock_minecart", "command_block_minecart", "xp_orb", "experience_orb",
            "xp_bottle", "experience_bottle", "ender_crystal", "end_crystal", "evocation_illager", "evoker",
            "vindication_illager", "vindicator", "illusion_illager", "illusioner");

    private static final java.util.Set<String> MISC_MOBS = java.util.Set.of("villager", "iron_golem", "snow_golem", "wandering_trader");

    /** The legacy base class standing in for a vanilla entity's class: mobs are EntityLiving (EnderIO lists mobs by it). */
    @SuppressWarnings("unchecked")
    private static Class<? extends legacyconnector.v1_12_2.net.minecraft.entity.Entity> entityBase(net.minecraft.world.entity.EntityType<?> type) {
        String path = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        boolean mob = type.getCategory() != net.minecraft.world.entity.MobCategory.MISC || MISC_MOBS.contains(path)
                || path.equals("ender_dragon") || path.equals("wither");
        if (mob) {
            for (String prefix : new String[] {"legacyconnector.v1_12_2.", "legacyconnector_synth.v1_12_2."}) {
                try {
                    return (Class<? extends legacyconnector.v1_12_2.net.minecraft.entity.Entity>) Class.forName(prefix + "net.minecraft.entity.EntityLiving",
                            false, Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException | LinkageError | ClassCastException e) {
                    // next
                }
            }
            return legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.class;
        }
        return legacyconnector.v1_12_2.net.minecraft.entity.Entity.class;
    }

    /** Vanilla entity types as legacy entries (Bountiful lists them by name), under their 1.12 ids. */
    private static void vanillaEntities(Reg<legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.EntityEntry> reg) {
        Map<String, String> legacyName = new java.util.HashMap<>();
        ENTITY_RENAMES.forEach((legacy, modern) -> legacyName.put(modern, legacy));
        for (var type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!"minecraft".equals(id.getNamespace())) continue;
            String path = legacyName.getOrDefault(id.getPath(), id.getPath());
            var entry = new legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.EntityEntry(entityBase(type), path);
            entry.setRegistryName(new ResourceLocation("minecraft", path));
            reg.registerVanilla(entry);
        }
    }
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
                if (v != null) return v;
                V fallback = super.getObject(name);
                return fallback != null ? fallback : reg.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(defaultKey));
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
                return reg.getID(value);
            }

            @Override
            public synchronized V getObjectById(int id) {
                return reg.getValue(id);
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
        /** Adds the vanilla entries on first read, for registries whose vanilla objects nobody names through a constant. */
        Consumer<Reg<V>> vanilla;

        Reg(Class<V> type, Consumer<V> onRegister) {
            this.type = type;
            this.onRegister = onRegister;
        }

        /** Adds a vanilla object so lookups by name find it; it is not registered with the modern registries. */
        private void ensureVanilla() {
            var fill = vanilla;
            if (fill == null) return;
            vanilla = null;
            fill.accept(this);
        }

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

        /** Numeric ids are registration order; the numbering is rebuilt after the registry grows, so lookups stay O(1). */
        private java.util.List<V> byId;
        private Map<V, Integer> idOf;

        private void numbering() {
            ensureVanilla();
            if (byId != null && byId.size() == byName.size()) return;
            byId = new ArrayList<>(byName.values());
            idOf = new IdentityHashMap<>();
            for (int i = 0; i < byId.size(); i++) idOf.putIfAbsent(byId.get(i), i);
        }

        @Override
        public synchronized int getID(V value) {
            numbering();
            Integer id = idOf.get(value);
            return id == null ? -1 : id;
        }

        @Override
        public synchronized int getID(ResourceLocation name) {
            ensureVanilla();
            V v = byName.get(name);
            return v == null ? -1 : getID(v);
        }

        @Override
        public synchronized V getValue(int id) {
            numbering();
            return id >= 0 && id < byId.size() ? byId.get(id) : null;
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
            byId = null;
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
            ensureVanilla();
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
            ensureVanilla();
            return byValue.get(value);
        }

        @Override
        public synchronized Set<ResourceLocation> getKeys() {
            ensureVanilla();
            return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(byName.keySet()));
        }

        @Override
        public synchronized Collection<V> getValues() {
            ensureVanilla();
            return Collections.unmodifiableList(new ArrayList<>(byName.values()));
        }

        @Override
        public java.util.Iterator<V> iterator() {
            return getValues().iterator();
        }

        @Override
        public synchronized Set<Map.Entry<ResourceLocation, V>> getEntries() {
            ensureVanilla();
            return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(byName.entrySet()));
        }
    }
}
