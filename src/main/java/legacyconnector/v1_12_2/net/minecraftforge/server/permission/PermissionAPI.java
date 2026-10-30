package legacyconnector.v1_12_2.net.minecraftforge.server.permission;

import com.mojang.authlib.GameProfile;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraftforge.server.permission.context.IContext;
import legacyconnector.v1_12_2.net.minecraftforge.server.permission.context.PlayerContext;

/**
 * Forge's permission nodes: mods register nodes with a default level and ask whether a player has one; the handler decides
 * ({@link DefaultPermissionHandler} unless a permission mod set its own).
 */
public final class PermissionAPI {
    private static IPermissionHandler permissionHandler = DefaultPermissionHandler.INSTANCE;

    private PermissionAPI() {}

    public static void setPermissionHandler(IPermissionHandler handler) {
        if (handler == null) throw new NullPointerException("Permission handler can't be null!");
        permissionHandler = handler;
    }

    public static IPermissionHandler getPermissionHandler() {
        return permissionHandler;
    }

    public static String registerNode(String node, DefaultPermissionLevel level, String desc) {
        if (node == null || node.isEmpty()) throw new IllegalArgumentException("Permission node can't be empty");
        permissionHandler.registerNode(node, level, desc);
        return node;
    }

    public static boolean hasPermission(GameProfile profile, String node, IContext context) {
        if (profile == null || node == null || node.isEmpty()) return false;
        return permissionHandler.hasPermission(profile, node, context);
    }

    public static boolean hasPermission(EntityPlayer player, String node) {
        if (player == null || node == null) return false;
        var modern = player.mcc$player();
        return modern != null && hasPermission(modern.getGameProfile(), node, new PlayerContext(player));
    }

    /** The nodes mods registered (the handler's {@code getRegisteredNodes()}). */
    public static java.util.Collection<String> mcc$registeredNodes() {
        return permissionHandler.getRegisteredNodes();
    }

    public static String mcc$nodeDescription(String node) {
        return permissionHandler.getNodeDescription(node);
    }
}
