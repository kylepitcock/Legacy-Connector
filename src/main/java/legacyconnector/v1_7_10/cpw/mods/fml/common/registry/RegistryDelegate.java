package legacyconnector.v1_7_10.cpw.mods.fml.common.registry;

import legacyconnector.v1_7_10.net.minecraft.util.RegistryNamespaced;

/** Forge's stable handle to a registered block/item ({@code block.delegate}, {@code item.delegate}). */
public interface RegistryDelegate<T> {
    T get();

    String name();

    Class<T> type();

    final class Delegate<T> implements RegistryDelegate<T> {
        // Not final, as in FML: mods replacing a vanilla entry repoint the delegate by reflection.
        private T referant;
        private String name;
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
            if (name != null) return name;
            return registry == null ? null : registry.getNameForObject((Object) referant);
        }

        @Override
        public Class<T> type() {
            return type;
        }
    }
}
