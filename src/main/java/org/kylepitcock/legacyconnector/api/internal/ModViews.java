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
        List<LegacyMod> out = new java.util.ArrayList<>();
        LegacyMods.ordered().forEach(m -> out.add(new View(m)));
        for (String id : InPlaceMods.ids()) out.add(new InPlaceView(id));
        return out;
    }

    public static Optional<LegacyMod> find(String id) {
        LegacyModRecord record = LegacyMods.find(id);
        if (record != null) return Optional.of(new View(record));
        if (id != null && InPlaceMods.ids().contains(id)) return Optional.of(new InPlaceView(id));
        return Optional.empty();
    }

    /** A Forge 1.20.1 mod: a real NeoForge mod after the in-place remap, so its container, class loader and instance are the ordinary ones. */
    private record InPlaceView(String id) implements LegacyMod {
        private java.util.Optional<net.neoforged.fml.ModContainer> container() {
            return net.neoforged.fml.ModList.get().getModContainerById(id).map(c -> (net.neoforged.fml.ModContainer) c);
        }

        @Override
        public String legacyId() {
            return id;
        }

        @Override
        public String name() {
            return container().map(c -> c.getModInfo().getDisplayName()).orElse(id);
        }

        @Override
        public String version() {
            return container().map(c -> c.getModInfo().getVersion().toString()).orElse("");
        }

        @Override
        public LegacyVersion minecraftVersion() {
            return LegacyVersion.V1_20_1;
        }

        @Override
        public boolean failed() {
            return false;
        }

        @Override
        public Optional<String> failureMessage() {
            return Optional.empty();
        }

        @Override
        public ClassLoader classLoader() {
            // FML keeps the mod's entry classes (not the instance); their loader is the mod's
            return mainClass().map(Class::getClassLoader).orElse(ModViews.class.getClassLoader());
        }

        @SuppressWarnings("unchecked")
        private Optional<Class<?>> mainClass() {
            return container().filter(c -> c instanceof net.neoforged.fml.javafmlmod.FMLModContainer).flatMap(c -> {
                try {
                    var f = net.neoforged.fml.javafmlmod.FMLModContainer.class.getDeclaredField("modClasses");
                    f.setAccessible(true);
                    var classes = (java.util.List<Class<?>>) f.get(c);
                    return classes.isEmpty() ? Optional.<Class<?>>empty() : Optional.<Class<?>>of(classes.get(0));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    return Optional.<Class<?>>empty();
                }
            });
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
            return Optional.empty(); // FML does not keep the instance of a NeoForge mod; mainClass() is what the API can offer
        }

        @Override
        public Path file() {
            return container().map(c -> c.getModInfo().getOwningFile().getFile().getFilePath()).orElse(null);
        }
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
        public List<String> missingDependencies() {
            return LegacyLifecycle.missingDependencies(mod.legacyId);
        }

        @Override
        public Optional<String> failedPhase() {
            Throwable t = LegacyLifecycle.failures().get(mod);
            return t instanceof org.kylepitcock.legacyconnector.core.LegacyModException e ? Optional.ofNullable(e.phase()) : Optional.empty();
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
