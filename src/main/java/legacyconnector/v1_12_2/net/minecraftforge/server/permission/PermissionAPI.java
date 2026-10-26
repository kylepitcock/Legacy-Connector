package legacyconnector.v1_12_2.net.minecraftforge.server.permission;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;

/**
 * Forge's permission nodes, answered like its {@code DefaultPermissionHandler}: a node registered as ALL is granted to everyone,
 * OP to players with operator rights on the running server (the modern op level), NONE and unknown nodes to nobody.
 */
public final class PermissionAPI {
    private static final Map<String, DefaultPermissionLevel> LEVELS = new ConcurrentHashMap<>();
    private static final Map<String, String> DESCRIPTIONS = new ConcurrentHashMap<>();

    private PermissionAPI() {}

    public static String registerNode(String node, DefaultPermissionLevel level, String desc) {
        if (node == null || node.isEmpty()) throw new IllegalArgumentException("Permission node can't be empty");
        LEVELS.put(node, level == null ? DefaultPermissionLevel.NONE : level);
        if (desc != null && !desc.isEmpty()) DESCRIPTIONS.put(node, desc);
        return node;
    }

    public static boolean hasPermission(EntityPlayer player, String node) {
        if (player == null || node == null) return false;
        DefaultPermissionLevel level = LEVELS.getOrDefault(node, DefaultPermissionLevel.NONE);
        if (level == DefaultPermissionLevel.ALL) return true;
        if (level == DefaultPermissionLevel.NONE) return false;
        net.minecraft.world.entity.player.Player modern = player.mcc$player();
        if (modern == null) return false;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return modern.hasPermissions(server != null ? server.getOperatorUserPermissionLevel() : 2);
    }

    /** The nodes mods registered (the handler's {@code getRegisteredNodes()}). */
    public static java.util.Collection<String> mcc$registeredNodes() {
        return java.util.Collections.unmodifiableSet(LEVELS.keySet());
    }

    public static String mcc$nodeDescription(String node) {
        return DESCRIPTIONS.getOrDefault(node, "");
    }
}
