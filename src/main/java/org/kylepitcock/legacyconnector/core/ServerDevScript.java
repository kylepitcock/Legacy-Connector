package org.kylepitcock.legacyconnector.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Development aid for headless servers: with {@code -Dlegacyconnector.serverDevScript=<dir>} every {@code <dir>/*.js} file is
 * evaluated once on the server thread (bindings {@code server}, {@code log}; see {@link DevScript}) and its output is written
 * to {@code <name>.txt} next to it, then the script is renamed to {@code <name>.js.done}.
 */
public final class ServerDevScript {
    private ServerDevScript() {}

    public static void register() {
        String dir = System.getProperty("legacyconnector.serverDevScript");
        if (dir == null || dir.isEmpty()) return;
        Path root = Path.of(dir);
        int[] tick = {0};
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (++tick[0] % 10 != 0) return;
            try (var files = Files.list(root)) {
                for (Path p : files.filter(f -> f.getFileName().toString().endsWith(".js")).toList()) {
                    String name = p.getFileName().toString();
                    String out;
                    try {
                        out = DevScript.eval(Files.readString(p), Map.of("server", e.getServer(), "log", org.slf4j.LoggerFactory.getLogger("LegacyConnector/ServerScript")));
                    } catch (Throwable t) {
                        out = "ERROR " + t;
                    }
                    Files.writeString(root.resolve(name.substring(0, name.length() - 3) + ".txt"), out);
                    Files.move(p, root.resolve(name + ".done"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {
            }
        });
    }
}
