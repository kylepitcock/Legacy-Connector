package mccompat.v1_7_10.cpw.mods.fml.common.registry;

import mccompat.v1_7_10.net.minecraft.util.RegistryNamespaced;

/** Forge's stable handle to a registered block/item ({@code block.delegate}, {@code item.delegate}). */
public interface RegistryDelegate<T> {
    T get();

    String name();

    Class<T> type();

    final class Delegate<T> implements RegistryDelegate<T> {
        private final T referant;
        private final Class<T> type;
        private final RegistryNamespaced registry;

        public Delegate(T referant, Class<T> type, RegistryNamespaced registry) {
            this.referant = referant;
            this.type = type;
            this.registry = registry;
        }

        @Override
        public T get() {
            return referant;
        }

        @Override
        public String name() {
            return registry == null ? null : registry.getNameForObject((Object) referant);
        }

        @Override
        public Class<T> type() {
            return type;
        }
    }
}
