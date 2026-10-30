package legacyconnector.v1_12_2.net.minecraftforge.server.permission;

import com.mojang.authlib.GameProfile;
import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraftforge.server.permission.context.IContext;

/** Decides permission nodes (a permission mod replaces the default handler). */
public interface IPermissionHandler {
    void registerNode(String node, DefaultPermissionLevel level, String desc);

    Collection<String> getRegisteredNodes();

    boolean hasPermission(GameProfile profile, String node, IContext context);

    String getNodeDescription(String node);
}
