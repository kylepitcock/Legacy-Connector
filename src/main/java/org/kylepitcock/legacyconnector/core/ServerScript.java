package org.kylepitcock.legacyconnector.core;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.script.ScriptEngine;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development aid for headless servers ({@code -Dlegacyconnector.serverScript=<dir>}): every few ticks the server runs the first {@code *.js}
 * file in the directory (Nashorn, on the server thread) and writes the output to {@code <name>.out}, so a long-lived server can be probed
 * without restarts. {@code -Dlegacyconnector.serverScriptMod=<legacy mod id>} makes {@code Java.type} resolve that legacy mod's classes
 * (the mod's own loader). Bindings: {@code server}, {@code log}, {@code tick} (server ticks so far). A script may define
 * {@code function onTick(n)}, which then runs every tick until it returns {@code true} (multi-tick scenarios: set up, wait, assert).
 */
public final class ServerScript {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/ServerScript");
    private static ScriptEngine engine;
    private static boolean hasTick;
    private static int tick;

    private ServerScript() {}

    public static void register() {
        String dir = System.getProperty("legacyconnector.serverScript");
        if (dir == null || dir.isEmpty()) return;
        Path path = Path.of(dir);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> poll(e.getServer(), path));
    }

    private static void poll(net.minecraft.server.MinecraftServer server, Path dir) {
        tick++;
        try {
            if (hasTick) {
                try {
                    engine.put("tick", tick);
                    Object r = ((javax.script.Invocable) engine).invokeFunction("onTick", tick);
                    if (Boolean.TRUE.equals(r)) {
                        hasTick = false;
                        engine.eval("onTick = undefined;");
                    }
                } catch (NoSuchMethodException ex) {
                    hasTick = false;
                } catch (Throwable t) {
                    hasTick = false;
                    LOG.warn("onTick failed", t);
                }
            }
            if (tick % 5 != 0) return;
            Files.createDirectories(dir);
            Path script;
            try (Stream<Path> s = Files.list(dir)) {
                script = s.filter(p -> p.toString().endsWith(".js")).sorted().findFirst().orElse(null);
            }
            if (script == null) return;
            String code = Files.readString(script);
            Path out = Path.of(script.toString().replaceAll("\\.js$", ".out"));
            Files.move(script, Path.of(script + ".done"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            StringWriter w = new StringWriter();
            try {
                if (engine == null) {
                    ClassLoader loader = ServerScript.class.getClassLoader();
                    String modId = System.getProperty("legacyconnector.serverScriptMod");
                    LegacyModRecord rec = modId == null ? null : LegacyMods.byLegacyId(modId);
                    if (rec != null && rec.mainClass != null) loader = rec.mainClass.getClassLoader();
                    engine = new org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory().getScriptEngine(new String[] {"--language=es6"}, loader);
                }
                engine.put("server", server);
                engine.put("log", LOG);
                engine.put("tick", tick);
                engine.getContext().setWriter(w);
                engine.getContext().setErrorWriter(w);
                Object r = engine.eval(code);
                hasTick = engine.get("onTick") != null;
                w.write(r == null ? "" : "=> " + r);
            } catch (Throwable t) {
                Throwable root = t;
                while (root.getCause() != null && root.getCause() != root) root = root.getCause();
                w.write("ERROR " + t.getMessage() + (root != t ? "\n  caused by " + root : ""));
                int shown = 0;
                for (StackTraceElement f : root.getStackTrace()) {
                    String c = f.getClassName();
                    if (c.startsWith("org.openjdk.nashorn") || c.startsWith("jdk.dynalink") || c.startsWith("java.scripting")) continue;
                    w.write("\n    at " + f);
                    if (++shown >= 8) break;
                }
            }
            Files.writeString(out, w.toString());
        } catch (Exception e) {
            LOG.warn("server script poll failed", e);
        }
    }
}
