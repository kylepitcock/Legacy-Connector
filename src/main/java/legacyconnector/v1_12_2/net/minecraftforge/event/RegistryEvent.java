package legacyconnector.v1_12_2.net.minecraftforge.event;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.GenericEvent;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

public class RegistryEvent<T extends IForgeRegistryEntry<T>> extends GenericEvent<T> {
    protected RegistryEvent(Class<T> type) {
        super(type);
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
