package legacyconnector.v1_12_2.net.minecraftforge.event;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;
import org.kylepitcock.legacyconnector.core.event.GenericEventType;

public class RegistryEvent<T extends IForgeRegistryEntry<T>> extends Event implements GenericEventType {
    private final Class<T> type;

    protected RegistryEvent(Class<T> type) {
        this.type = type;
    }

    public Class<T> getGenericType() {
        return type;
    }

    @Override
    public Class<?> mcc$genericType() {
        return type;
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
