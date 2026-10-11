package mccompat.v1_7_10.mcp.mobius.waila.api;

import java.util.HashMap;
import java.util.Set;

public interface IWailaConfigHandler {
    Set<String> getModuleNames();

    HashMap<String, String> getConfigKeys(String module);

    boolean getConfig(String key, boolean defaultValue);

    boolean getConfig(String key);
}
