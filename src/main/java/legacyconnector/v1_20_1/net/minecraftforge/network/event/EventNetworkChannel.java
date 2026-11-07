package legacyconnector.v1_20_1.net.minecraftforge.network.event;

import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;

/**
 * Forge 1.20.1's raw event channel ({@code NetworkRegistry.newEventChannel}): mods used it to listen to custom payloads by hand. Raw payloads have no
 * counterpart in NeoForge 1.21.1's typed payload system, so the channel exists (the mod links and constructs) and never delivers a packet.
 */
public class EventNetworkChannel {
    private final ResourceLocation name;

    public EventNetworkChannel(ResourceLocation name) {
        this.name = name;
    }

    public ResourceLocation getName() {
        return name;
    }

    public void registerObject(Object target) {
    }

    public <T> void addListener(Consumer<T> eventListener) {
    }

    public <T> void addGatherListener(Consumer<T> eventListener) {
    }
}
