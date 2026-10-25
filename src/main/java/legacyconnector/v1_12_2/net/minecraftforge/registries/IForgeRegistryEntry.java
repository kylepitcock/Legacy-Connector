package legacyconnector.v1_12_2.net.minecraftforge.registries;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** Anything registered through a Forge registry (blocks, items, ...): it carries its registry name. */
public interface IForgeRegistryEntry<V> {
    V setRegistryName(ResourceLocation name);

    ResourceLocation getRegistryName();

    Class<V> getRegistryType();

    class Impl<V extends IForgeRegistryEntry<V>> implements IForgeRegistryEntry<V> {
        private ResourceLocation registryName;
        /** Forge's handle to the entry that stays valid when the registry is rebuilt. */
        public IRegistryDelegate<V> delegate = newDelegate();

        /** For objects made without running constructors (placeholders): gives them their delegate. */
        public final void mcc$initDelegate() {
            if (delegate == null) delegate = newDelegate();
        }

        private IRegistryDelegate<V> newDelegate() {
            return new IRegistryDelegate<V>() {
            @SuppressWarnings("unchecked")
            @Override
            public V get() {
                return (V) Impl.this;
            }

            @Override
            public ResourceLocation name() {
                return registryName;
            }

            @Override
            public Class<V> type() {
                return getRegistryType();
            }
            };
        }

        @SuppressWarnings("unchecked")
        @Override
        public final V setRegistryName(ResourceLocation name) {
            if (this.registryName != null) {
                throw new IllegalStateException("Attempted to set registry name with existing registry name! New: " + name + " Old: " + this.registryName);
            }
            this.registryName = name;
            return (V) this;
        }

        public final V setRegistryName(String name) {
            return setRegistryName(name.indexOf(':') >= 0 ? new ResourceLocation(name) : new ResourceLocation(activeModId(), name));
        }

        public final V setRegistryName(String modId, String name) {
            return setRegistryName(new ResourceLocation(modId, name));
        }

        @Override
        public final ResourceLocation getRegistryName() {
            return registryName;
        }

        @SuppressWarnings("unchecked")
        @Override
        public final Class<V> getRegistryType() {
            Class<?> c = getClass();
            while (c != null) {
                // the registry type is the class that extends Impl (Block, Item, ...), not Impl itself
                if (c.getSuperclass() == Impl.class) return (Class<V>) c;
                for (Class<?> i : c.getInterfaces()) if (i == IForgeRegistryEntry.class && c != Impl.class) return (Class<V>) c;
                c = c.getSuperclass();
            }
            return (Class<V>) getClass();
        }

        /** Forge named unprefixed registry names after the active mod container. */
        private static String activeModId() {
            var container = org.kylepitcock.legacyconnector.v1_12_2.LegacyModContainer1122.active();
            return container == null ? "minecraft" : container.getModId();
        }
    }
}
