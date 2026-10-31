package org.kylepitcock.legacyconnector.api.internal;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.kylepitcock.legacyconnector.api.LegacyMod;
import org.kylepitcock.legacyconnector.api.LegacyVersion;
import org.kylepitcock.legacyconnector.core.LegacyLifecycle;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;

/** {@link LegacyMod} views of the runtime's {@link LegacyModRecord}s. */
public final class ModViews {
    private ModViews() {}

    public static List<LegacyMod> all() {
        return LegacyMods.ordered().stream().<LegacyMod>map(View::new).toList();
    }

    public static Optional<LegacyMod> find(String id) {
        LegacyModRecord record = LegacyMods.find(id);
        return record == null ? Optional.empty() : Optional.of(new View(record));
    }

    private record View(LegacyModRecord mod) implements LegacyMod {
        @Override
        public String id() {
            return mod.modernId;
        }

        @Override
        public String legacyId() {
            return mod.legacyId;
        }

        @Override
        public String name() {
            return mod.name();
        }

        @Override
        public String version() {
            return mod.modVersion();
        }

        @Override
        public LegacyVersion minecraftVersion() {
            return LegacyVersion.byId(mod.version).orElseThrow(() -> new IllegalStateException("Unknown legacy version " + mod.version));
        }

        @Override
        public boolean failed() {
            return LegacyLifecycle.failures().containsKey(mod);
        }

        @Override
        public Optional<String> failureMessage() {
            Throwable t = LegacyLifecycle.failures().get(mod);
            return t == null ? Optional.empty() : Optional.ofNullable(t.getMessage());
        }

        @Override
        public ClassLoader classLoader() {
            return mod.mainClass != null ? mod.mainClass.getClassLoader() : ModViews.class.getClassLoader();
        }

        @Override
        public Optional<Class<?>> classOf(String className) {
            try {
                return Optional.of(Class.forName(className, false, classLoader()));
            } catch (ClassNotFoundException | LinkageError e) {
                return Optional.empty();
            }
        }

        @Override
        public Optional<Object> instance() {
            return Optional.ofNullable(mod.instance);
        }

        @Override
        public Path file() {
            return mod.originalFile();
        }
    }
}
