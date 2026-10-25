package org.kylepitcock.legacyconnector.core;

import java.io.StringWriter;
import java.util.Map;
import javax.script.ScriptEngine;

/**
 * Development aid: evaluates JavaScript (Nashorn) inside the running game, so a developer can inspect or change live state
 * (entities per level, a block's legacy object, a registry) without restarting. Used by the remote control's {@code js}
 * command. Scripts reach any class with {@code Java.type("net.minecraft.world.level.Level")}; {@code print} goes to the reply.
 */
public final class DevScript {
    private static ScriptEngine engine;

    private DevScript() {}

    public static synchronized String eval(String code, Map<String, Object> bindings) throws Exception {
        if (engine == null) {
            engine = new org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory()
                    .getScriptEngine(new String[] {"--language=es6"}, DevScript.class.getClassLoader());
        }
        bindings.forEach(engine::put);
        StringWriter out = new StringWriter();
        engine.getContext().setWriter(out);
        engine.getContext().setErrorWriter(out);
        Object result = engine.eval(code);
        return out + (result == null ? "" : String.valueOf(result));
    }
}
