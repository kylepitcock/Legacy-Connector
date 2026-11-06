package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The server self test report of the 1.6.4 adapters (logged right before the autostop of {@code runServerSubTest}): the registered tick
 * handlers with the number of times each fired, the network channels, and a loopback check of the packet receive path.
 */
public final class SelfTest164 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/SelfTest164");

    private SelfTest164() {}

    /** Vanilla statics of the id facades ({@code Block.mushroomBrown}, {@code Item.ingotIron}) that come out null: a type of 1.6.4 the shim's object is not an instance of. */
    private static void nullStatics() {
        for (Class<?> facade : new Class<?>[] {legacyconnector.v1_6_4.facade.net.minecraft.block.Block.class, legacyconnector.v1_6_4.facade.net.minecraft.item.Item.class}) {
            StringBuilder missing = new StringBuilder();
            int total = 0;
            for (java.lang.reflect.Method m : facade.getDeclaredMethods()) {
                if (!m.getName().startsWith("mcc$get$") || m.getParameterCount() != 0 || !java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
                total++;
                try {
                    if (m.invoke(null) == null) missing.append(' ').append(m.getName().substring(8));
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    missing.append(' ').append(m.getName().substring(8)).append("(!)");
                }
            }
            LOG.info("1.6.4 {} statics: {} read, null:{}", facade.getSimpleName(), total, missing.length() == 0 ? " none" : missing);
        }
    }

    public static void report() {
        var ticks = Ticks164.summary();
        String network = Network164.summary();
        if (ticks.isEmpty() && network.startsWith("0 channel(s)")) return; // no 1.6.4 mods
        nullStatics();
        LOG.info("1.6.4 tick handlers: {} registered", ticks.size());
        for (String line : ticks) LOG.info("1.6.4 tick handler {}", line);
        LOG.info("1.6.4 network: {}", network);
        LOG.info("1.6.4 packet receive loopback: {}", Network164.loopback() ? "ok" : "FAILED");
    }
}
