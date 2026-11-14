package legacyconnector.v1_20_1.net.minecraftforge.network;

import java.util.function.Predicate;
import java.util.function.Supplier;
import legacyconnector.v1_20_1.net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;

/** Forge 1.20.1's channel factory. Version negotiation is not enforced: NeoForge's payload registration is marked optional. */
public class NetworkRegistry {
    public static final String ABSENT = "ABSENT 🤔";
    public static final String ACCEPTVANILLA = "ALLOWVANILLA 👌";

    public static SimpleChannel newSimpleChannel(ResourceLocation name, Supplier<String> networkProtocolVersion, Predicate<String> clientAcceptedVersions,
            Predicate<String> serverAcceptedVersions) {
        return new SimpleChannel(name, networkProtocolVersion, clientAcceptedVersions, serverAcceptedVersions);
    }

    public static legacyconnector.v1_20_1.net.minecraftforge.network.event.EventNetworkChannel newEventChannel(ResourceLocation name, Supplier<String> version,
            Predicate<String> clientAcceptedVersions, Predicate<String> serverAcceptedVersions) {
        return new legacyconnector.v1_20_1.net.minecraftforge.network.event.EventNetworkChannel(name, version);
    }

    public static Predicate<String> acceptMissingOr(String version) {
        return v -> true;
    }

    public static Predicate<String> acceptMissingOr(Predicate<String> versionCheck) {
        return v -> true;
    }

    /** {@code ChannelBuilder.named(id).networkProtocolVersion(() -> V).clientAcceptedVersions(V::equals).serverAcceptedVersions(V::equals).simpleChannel()}. */
    public static class ChannelBuilder {
        private final ResourceLocation name;
        private Supplier<String> version = () -> "1";

        private ChannelBuilder(ResourceLocation name) {
            this.name = name;
        }

        public static ChannelBuilder named(ResourceLocation name) {
            return new ChannelBuilder(name);
        }

        public ChannelBuilder networkProtocolVersion(Supplier<String> version) {
            this.version = version;
            return this;
        }

        public ChannelBuilder clientAcceptedVersions(Predicate<String> accepted) {
            return this;
        }

        public ChannelBuilder serverAcceptedVersions(Predicate<String> accepted) {
            return this;
        }

        public SimpleChannel simpleChannel() {
            return new SimpleChannel(name, version, v -> true, v -> true);
        }

        /** {@code ...eventNetworkChannel()}: the raw channel (CoFH Core's packet handler), see {@link legacyconnector.v1_20_1.net.minecraftforge.network.event.EventNetworkChannel}. */
        public legacyconnector.v1_20_1.net.minecraftforge.network.event.EventNetworkChannel eventNetworkChannel() {
            return new legacyconnector.v1_20_1.net.minecraftforge.network.event.EventNetworkChannel(name, version);
        }
    }
}
