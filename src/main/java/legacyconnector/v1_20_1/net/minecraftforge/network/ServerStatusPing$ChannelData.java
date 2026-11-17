package legacyconnector.v1_20_1.net.minecraftforge.network;

import net.minecraft.resources.ResourceLocation;

/** Forge 1.20.1's {@code ServerStatusPing.ChannelData}: a channel's name, protocol version and whether the other side must have it. */
public final class ServerStatusPing$ChannelData {
    private final ResourceLocation channel;
    private final String version;
    private final boolean required;

    public ServerStatusPing$ChannelData(ResourceLocation channel, String version, boolean required) {
        this.channel = channel;
        this.version = version;
        this.required = required;
    }

    public ResourceLocation channel() {
        return channel;
    }

    public String version() {
        return version;
    }

    public boolean required() {
        return required;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ServerStatusPing$ChannelData d && channel.equals(d.channel) && version.equals(d.version) && required == d.required;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(channel, version, required);
    }

    @Override
    public String toString() {
        return "ChannelData[channel=" + channel + ", version=" + version + ", required=" + required + "]";
    }
}
