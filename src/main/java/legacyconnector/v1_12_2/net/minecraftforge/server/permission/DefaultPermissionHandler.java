package legacyconnector.v1_12_2.net.minecraftforge.server.permission;

import com.mojang.authlib.GameProfile;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraftforge.server.permission.context.IContext;

/**
 * Forge's default handler: a node registered as ALL is granted to everyone, OP to operators of the running server (the modern op
 * list), NONE and unknown nodes to nobody.
 */
public enum DefaultPermissionHandler implements IPermissionHandler {
    INSTANCE;

    private final Map<String, DefaultPermissionLevel> permissionLevelMap = new ConcurrentHashMap<>();
    private final Map<String, String> descriptionMap = new ConcurrentHashMap<>();

    @Override
    public void registerNode(String node, DefaultPermissionLevel level, String desc) {
        permissionLevelMap.put(node, level == null ? DefaultPermissionLevel.NONE : level);
        if (desc != null && !desc.isEmpty()) descriptionMap.put(node, desc);
    }

    @Override
    public Collection<String> getRegisteredNodes() {
        return Collections.unmodifiableSet(permissionLevelMap.keySet());
    }

    @Override
    public boolean hasPermission(GameProfile profile, String node, IContext context) {
        DefaultPermissionLevel level = getDefaultPermissionLevel(node);
        if (level == DefaultPermissionLevel.NONE) return false;
        if (level == DefaultPermissionLevel.ALL) return true;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server != null && profile != null && server.getPlayerList().isOp(profile);
    }

    @Override
    public String getNodeDescription(String node) {
        return descriptionMap.getOrDefault(node, "");
    }

    public DefaultPermissionLevel getDefaultPermissionLevel(String node) {
        return node == null ? DefaultPermissionLevel.NONE : permissionLevelMap.getOrDefault(node, DefaultPermissionLevel.NONE);
    }
}
