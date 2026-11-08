package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_6_4.net.minecraft.network.INetworkManager;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet132TileEntityData;
import legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The client end of 1.6.4's tile entity description packet: the tile's own {@code onDataPacket(INetworkManager, Packet132TileEntityData)},
 * which the 1.7.10 shim's tile entity does not declare (its base method takes 1.7.10's packet), is found by reflection once per class.
 */
public final class Tiles164 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Network164");
    private static final Map<Class<?>, java.util.Optional<Method>> HANDLERS = new ConcurrentHashMap<>();

    private Tiles164() {}

    /** Hands the description to {@code tile}; false when the tile has no 1.6.4 handler (the caller then uses 1.7.10's). */
    public static boolean deliver(TileEntity tile, int x, int y, int z, int type, NBTTagCompound tag) {
        var found = HANDLERS.computeIfAbsent(tile.getClass(), Tiles164::find);
        if (found.isEmpty()) return false;
        try {
            found.get().invoke(tile, Manager164.of(NetHandlerPlayClient.INSTANCE), new Packet132TileEntityData(x, y, z, type, tag));
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("{} failed to handle its update packet", tile.getClass().getName(), e);
        }
        return true;
    }

    private static java.util.Optional<Method> find(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class && c != TileEntity.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getName().equals("onDataPacket") && p.length == 2 && p[0] == INetworkManager.class && p[1] == Packet132TileEntityData.class) {
                    m.setAccessible(true);
                    return java.util.Optional.of(m);
                }
            }
        }
        return java.util.Optional.empty();
    }
}
