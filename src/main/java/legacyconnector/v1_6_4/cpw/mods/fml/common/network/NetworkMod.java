package legacyconnector.v1_6_4.cpw.mods.fml.common.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import legacyconnector.v1_6_4.net.minecraft.network.INetworkManager;
import legacyconnector.v1_6_4.net.minecraft.network.NetLoginHandler;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet131MapData;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet1Login;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet250CustomPayload;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.Player;
import legacyconnector.v1_7_10.net.minecraft.network.INetHandler;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;

/**
 * FML 1.6's declaration of a mod's network side on its {@code @Mod} class: the channels and handlers the mod wants registered
 * (see {@code bridge.NetworkModHook}, which reads it after the mod is constructed).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NetworkMod {
    boolean clientSideRequired() default false;

    boolean serverSideRequired() default false;

    String[] channels() default {};

    Class<?> packetHandler() default NULL.class;

    Class<?> connectionHandler() default NULL.class;

    Class<?> tinyPacketHandler() default NULL.class;

    String versionBounds() default "";

    SidedPacketHandler clientPacketHandlerSpec() default @SidedPacketHandler(channels = {}, packetHandler = NULL.class);

    SidedPacketHandler serverPacketHandlerSpec() default @SidedPacketHandler(channels = {}, packetHandler = NULL.class);

    /** The "no handler" default of the annotation's class members. */
    class NULL implements IPacketHandler, IConnectionHandler, ITinyPacketHandler {
        @Override
        public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player) {
        }

        @Override
        public void playerLoggedIn(Player player, INetHandler netHandler, INetworkManager manager) {
        }

        @Override
        public String connectionReceived(NetLoginHandler netHandler, INetworkManager manager) {
            return null;
        }

        @Override
        public void connectionOpened(INetHandler netClientHandler, String server, int port, INetworkManager manager) {
        }

        @Override
        public void connectionOpened(INetHandler netClientHandler, MinecraftServer server, INetworkManager manager) {
        }

        @Override
        public void connectionClosed(INetworkManager manager) {
        }

        @Override
        public void clientLoggedIn(INetHandler clientHandler, INetworkManager manager, Packet1Login login) {
        }

        @Override
        public void handle(INetHandler handler, Packet131MapData mapData) {
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface SidedPacketHandler {
        String[] channels();

        Class<?> packetHandler();
    }

    /** Marks the method that decides whether a remote version of the mod is acceptable. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface VersionCheckHandler {
    }
}
