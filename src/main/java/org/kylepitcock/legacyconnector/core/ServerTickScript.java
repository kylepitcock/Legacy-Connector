package org.kylepitcock.legacyconnector.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development aid for headless server runs ({@code -Dlegacyconnector.serverTickScript=<file.js>}): evaluates the file once per server tick
 * after {@code legacyconnector.serverTickScriptFrom} ticks (default 200) with the bindings {@code server}, {@code tick} (counted from the
 * first run) and {@code log}. The engine keeps its globals between runs, so a script can place a machine on one tick and read it on
 * the next. Everything it prints goes to the log as {@code SERVERSCRIPT: ...}. Never used in normal play.
 */
public final class ServerTickScript {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/ServerTickScript");
    private static int ticks;
    private static int ran;
    private static boolean failed;

    private ServerTickScript() {}

    public static void register(IEventBus game) {
        String file = System.getProperty("legacyconnector.serverTickScript");
        if (file == null || file.isBlank()) return;
        int from = Integer.getInteger("legacyconnector.serverTickScriptFrom", 200);
        game.addListener((ServerTickEvent.Post e) -> {
            if (++ticks < from || failed) return;
            try {
                String out = DevScript.eval(Files.readString(Path.of(file)), Map.of("server", e.getServer(), "tick", ran++, "log", LOG));
                if (!out.isBlank()) LOG.info("SERVERSCRIPT: {}", out.strip());
            } catch (Throwable t) {
                failed = true;
                LOG.error("SERVERSCRIPT failed on run {}", ran, t);
            }
        });
    }
}
