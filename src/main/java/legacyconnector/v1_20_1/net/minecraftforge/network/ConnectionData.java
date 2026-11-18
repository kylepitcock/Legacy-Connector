package legacyconnector.v1_20_1.net.minecraftforge.network;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Forge 1.20.1's {@code ConnectionData}: what the other end of a connection announced during the login handshake (its mods with versions and its
 * channels). NeoForge 1.21.1 negotiates payload channels in the configuration phase and does not keep a mod list per connection, so
 * {@link NetworkHooks#getConnectionData} answers with this game's own mods and channels (APPROXIMATION: the remote side is assumed to have the same mods;
 * a mod only reads the list to show or log it).
 */
public class ConnectionData {
    private final ImmutableMap<String, Map.Entry<String, String>> modData;
    private final ImmutableMap<ResourceLocation, String> channels;

    public ConnectionData(Map<String, Map.Entry<String, String>> modData, Map<ResourceLocation, String> channels) {
        this.modData = ImmutableMap.copyOf(modData);
        this.channels = ImmutableMap.copyOf(channels);
    }

    public ImmutableList<String> getModList() {
        return modData.keySet().asList();
    }

    /** Mod id to (display name, version). */
    public ImmutableMap<String, Map.Entry<String, String>> getModData() {
        return modData;
    }

    public ImmutableMap<ResourceLocation, String> getChannels() {
        return channels;
    }
}
