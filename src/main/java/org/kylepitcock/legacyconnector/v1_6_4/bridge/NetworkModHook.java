package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_6_4.cpw.mods.fml.common.network.IConnectionHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.network.IPacketHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.network.ITinyPacketHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.network.NetworkMod;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FML 1.6 acted on a mod's {@code @NetworkMod} when it loaded: the channels it names got the packet handler, the connection and tiny packet
 * handlers were registered. Done here right after the mod's main class is constructed (before pre-init, as FML did).
 */
public final class NetworkModHook {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Network164");

    private NetworkModHook() {}

    public static void apply(LegacyModRecord mod) {
        if (mod.mainClass == null) return;
        NetworkMod nm = mod.mainClass.getAnnotation(NetworkMod.class);
        if (nm == null) return;
        Map<Class<?>, Object> instances = new HashMap<>();
        Object packetHandler = make(nm.packetHandler(), instances, mod);
        if (packetHandler instanceof IPacketHandler h) {
            for (String channel : nm.channels()) Network164.registerChannel(h, channel, null);
        }
        sided(nm.clientPacketHandlerSpec(), Side.CLIENT, instances, mod);
        sided(nm.serverPacketHandlerSpec(), Side.SERVER, instances, mod);
        if (make(nm.connectionHandler(), instances, mod) instanceof IConnectionHandler c) Network164.registerConnectionHandler(c);
        if (make(nm.tinyPacketHandler(), instances, mod) instanceof ITinyPacketHandler t) Network164.registerTinyHandler(mod.legacyId, t);
    }

    private static void sided(NetworkMod.SidedPacketHandler spec, Side side, Map<Class<?>, Object> instances, LegacyModRecord mod) {
        if (make(spec.packetHandler(), instances, mod) instanceof IPacketHandler h) {
            for (String channel : spec.channels()) Network164.registerChannel(h, channel, side);
        }
    }

    /** The handler of a class the annotation names (once per class; the annotation default means none). */
    private static Object make(Class<?> type, Map<Class<?>, Object> instances, LegacyModRecord mod) {
        if (type == null || type == NetworkMod.NULL.class) return null;
        return instances.computeIfAbsent(type, t -> {
            try {
                var ctor = t.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                LOG.error("{}: the network handler {} could not be created", mod.legacyId, t.getName(), e);
                return null;
            }
        });
    }
}
