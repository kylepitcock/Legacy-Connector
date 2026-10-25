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

    private Registries1122() {}

    /** The item registered for a block (its ItemBlock), or null. */
    public static Item itemOf(Block block) {
        return ItemBridge1122.itemOf(block);
    }

    public static final class Reg<V extends IForgeRegistryEntry<V>> implements IForgeRegistry<V> {
        private final Class<V> type;
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

        @Override
        public Class<V> getRegistrySuperType() {
            return type;
        }

        @Override
        public synchronized void register(V value) {
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
            return byName.containsKey(key);
        }

        @Override
        public synchronized boolean containsValue(V value) {
            return byValue.containsKey(value);
        }

        @Override
        public synchronized V getValue(ResourceLocation key) {
            return byName.get(key);
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
        public synchronized Set<Map.Entry<ResourceLocation, V>> getEntries() {
            return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(byName.entrySet()));
        }
    }
}
