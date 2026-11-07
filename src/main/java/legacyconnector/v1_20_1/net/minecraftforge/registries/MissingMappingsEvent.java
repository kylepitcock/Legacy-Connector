package legacyconnector.v1_20_1.net.minecraftforge.registries;

import java.util.List;
import net.neoforged.bus.api.Event;

/** Forge 1.20.1's "registry entries from an old save are gone" event. NeoForge 1.21.1 has no such event; mods that listen to it (to remap ids) link and are never called. */
public class MissingMappingsEvent extends Event {
    public <T> List<Mapping<T>> getMappings(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registry, String modid) {
        return List.of();
    }

    public <T> List<Mapping<T>> getAllMappings(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registry) {
        return List.of();
    }

    public static class Mapping<T> {
        public net.minecraft.resources.ResourceLocation getKey() {
            return null;
        }

        public void remap(T target) {
        }

        public void ignore() {
        }

        public void warn() {
        }

        public void fail() {
        }
    }
}
