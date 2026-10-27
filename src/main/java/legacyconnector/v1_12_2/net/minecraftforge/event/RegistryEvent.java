package legacyconnector.v1_12_2.net.minecraftforge.event;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.GenericEvent;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

public class RegistryEvent<T extends IForgeRegistryEntry<T>> extends GenericEvent<T> {
    protected RegistryEvent(Class<T> type) {
        super(type);
    }

    /** Fired before the registries fill: mods create theirs in it. */
    public static class NewRegistry extends legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event {
    }

    /**
     * Ids a saved world knew that no mod registers any more. Modern worlds keep their own ids, so the layer never has missing
     * mappings to report; the event exists for the mods that listen for it.
     */
    public static class MissingMappings<T extends IForgeRegistryEntry<T>> extends RegistryEvent<T> {
        private final IForgeRegistry<T> registry;
        private final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation name;
        private final com.google.common.collect.ImmutableList<Mapping<T>> mappings;
        private legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer activeMod;

        public MissingMappings(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation name, IForgeRegistry<T> registry,
                java.util.Collection<Mapping<T>> missed) {
            super(registry.getRegistrySuperType());
            this.registry = registry;
            this.name = name;
            this.mappings = com.google.common.collect.ImmutableList.copyOf(missed);
        }

        public legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getName() {
            return name;
        }

        public IForgeRegistry<T> getRegistry() {
            return registry;
        }

        public void setModContainer(legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer mod) {
            this.activeMod = mod;
        }

        /** The mappings of the mod handling the event. */
        public com.google.common.collect.ImmutableList<Mapping<T>> getMappings() {
            String id = activeMod == null ? null : activeMod.getModId();
            if (id == null) return mappings;
            return com.google.common.collect.ImmutableList.copyOf(mappings.stream().filter(m -> id.equals(m.key.getNamespace())).toList());
        }

        public com.google.common.collect.ImmutableList<Mapping<T>> getAllMappings() {
            return mappings;
        }

        public enum Action {
            DEFAULT, IGNORE, WARN, FAIL, REMAP
        }

        public static class Mapping<T extends IForgeRegistryEntry<T>> {
            public final IForgeRegistry<T> registry;
            public final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation key;
            public final int id;
            private Action action = Action.DEFAULT;
            private T target;

            public Mapping(IForgeRegistry<T> registry, legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation key, int id) {
                this.registry = registry;
                this.key = key;
                this.id = id;
            }

            public void ignore() {
                action = Action.IGNORE;
            }

            public void warn() {
                action = Action.WARN;
            }

            public void fail() {
                action = Action.FAIL;
            }

            public void remap(T target) {
                this.action = Action.REMAP;
                this.target = target;
            }

            public Action getAction() {
                return action;
            }

            public T getTarget() {
                return target;
            }
        }
    }

    public static class Register<T extends IForgeRegistryEntry<T>> extends RegistryEvent<T> {
        private final IForgeRegistry<T> registry;

        public Register(IForgeRegistry<T> registry) {
            super(registry.getRegistrySuperType());
            this.registry = registry;
        }

        public IForgeRegistry<T> getRegistry() {
            return registry;
        }
    }
}
